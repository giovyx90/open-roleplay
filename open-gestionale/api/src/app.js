'use strict';

const path = require('path');
const fs = require('fs');
const express = require('express');
const cors = require('cors');
const { ok, fail, ApiError, CODES } = require('./envelope');

const authRoutes = require('./routes/auth');
const demoRoutes = require('./routes/demo');
const configRoutes = require('./routes/config');
const playerRoutes = require('./routes/player');
const widgetRoutes = require('./routes/widgets');
const { makeLimiter } = require('./ratelimit');
const { verifyToken } = require('./auth/jwt');

/*
 * Builds the Express app from a context: { config, dataSource, layout }. Kept as
 * a pure factory (no listen, no process.env reads) so tests can spin up an app
 * against an in-memory demo source with a fixed config.
 */

function createApp(ctx) {
  const app = express();
  app.disable('x-powered-by');
  app.set('trust proxy', true);
  app.use(express.json({ limit: '64kb' }));

  // ---- CORS: explicit allow-list, or open in demo mode ----
  const origins = ctx.config.cors.allowedOrigins;
  app.use(cors({
    origin(origin, cb) {
      if (!origin) return cb(null, true);            // same-origin / curl
      if (origins.length === 0) return cb(null, true); // unconfigured / demo: reflect
      cb(null, origins.includes(origin));
    },
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Authorization', 'Content-Type'],
    maxAge: 600,
  }));

  // ---- Require TLS in production (behind a TLS-terminating proxy) ----
  if (ctx.config.api.requireTls) {
    app.use((req, res, next) => {
      if (req.path === '/health') return next();
      const proto = req.get('x-forwarded-proto') || (req.secure ? 'https' : 'http');
      if (proto !== 'https') {
        return res.status(400).json(fail(CODES.BAD_REQUEST, 'HTTPS obbligatorio.'));
      }
      next();
    });
  }

  // ---- General per-token rate limit on everything below /auth ----
  const generalLimiter = makeLimiter({
    max: ctx.config.api.rateLimit.general,
    keyFn(req) {
      const m = (req.get('authorization') || '').match(/^Bearer\s+(.+)$/i);
      if (m) {
        const v = verifyToken(m[1], ctx.config.auth.jwtSecret);
        if (v.ok) return 'tok:' + v.payload.sub;
      }
      return 'anon:' + (req.ip || 'unknown');
    },
  });

  // ---- Meta endpoints ----
  app.get('/health', (req, res) => res.json(ok({ status: 'ok', mode: ctx.config.mode })));

  app.get('/', (req, res) => res.json(ok({
    name: 'Open Gestionale API',
    version: require('../package.json').version,
    mode: ctx.config.mode,
    docs: '/openapi.yaml',
  })));

  const openapiPath = path.join(__dirname, '..', 'openapi.yaml');
  app.get('/openapi.yaml', (req, res) => {
    if (!fs.existsSync(openapiPath)) return res.status(404).json(fail(CODES.NOT_FOUND, 'Spec assente.'));
    res.type('text/yaml').send(fs.readFileSync(openapiPath, 'utf8'));
  });

  // ---- Routers ----
  app.use('/auth', authRoutes(ctx));
  app.use('/demo', demoRoutes(ctx));
  app.use('/config', generalLimiter, configRoutes(ctx));
  app.use('/player', generalLimiter, playerRoutes(ctx));
  app.use('/widget', generalLimiter, widgetRoutes(ctx));

  // ---- 404 + error handler ----
  app.use((req, res) => res.status(404).json(fail(CODES.NOT_FOUND, 'Endpoint non trovato.')));

  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    if (err instanceof ApiError) {
      return res.status(err.status).json(fail(err.code, err.message));
    }
    if (err && err.type === 'entity.parse.failed') {
      return res.status(400).json(fail(CODES.BAD_REQUEST, 'JSON non valido.'));
    }
    // Unexpected: log server-side, never leak internals to the client.
    // eslint-disable-next-line no-console
    console.error('[open-gestionale] errore non gestito:', err);
    res.status(500).json(fail(CODES.INTERNAL, 'Errore interno.'));
  });

  return app;
}

module.exports = { createApp };
