'use strict';

const express = require('express');
const { ok, ApiError, CODES } = require('../envelope');
const { authRequired } = require('../auth/middleware');
const { getWidget } = require('../widgets/registry');
const { getHandler } = require('../widgets/handlers');

/*
 * Widget router. A single dynamic endpoint, GET /widget/:widgetId, keyed by the
 * widget id (e.g. economy.balance). Before dispatching it checks, in order:
 *   1. the widget exists                  -> else 404 WIDGET_UNKNOWN
 *   2. its module is active               -> else 404 WIDGET_NOT_AVAILABLE
 *   3. the player holds the capability     -> else 403 WIDGET_FORBIDDEN
 * This is the server-side twin of the layout filtering: even if a client asks
 * for a widget the layout hid, the endpoint refuses it.
 */

module.exports = function widgetRoutes(ctx) {
  const router = express.Router();
  const activeModules = new Set(ctx.config.modules.active);

  function access(widgetId, player) {
    const def = getWidget(widgetId);
    if (!def) throw new ApiError(CODES.WIDGET_UNKNOWN, `Widget sconosciuto: ${widgetId}`);
    if (def.module !== 'core' && !activeModules.has(def.module)) {
      throw new ApiError(CODES.WIDGET_NOT_AVAILABLE,
        `Il modulo ${def.module} non è attivo su questo server.`);
    }
    if (def.capability && !(player.capabilities || []).includes(def.capability)) {
      throw new ApiError(CODES.WIDGET_FORBIDDEN,
        'Non hai i permessi per questo widget.');
    }
    return def;
  }

  router.use(authRequired(ctx));

  // POST /widget/notifications.feed/:id/read — mark a notification read.
  router.post('/notifications.feed/:id/read', async (req, res, next) => {
    try {
      access('notifications.feed', req.player);
      const marked = await ctx.dataSource.markNotificationRead(req.player, req.params.id);
      if (!marked) throw new ApiError(CODES.NOT_FOUND, 'Notifica non trovata.');
      res.json(ok({ id: req.params.id, read: true }));
    } catch (err) { next(err); }
  });

  // POST /widget/economy.transfers — the single allowed write, demo-only.
  router.post('/economy.transfers', async (req, res, next) => {
    try {
      access('economy.transfers', req.player);
      if (ctx.config.mode !== 'demo') {
        throw new ApiError(CODES.WIDGET_FORBIDDEN,
          'I bonifici reali richiedono un EconomyAdapter con permesso di scrittura, ' +
          'non abilitato di default per proteggere l\'economia di gioco.');
      }
      const result = await ctx.dataSource.recordTransfer(req.player, req.body || {});
      res.json(ok(result));
    } catch (err) { next(mapError(err)); }
  });

  // GET /widget/:widgetId — read any widget for the current player.
  router.get('/:widgetId', async (req, res, next) => {
    try {
      const widgetId = req.params.widgetId;
      const def = access(widgetId, req.player);
      const handler = getHandler(widgetId);
      if (!handler) throw new ApiError(CODES.WIDGET_UNKNOWN, `Widget senza handler: ${widgetId}`);

      const data = await handler(req.player, ctx.dataSource, req.query);
      if (data == null && (def.kind === 'detail' || def.kind === 'collection')) {
        throw new ApiError(CODES.NOT_FOUND, 'Risorsa non trovata.');
      }
      res.json(ok(data, { widget: widgetId }));
    } catch (err) { next(mapError(err)); }
  });

  return router;
};

// Handlers reject with a plain Error carrying err.code for client mistakes.
function mapError(err) {
  if (err instanceof ApiError) return err;
  if (err && err.code === 'BAD_REQUEST') return new ApiError(CODES.BAD_REQUEST, err.message);
  return err;
}
