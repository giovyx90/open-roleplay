'use strict';

const test = require('node:test');
const assert = require('node:assert');
const path = require('path');

const { loadConfig, loadLayout } = require('../src/config');
const { createDataSource } = require('../src/datasource');
const { createApp } = require('../src/app');

const DEMO_CONFIG = path.join(__dirname, '..', '..', 'demo', 'gestionale.demo.yml');

const MARCO = '11111111-1111-4111-8111-111111111111';   // citizen, no capabilities
const GIULIA = '22222222-2222-4222-8222-222222222222';   // fdo.agent

async function startServer(mutateConfig) {
  const config = loadConfig({ configPath: DEMO_CONFIG });
  if (mutateConfig) mutateConfig(config);
  const layout = loadLayout(config.layoutPath);
  const dataSource = createDataSource(config);
  const app = createApp({ config, dataSource, layout });
  const server = await new Promise((resolve) => {
    const s = app.listen(0, () => resolve(s));
  });
  const base = `http://127.0.0.1:${server.address().port}`;
  return { base, stop: () => new Promise((r) => server.close(r)) };
}

async function demoToken(base, uuid) {
  const res = await fetch(`${base}/auth/demo`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ uuid }),
  });
  const json = await res.json();
  return json.data.token;
}

function auth(token) { return { headers: { authorization: `Bearer ${token}` } }; }

test('health and server config are public', async () => {
  const { base, stop } = await startServer();
  try {
    const h = await (await fetch(`${base}/health`)).json();
    assert.equal(h.data.status, 'ok');
    const cfg = await (await fetch(`${base}/config/server`)).json();
    assert.equal(cfg.data.mode, 'demo');
    assert.ok(cfg.data.modules.includes('economy'));
  } finally { await stop(); }
});

test('demo login issues a token and /player/me reflects identity', async () => {
  const { base, stop } = await startServer();
  try {
    const token = await demoToken(base, MARCO);
    assert.ok(token);
    const me = await (await fetch(`${base}/player/me`, auth(token))).json();
    assert.equal(me.data.display_name, 'Marco Rossi');
    assert.equal(me.data.phone_number, '+39 351 0099 412'); // identity active
    assert.deepEqual(me.data.capabilities, []);
  } finally { await stop(); }
});

test('layout is resolved per capability: citizen vs agent', async () => {
  const { base, stop } = await startServer();
  try {
    const marcoLayout = (await (await fetch(`${base}/config/layout`, auth(await demoToken(base, MARCO)))).json()).data.dashboard;
    const justice = marcoLayout.tabs.find((t) => t.id === 'justice');
    const ids = justice.widgets.map((w) => w.widget);
    assert.ok(ids.includes('justice.casellario'));
    assert.ok(!ids.includes('justice.dossiers.agent')); // gated away for citizen
    assert.ok(!ids.includes('justice.wanted'));

    const giuliaLayout = (await (await fetch(`${base}/config/layout`, auth(await demoToken(base, GIULIA)))).json()).data.dashboard;
    const justiceG = giuliaLayout.tabs.find((t) => t.id === 'justice');
    assert.ok(justiceG.widgets.map((w) => w.widget).includes('justice.dossiers.agent'));
  } finally { await stop(); }
});

test('widget endpoint enforces capability the same as the layout', async () => {
  const { base, stop } = await startServer();
  try {
    const marco = await demoToken(base, MARCO);
    const giulia = await demoToken(base, GIULIA);

    const balance = await fetch(`${base}/widget/economy.balance`, auth(marco));
    assert.equal(balance.status, 200);
    assert.equal((await balance.json()).data.currency, 'EUR');

    const wantedAsCitizen = await fetch(`${base}/widget/justice.wanted`, auth(marco));
    assert.equal(wantedAsCitizen.status, 403);
    assert.equal((await wantedAsCitizen.json()).error.code, 'WIDGET_FORBIDDEN');

    const wantedAsAgent = await fetch(`${base}/widget/justice.wanted`, auth(giulia));
    assert.equal(wantedAsAgent.status, 200);
    assert.ok((await wantedAsAgent.json()).data.items.length >= 1);
  } finally { await stop(); }
});

test('unknown widget and unauthenticated access are rejected', async () => {
  const { base, stop } = await startServer();
  try {
    const token = await demoToken(base, MARCO);
    const unknown = await fetch(`${base}/widget/does.not.exist`, auth(token));
    assert.equal(unknown.status, 404);
    assert.equal((await unknown.json()).error.code, 'WIDGET_UNKNOWN');

    const noAuth = await fetch(`${base}/widget/economy.balance`);
    assert.equal(noAuth.status, 401);
    assert.equal((await noAuth.json()).error.code, 'UNAUTHENTICATED');
  } finally { await stop(); }
});

test('an inactive module answers WIDGET_NOT_AVAILABLE', async () => {
  const { base, stop } = await startServer((config) => {
    config.modules.active = config.modules.active.filter((m) => m !== 'health');
  });
  try {
    const token = await demoToken(base, MARCO);
    const res = await fetch(`${base}/widget/health.record`, auth(token));
    assert.equal(res.status, 404);
    assert.equal((await res.json()).error.code, 'WIDGET_NOT_AVAILABLE');
  } finally { await stop(); }
});

test('politics.laws filters by status', async () => {
  const { base, stop } = await startServer();
  try {
    const token = await demoToken(base, MARCO);
    const archived = await (await fetch(`${base}/widget/politics.laws?status=archived`, auth(token))).json();
    assert.ok(archived.data.items.length >= 1);
    assert.ok(archived.data.items.every((l) => l.status === 'archived'));
  } finally { await stop(); }
});

test('transactions paginate', async () => {
  const { base, stop } = await startServer();
  try {
    const token = await demoToken(base, MARCO);
    const page1 = await (await fetch(`${base}/widget/economy.transactions?page=1&limit=2`, auth(token))).json();
    assert.equal(page1.data.items.length, 2);
    assert.equal(page1.data.limit, 2);
    assert.ok(page1.data.total >= 3);
    assert.ok(page1.data.pages >= 2);
  } finally { await stop(); }
});

test('demo-only writes: mark notification read and simulate a transfer', async () => {
  const { base, stop } = await startServer();
  try {
    const token = await demoToken(base, MARCO);
    const feed = await (await fetch(`${base}/widget/notifications.feed`, auth(token))).json();
    const id = feed.data.items[0].id;
    const read = await fetch(`${base}/widget/notifications.feed/${id}/read`, { method: 'POST', ...auth(token) });
    assert.equal(read.status, 200);
    const after = await (await fetch(`${base}/widget/notifications.feed?unread=true`, auth(token))).json();
    assert.ok(after.data.items.every((n) => n.id !== id));

    const transfer = await fetch(`${base}/widget/economy.transfers`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${token}` },
      body: JSON.stringify({ to: 'Anna Verdi', amount: 25, note: 'Test' }),
    });
    assert.equal(transfer.status, 200);
    assert.equal((await transfer.json()).data.ok, true);
  } finally { await stop(); }
});

test('demo profiles are listed', async () => {
  const { base, stop } = await startServer();
  try {
    const res = await (await fetch(`${base}/demo/profiles`)).json();
    assert.equal(res.data.items.length, 5);
    assert.ok(res.data.items.every((p) => p.uuid && p.display_name && p.blurb));
  } finally { await stop(); }
});
