'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { resolveLayout } = require('../src/layout');

const RAW = {
  hero: [
    { widget: 'economy.balance', label: 'Bilancio' },
    { widget: 'identity.phone', label: 'Telefono', requires_module: 'identity' },
    { widget: 'player.weekly_time', label: 'Tempo' },
  ],
  tabs: [
    { id: 'economy', label: 'Economia', widgets: ['economy.transactions'] },
    {
      id: 'justice', label: 'Giustizia', requires_module: 'openfdo',
      widgets: ['justice.casellario', 'justice.dossiers.agent', 'justice.wanted'],
    },
  ],
  sidebar: ['notifications.feed', 'server.news'],
  pages: [{ id: 'reg', label: 'Registro', widgets: ['company.registry'], requires_module: 'companies' }],
};

test('module gating: an inactive module removes its widgets and empties its tabs', () => {
  const out = resolveLayout(RAW, {
    activeModules: ['core', 'economy', 'openfdo'], // no identity, no companies
    capabilities: ['fdo.agent'],
  });
  // identity.phone dropped from hero
  assert.deepEqual(out.hero.map((w) => w.widget), ['economy.balance', 'player.weekly_time']);
  // companies tab/page gone -> pages empty
  assert.equal(out.pages.length, 0);
  // justice tab survives with agent widgets visible
  const justice = out.tabs.find((t) => t.id === 'justice');
  assert.deepEqual(justice.widgets.map((w) => w.widget),
    ['justice.casellario', 'justice.dossiers.agent', 'justice.wanted']);
});

test('capability gating: without fdo.agent only own casellario remains, tab still shown', () => {
  const out = resolveLayout(RAW, {
    activeModules: ['core', 'economy', 'openfdo', 'identity', 'companies'],
    capabilities: [], // plain citizen
  });
  const justice = out.tabs.find((t) => t.id === 'justice');
  assert.deepEqual(justice.widgets.map((w) => w.widget), ['justice.casellario']);
});

test('a tab whose every widget is gated is dropped entirely', () => {
  const raw = { tabs: [{ id: 'fdo', label: 'FDO', requires_module: 'openfdo', widgets: ['justice.wanted'] }] };
  const out = resolveLayout(raw, { activeModules: ['core', 'openfdo'], capabilities: [] });
  assert.equal(out.tabs.length, 0);
});

test('unknown widget ids are never rendered', () => {
  const raw = { hero: ['does.not.exist', 'player.profile'] };
  const out = resolveLayout(raw, { activeModules: ['core'], capabilities: [] });
  assert.deepEqual(out.hero.map((w) => w.widget), ['player.profile']);
});

test('labels fall back to the registry default when the layout omits them', () => {
  const raw = { hero: ['economy.balance'] };
  const out = resolveLayout(raw, { activeModules: ['core', 'economy'], capabilities: [] });
  assert.equal(out.hero[0].label, 'Bilancio');
});
