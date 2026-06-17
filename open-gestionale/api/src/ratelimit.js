'use strict';

const { ApiError, CODES } = require('./envelope');

/*
 * Minimal in-memory fixed-window rate limiter. No external dependency, which
 * keeps the bridge easy to audit. Auth endpoints are limited per IP, general
 * endpoints per token. For a multi-instance deployment, swap this for a shared
 * store — the middleware contract stays the same.
 */

function makeLimiter({ max, windowMs = 3600000, keyFn }) {
  const hits = new Map(); // key -> { count, resetAt }

  return function (req, res, next) {
    if (!Number.isFinite(max) || max <= 0) return next(); // disabled
    const now = Date.now();
    const key = keyFn(req);
    let entry = hits.get(key);
    if (!entry || now >= entry.resetAt) {
      entry = { count: 0, resetAt: now + windowMs };
      hits.set(key, entry);
    }
    entry.count += 1;

    const remaining = Math.max(0, max - entry.count);
    res.set('X-RateLimit-Limit', String(max));
    res.set('X-RateLimit-Remaining', String(remaining));
    res.set('X-RateLimit-Reset', String(Math.ceil(entry.resetAt / 1000)));

    if (entry.count > max) {
      res.set('Retry-After', String(Math.ceil((entry.resetAt - now) / 1000)));
      return next(new ApiError(CODES.RATE_LIMITED, 'Troppe richieste, riprova più tardi.'));
    }

    // Opportunistic cleanup so the map cannot grow without bound.
    if (hits.size > 5000) {
      for (const [k, v] of hits) if (now >= v.resetAt) hits.delete(k);
    }
    next();
  };
}

function clientIp(req) {
  const fwd = req.get('x-forwarded-for');
  if (fwd) return fwd.split(',')[0].trim();
  return req.ip || req.socket?.remoteAddress || 'unknown';
}

module.exports = { makeLimiter, clientIp };
