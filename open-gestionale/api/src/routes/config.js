'use strict';

const express = require('express');
const { ok } = require('../envelope');
const { authRequired } = require('../auth/middleware');
const { resolveLayout } = require('../layout');

/*
 * Config router. /config/server is public so the frontend can theme the login
 * screen before anyone authenticates. /config/layout is per-player: it returns
 * the layout already resolved against the active modules and the player's
 * capabilities, so the frontend renders it blindly.
 */

module.exports = function configRoutes(ctx) {
  const router = express.Router();

  router.get('/server', (req, res) => {
    res.json(ok({
      name: ctx.config.server.name,
      setting: ctx.config.server.setting,
      theme_color: ctx.config.server.theme_color,
      logo_url: ctx.config.server.logo_url,
      timezone: ctx.config.server.timezone,
      mode: ctx.config.mode,
      modules: ctx.config.modules.active,
    }));
  });

  router.get('/layout', authRequired(ctx), (req, res) => {
    const resolved = resolveLayout(ctx.layout, {
      activeModules: new Set(ctx.config.modules.active),
      capabilities: new Set(req.player.capabilities || []),
    });
    res.json(ok({
      theme_color: ctx.config.server.theme_color,
      server_name: ctx.config.server.name,
      dashboard: resolved,
    }));
  });

  return router;
};
