'use strict';

const express = require('express');
const { ok, ApiError, CODES } = require('../envelope');
const { verifyOtp } = require('../auth/otp');
const { signToken } = require('../auth/jwt');
const { authRequired } = require('../auth/middleware');
const { makeLimiter, clientIp } = require('../ratelimit');

/*
 * Auth router. The gestionale owns no password: identity is proven by the
 * in-game OTP (real mode) or chosen from the demo roster (demo mode). The token
 * carries only the uuid; authority is re-read from the DB on every request.
 */

module.exports = function authRoutes(ctx) {
  const router = express.Router();

  const authLimiter = makeLimiter({
    max: ctx.config.api.rateLimit.auth,
    keyFn: (req) => 'auth:' + clientIp(req),
  });

  function issue(player, mode) {
    const { token, expiresAt } = signToken(player.uuid, {
      secret: ctx.config.auth.jwtSecret,
      expiryHours: ctx.config.auth.jwtExpiryHours,
      mode,
    });
    return { token, expires_at: expiresAt, player: ctx.dataSource.profile(player) };
  }

  // Real login: uuid + in-game OTP.
  router.post('/login', authLimiter, async (req, res, next) => {
    try {
      const { uuid, otp } = req.body || {};
      if (!uuid || !otp) throw new ApiError(CODES.BAD_REQUEST, 'uuid e otp sono obbligatori.');

      const record = await ctx.dataSource.getOtpRecord(uuid);
      const result = verifyOtp(uuid, otp, record, Date.now());
      if (!result.ok) throw new ApiError(CODES.INVALID_CREDENTIALS, 'Codice non valido o scaduto.');

      const player = await ctx.dataSource.getPlayer(uuid);
      if (!player) throw new ApiError(CODES.INVALID_CREDENTIALS, 'Giocatore non trovato.');

      await ctx.dataSource.consumeOtp(uuid);
      res.json(ok(issue(player, 'real')));
    } catch (err) { next(err); }
  });

  // Demo login: pick a profile, no OTP. Only when the server runs in demo mode.
  router.post('/demo', authLimiter, async (req, res, next) => {
    try {
      if (ctx.config.mode !== 'demo') {
        throw new ApiError(CODES.DEMO_DISABLED, 'La modalità demo non è attiva su questo server.');
      }
      const { uuid } = req.body || {};
      const player = uuid ? await ctx.dataSource.getPlayer(uuid) : null;
      if (!player) throw new ApiError(CODES.INVALID_CREDENTIALS, 'Profilo demo non trovato.');
      res.json(ok(issue(player, 'demo')));
    } catch (err) { next(err); }
  });

  router.post('/refresh', authRequired(ctx), (req, res) => {
    res.json(ok(issue(req.player, req.tokenMode)));
  });

  router.post('/logout', (req, res) => {
    // Stateless JWT: the client drops the token. Endpoint exists for symmetry.
    res.json(ok({ ok: true }));
  });

  return router;
};
