'use strict';

const { verifyToken } = require('./jwt');
const { ApiError, CODES } = require('../envelope');

/*
 * Express middleware that turns a Bearer token into req.player. The player
 * record (rank, corps, capabilities) is loaded from the data source on every
 * request — the token is only an identity claim, never an authority claim.
 */

function authRequired(ctx) {
  return async function (req, res, next) {
    try {
      const header = req.get('authorization') || '';
      const m = header.match(/^Bearer\s+(.+)$/i);
      if (!m) throw new ApiError(CODES.UNAUTHENTICATED, 'Token mancante.');

      const result = verifyToken(m[1], ctx.config.auth.jwtSecret);
      if (!result.ok) throw new ApiError(CODES.UNAUTHENTICATED, 'Token non valido o scaduto.');

      const uuid = result.payload.sub;
      const player = await ctx.dataSource.getPlayer(uuid);
      if (!player) throw new ApiError(CODES.UNAUTHENTICATED, 'Giocatore non trovato.');

      req.player = player;
      req.tokenMode = result.payload.mode || 'real';
      next();
    } catch (err) {
      next(err);
    }
  };
}

module.exports = { authRequired };
