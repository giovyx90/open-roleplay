'use strict';

const express = require('express');
const { ok, ApiError, CODES } = require('../envelope');

/*
 * Demo router. Exposes the roster shown on the login-free demo landing page.
 * Disabled entirely outside demo mode so a production bridge never leaks a
 * player list.
 */

module.exports = function demoRoutes(ctx) {
  const router = express.Router();

  router.get('/profiles', async (req, res, next) => {
    try {
      if (ctx.config.mode !== 'demo') {
        throw new ApiError(CODES.DEMO_DISABLED, 'La modalità demo non è attiva su questo server.');
      }
      const profiles = await ctx.dataSource.listPublicProfiles();
      res.json(ok({ items: profiles }));
    } catch (err) { next(err); }
  });

  return router;
};
