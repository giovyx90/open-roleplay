'use strict';

const { getWidget } = require('./widgets/registry');

/*
 * Resolves the raw layout (from layout.yml) into the concrete layout a specific
 * player should see. This is where adapter-first lives: a widget disappears if
 *   - its module is not active on this server, or
 *   - the player lacks the capability the widget requires.
 * Tabs and pages that end up empty are dropped, so the UI never shows a hollow
 * section. The frontend renders exactly what it receives and asks no questions.
 */

function normalizeEntry(entry) {
  // A widget entry can be a bare id ("economy.balance") or an object.
  if (typeof entry === 'string') return { widget: entry };
  return Object.assign({}, entry);
}

function canSee(entry, ctx) {
  const def = getWidget(entry.widget);
  if (!def) return false; // unknown widget id — never render guesses

  const module = entry.requires_module || def.module;
  if (module !== 'core' && !ctx.activeModules.has(module)) return false;

  // An explicit requires_module on the entry must also be active.
  if (entry.requires_module && entry.requires_module !== 'core' &&
      !ctx.activeModules.has(entry.requires_module)) {
    return false;
  }

  // A capability may come from the registry (structural) or the layout entry.
  const capability = entry.requires_capability || def.capability;
  if (capability && !ctx.capabilities.has(capability)) return false;

  return true;
}

function resolveWidget(entry, ctx) {
  const def = getWidget(entry.widget);
  return {
    widget: entry.widget,
    label: entry.label || def.label || entry.widget,
    kind: def.kind,
    module: def.module,
  };
}

function filterWidgetList(list, ctx) {
  if (!Array.isArray(list)) return [];
  return list
    .map(normalizeEntry)
    .filter((e) => canSee(e, ctx))
    .map((e) => resolveWidget(e, ctx));
}

function filterGroups(groups, ctx) {
  if (!Array.isArray(groups)) return [];
  return groups
    .map((g) => {
      const widgets = filterWidgetList(g.widgets, ctx);
      return {
        id: g.id,
        label: g.label || g.id,
        icon: g.icon || null,
        widgets,
      };
    })
    .filter((g) => g.widgets.length > 0); // drop empty tabs/pages
}

/**
 * @param {object} rawLayout  parsed layout.yml (dashboard section)
 * @param {object} ctx        { activeModules: Set, capabilities: Set }
 */
function resolveLayout(rawLayout, ctx) {
  const context = {
    activeModules: ctx.activeModules instanceof Set ? ctx.activeModules : new Set(ctx.activeModules || []),
    capabilities: ctx.capabilities instanceof Set ? ctx.capabilities : new Set(ctx.capabilities || []),
  };
  const layout = rawLayout || {};
  return {
    hero: filterWidgetList(layout.hero, context),
    tabs: filterGroups(layout.tabs, context),
    sidebar: filterWidgetList(layout.sidebar, context),
    pages: filterGroups(layout.pages, context),
  };
}

module.exports = { resolveLayout, canSee, normalizeEntry };
