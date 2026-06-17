'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');

const { DemoDataSource } = require('../src/datasource/demo');
const { SqliteDataSource } = require('../src/datasource/sqlite');
const { run: importSeedToDb } = require('../db/import-seed');
const { getHandler } = require('../src/widgets/handlers');

const SEED = path.join(__dirname, '..', '..', 'demo', 'seed.json');

/*
 * The demo (in-memory) source and the SQLite source must return byte-identical
 * widget payloads. If they ever diverge, a server moving from the demo to a real
 * DB would silently see different data — this test is the guard.
 */

function setup() {
  const demo = DemoDataSource.fromFile(SEED);
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'og-parity-'));
  const dbPath = path.join(dir, 'parity.db');
  importSeedToDb(SEED, dbPath);
  const sql = new SqliteDataSource(dbPath);
  return { demo, sql, cleanup: () => { sql.close(); fs.rmSync(dir, { recursive: true, force: true }); } };
}

const PER_PLAYER = [
  'player.profile', 'player.weekly_time', 'economy.balance', 'economy.transactions',
  'economy.cards', 'company.mine', 'justice.casellario', 'justice.dossiers',
  'notifications.feed', 'jobs.current', 'jobs.history', 'health.record',
  'identity.phone', 'identity.documents', 'education.degrees', 'politics.charges',
];
const GLOBAL = ['justice.wanted', 'politics.laws', 'politics.elections', 'politics.decrees',
  'jobs.postings', 'company.registry'];
// server.news is intentionally NOT here: it is a gestionale-owned table
// (schema.sql) with its own id/timestamp shape, so it is checked structurally.

const UUIDS = [
  '11111111-1111-4111-8111-111111111111',
  '22222222-2222-4222-8222-222222222222',
  '33333333-3333-4333-8333-333333333333',
  '44444444-4444-4444-8444-444444444444',
  '55555555-5555-4555-8555-555555555555',
];

test('demo and sqlite return identical per-player widget payloads', async () => {
  const { demo, sql, cleanup } = setup();
  try {
    for (const uuid of UUIDS) {
      const pDemo = await demo.getPlayer(uuid);
      const pSql = await sql.getPlayer(uuid);
      assert.deepEqual(pSql, pDemo, `getPlayer mismatch for ${uuid}`);
      for (const wid of PER_PLAYER) {
        const h = getHandler(wid);
        const a = await h(pDemo, demo, {});
        const b = await h(pSql, sql, {});
        assert.deepEqual(b, a, `${wid} mismatch for ${uuid}`);
      }
    }
  } finally { cleanup(); }
});

test('demo and sqlite return identical global widget payloads', async () => {
  const { demo, sql, cleanup } = setup();
  try {
    const player = await demo.getPlayer(UUIDS[0]);
    for (const wid of GLOBAL) {
      const h = getHandler(wid);
      const a = await h(player, demo, {});
      const b = await h(player, sql, {});
      assert.deepEqual(b, a, `${wid} mismatch`);
    }
    // Agent-only dossiers for Giulia
    const giulia = await sql.getPlayer(UUIDS[1]);
    const giuliaDemo = await demo.getPlayer(UUIDS[1]);
    const agentH = getHandler('justice.dossiers.agent');
    assert.deepEqual(await agentH(giulia, sql, {}), await agentH(giuliaDemo, demo, {}));
  } finally { cleanup(); }
});

test('news is equivalent across sources (same titles, pinned first)', async () => {
  const { demo, sql, cleanup } = setup();
  try {
    const a = await demo.news();
    const b = await sql.news();
    assert.deepEqual(b.map((n) => n.title), a.map((n) => n.title));
    assert.deepEqual(b.map((n) => !!n.pinned), a.map((n) => !!n.pinned));
    assert.equal(b[0].pinned, true); // the pinned item sorts first
  } finally { cleanup(); }
});

test('sqlite honours notification read-state through its own table', async () => {
  const { sql, cleanup } = setup();
  try {
    const giulia = await sql.getPlayer(UUIDS[1]);
    const before = await sql.notifications(giulia, {});
    assert.ok(before.unread >= 1);
    await sql.markNotificationRead(giulia, before.items[0].id);
    const after = await sql.notifications(giulia, {});
    assert.equal(after.unread, before.unread - 1);
  } finally { cleanup(); }
});
