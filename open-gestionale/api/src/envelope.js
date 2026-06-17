'use strict';

/*
 * Every successful response is wrapped in { data, meta }; every error in
 * { error: { code, message } }. The frontend relies on this shape to tell a
 * "module not installed" (404 + WIDGET_NOT_AVAILABLE) apart from a real failure.
 */

const VERSION = '1.0';

function nowIso(clock) {
  // clock is injected in tests so responses stay deterministic.
  return (clock ? clock() : new Date()).toISOString();
}

function ok(data, extraMeta) {
  return {
    data,
    meta: Object.assign({ timestamp: nowIso(), version: VERSION }, extraMeta || {}),
  };
}

function fail(code, message) {
  return { error: { code, message } };
}

/* Known error codes — kept here so the API and its tests agree on the contract. */
const CODES = Object.freeze({
  WIDGET_NOT_AVAILABLE: 'WIDGET_NOT_AVAILABLE',
  WIDGET_UNKNOWN: 'WIDGET_UNKNOWN',
  WIDGET_FORBIDDEN: 'WIDGET_FORBIDDEN',
  UNAUTHENTICATED: 'UNAUTHENTICATED',
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  RATE_LIMITED: 'RATE_LIMITED',
  NOT_FOUND: 'NOT_FOUND',
  BAD_REQUEST: 'BAD_REQUEST',
  DEMO_DISABLED: 'DEMO_DISABLED',
  INTERNAL: 'INTERNAL',
});

/* Maps each error code to the HTTP status the route should answer with. */
const STATUS = Object.freeze({
  WIDGET_NOT_AVAILABLE: 404,
  WIDGET_UNKNOWN: 404,
  WIDGET_FORBIDDEN: 403,
  UNAUTHENTICATED: 401,
  INVALID_CREDENTIALS: 401,
  RATE_LIMITED: 429,
  NOT_FOUND: 404,
  BAD_REQUEST: 400,
  DEMO_DISABLED: 403,
  INTERNAL: 500,
});

/* A small typed error the routes can throw and the error middleware understands. */
class ApiError extends Error {
  constructor(code, message) {
    super(message || code);
    this.name = 'ApiError';
    this.code = code;
    this.status = STATUS[code] || 500;
  }
}

module.exports = { ok, fail, ApiError, CODES, STATUS, VERSION };
