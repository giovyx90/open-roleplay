'use strict';

const fs = require('fs');
const path = require('path');
const { DatabaseSync } = require('node:sqlite');

/*
 * Reference loader: takes the demo seed and writes it into a SQLite database
 * shaped like db/schema.sql. Useful to (a) stand up a realistic SQLite bridge
 * from the demo data and (b) prove, in test/parity.test.js, that the SQLite
 * source returns the same widget payloads as the in-memory demo source.
 *
 *   node db/import-seed.js [seed.json] [out.db]
 */

const GLOBAL_KEYS = ['laws', 'election', 'decrees', 'wanted', 'jobPostings'];

function applySchema(db) {
  const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  db.exec(schema);
}

function importSeed(db, seed) {
  db.exec('BEGIN');
  try {
    for (const table of ['players', 'companies', 'dossiers', 'globals', 'gestionale_news']) {
      db.prepare(`DELETE FROM ${table}`).run();
    }

    const insPlayer = db.prepare('INSERT INTO players (uuid, doc) VALUES (?, ?)');
    for (const p of seed.players || []) insPlayer.run(p.uuid, JSON.stringify(p));

    const insCompany = db.prepare('INSERT INTO companies (id, doc) VALUES (?, ?)');
    for (const c of seed.companies || []) insCompany.run(c.id, JSON.stringify(c));

    const insDossier = db.prepare('INSERT INTO dossiers (id, doc) VALUES (?, ?)');
    for (const d of seed.dossiers || []) insDossier.run(d.id, JSON.stringify(d));

    const insGlobal = db.prepare('INSERT INTO globals (key, doc) VALUES (?, ?)');
    for (const key of GLOBAL_KEYS) {
      if (seed[key] != null) insGlobal.run(key, JSON.stringify(seed[key]));
    }

    const insNews = db.prepare(
      'INSERT INTO gestionale_news (title, body, author_uuid, published_at, pinned) VALUES (?, ?, ?, ?, ?)'
    );
    for (const n of seed.news || []) {
      insNews.run(n.title, n.body, n.author || n.author_uuid || 'system',
        toEpoch(n.published_at), n.pinned ? 1 : 0);
    }

    db.exec('COMMIT');
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
}

function toEpoch(value) {
  if (typeof value === 'number') return value;
  const t = Date.parse(value);
  return Number.isFinite(t) ? t : 0;
}

function run(seedPath, dbPath) {
  const seed = JSON.parse(fs.readFileSync(seedPath, 'utf8'));
  fs.mkdirSync(path.dirname(dbPath), { recursive: true });
  const db = new DatabaseSync(dbPath);
  applySchema(db);
  importSeed(db, seed);
  db.close();
  return dbPath;
}

if (require.main === module) {
  const seedPath = process.argv[2] || path.join(__dirname, '..', '..', 'demo', 'seed.json');
  const dbPath = process.argv[3] || path.join(__dirname, '..', 'data', 'openrp.db');
  run(path.resolve(seedPath), path.resolve(dbPath));
  // eslint-disable-next-line no-console
  console.log(`[import-seed] ${seedPath} -> ${dbPath}`);
}

module.exports = { applySchema, importSeed, run, GLOBAL_KEYS };
