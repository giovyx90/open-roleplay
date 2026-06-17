'use strict';

const express = require('express');
const { ok } = require('../envelope');
const { authRequired } = require('../auth/middleware');

/*
 * Player router. /player/me returns the identity the rest of the UI hangs off.
 * phone_number is null unless Open Identity is active and the player has one —
 * the gestionale never invents fields a module did not provide.
 */

module.exports = function playerRoutes(ctx) {
  const router = express.Router();

  router.get('/me', authRequired(ctx), (req, res) => {
    const p = req.player;
    const identityActive = ctx.config.modules.active.includes('identity');
    const phone = identityActive && p.identity && p.identity.phone ? p.identity.phone.number : null;
    res.json(ok({
      uuid: p.uuid,
      display_name: p.display_name,
      rank: p.rank,
      rank_label: p.rank_label || null,
      corps: p.corps || null,
      corps_label: p.corps_label || null,
      phone_number: phone,
      capabilities: p.capabilities || [],
      registered_at: p.registered_at || null,
      last_seen: p.last_seen || null,
    }));
  });

  return router;
};
