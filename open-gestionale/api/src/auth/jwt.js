'use strict';

const jwt = require('jsonwebtoken');

/*
 * Thin wrapper around jsonwebtoken so the rest of the code never imports it
 * directly. Tokens carry only the uuid and a mode flag; everything else
 * (rank, capabilities) is read fresh from the DB on each request, so an
 * in-game promotion/demotion is reflected at the next call.
 */

function signToken(uuid, { secret, expiryHours, mode }) {
  const expiresIn = `${expiryHours || 24}h`;
  const token = jwt.sign({ sub: uuid, mode: mode || 'real' }, secret, { expiresIn });
  const decoded = jwt.decode(token);
  return { token, expiresAt: new Date(decoded.exp * 1000).toISOString() };
}

function verifyToken(token, secret) {
  try {
    return { ok: true, payload: jwt.verify(token, secret) };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

module.exports = { signToken, verifyToken };
