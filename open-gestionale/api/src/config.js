'use strict';

const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');
const { isKnownModule } = require('./widgets/registry');

/*
 * Loads and validates gestionale.yml + layout.yml. The config is the contract
 * between a server and its gestionale: which modules are active, how auth
 * behaves, where the data lives. Nothing about the ambientazione is hardcoded.
 *
 * Environment variables override the file so the same image can be configured
 * at deploy time without editing the mounted YAML:
 *   GESTIONALE_CONFIG, GESTIONALE_LAYOUT, GESTIONALE_SEED
 *   GESTIONALE_MODE (demo|real), GESTIONALE_DB_PATH
 *   GESTIONALE_JWT_SECRET, PORT, NODE_ENV
 */

const PLACEHOLDER_SECRET = 'CAMBIA_QUESTA_CHIAVE_IN_PRODUZIONE';

function readYaml(file) {
  const raw = fs.readFileSync(file, 'utf8');
  const parsed = yaml.load(raw);
  if (parsed == null || typeof parsed !== 'object') {
    throw new Error(`Config non valida (atteso un oggetto YAML): ${file}`);
  }
  return parsed;
}

function loadConfig(opts = {}) {
  const env = opts.env || process.env;
  const configPath = opts.configPath || env.GESTIONALE_CONFIG ||
    path.join(__dirname, '..', 'config', 'gestionale.yml');

  const raw = readYaml(configPath);
  const baseDir = path.dirname(configPath);

  const server = raw.server || {};
  const auth = raw.auth || {};
  const api = raw.api || {};
  const database = raw.database || {};
  const cors = raw.cors || {};
  const modules = (raw.modules && raw.modules.active) || [];

  const mode = (env.GESTIONALE_MODE || server.mode || 'real').toLowerCase();
  if (mode !== 'real' && mode !== 'demo') {
    throw new Error(`server.mode deve essere "real" o "demo" (trovato: ${mode})`);
  }

  // Validate the active module list against what the registry knows.
  const active = [...new Set(['core', ...modules])];
  for (const m of active) {
    if (!isKnownModule(m)) {
      throw new Error(`Modulo sconosciuto in modules.active: "${m}"`);
    }
  }

  const jwtSecret = env.GESTIONALE_JWT_SECRET || auth.jwt_secret || PLACEHOLDER_SECRET;
  const isProd = (env.NODE_ENV || 'development') === 'production';
  if (mode === 'real' && isProd && (!jwtSecret || jwtSecret === PLACEHOLDER_SECRET)) {
    throw new Error('auth.jwt_secret non configurato: impostalo (o GESTIONALE_JWT_SECRET) prima della produzione.');
  }

  const layoutPath = opts.layoutPath || env.GESTIONALE_LAYOUT ||
    (raw.layout && resolveRelative(baseDir, raw.layout)) ||
    path.join(baseDir, 'layout.yml');

  let seedPath = opts.seedPath || env.GESTIONALE_SEED ||
    (raw.demo && raw.demo.seed && resolveRelative(baseDir, raw.demo.seed)) || null;
  if (mode === 'demo' && !seedPath) {
    seedPath = path.join(baseDir, 'seed.json');
  }

  const dbPath = env.GESTIONALE_DB_PATH || database.path ||
    (database.path ? resolveRelative(baseDir, database.path) : null);

  return {
    mode,
    configPath,
    layoutPath,
    seedPath,
    server: {
      name: server.name || 'Open Roleplay',
      setting: server.setting || null, // free-text ambientazione hint, neutral by default
      theme_color: server.theme_color || '#16a34a',
      logo_url: server.logo_url || null,
      timezone: server.timezone || 'Europe/Rome',
    },
    database: {
      type: (database.type || 'sqlite').toLowerCase(),
      path: dbPath ? (path.isAbsolute(dbPath) ? dbPath : resolveRelative(baseDir, dbPath)) : null,
      host: database.host || null,
      port: database.port || null,
      name: database.name || null,
      user: database.user || null,
      password: database.password || null,
    },
    auth: {
      otpExpiryMinutes: numberOr(auth.otp_expiry_minutes, 5),
      jwtExpiryHours: numberOr(auth.jwt_expiry_hours, 24),
      jwtSecret,
    },
    api: {
      port: numberOr(env.PORT, numberOr(api.port, 3000)),
      requireTls: api.require_tls != null ? !!api.require_tls : isProd,
      rateLimit: {
        auth: numberOr(api.rate_limit && api.rate_limit.auth, 10),
        general: numberOr(api.rate_limit && api.rate_limit.general, 300),
      },
    },
    cors: {
      allowedOrigins: Array.isArray(cors.allowed_origins) ? cors.allowed_origins : [],
    },
    modules: { active },
  };
}

function resolveRelative(baseDir, p) {
  if (!p) return p;
  return path.isAbsolute(p) ? p : path.resolve(baseDir, p);
}

function numberOr(value, fallback) {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

function loadLayout(layoutPath) {
  const raw = readYaml(layoutPath);
  // The layout file may wrap everything under `dashboard:` (matches the doc) or
  // expose the sections at the top level. Accept both.
  return raw.dashboard || raw;
}

module.exports = { loadConfig, loadLayout, PLACEHOLDER_SECRET };
