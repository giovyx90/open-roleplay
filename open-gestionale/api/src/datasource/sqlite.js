'use strict';

const { DatabaseSync } = require('node:sqlite');
const { BaseDataSource } = require('./base');

/*
 * Live SQLite source. Reads the Open Roleplay database that the Paper plugins
 * write during play, plus the gestionale-owned tables defined in db/schema.sql
 * (sessions, otp, news, notification read-state). Gameplay rows are stored as
 * JSON documents whose shape mirrors the seed, so a server can populate them
 * from its own plugin tables (see db/import-seed.js for the reference loader)
 * and the exact same widget handlers run against demo and production alike.
 *
 * Read-mostly: the only statements that write touch gestionale-owned tables.
 */

class SqliteDataSource extends BaseDataSource {
  constructor(dbPath, { readonly = false } = {}) {
    super();
    this.db = new DatabaseSync(dbPath, { readOnly: readonly });
    this._stmts = {};
  }

  _q(sql) {
    if (!this._stmts[sql]) this._stmts[sql] = this.db.prepare(sql);
    return this._stmts[sql];
  }

  _doc(row) { return row && row.doc ? JSON.parse(row.doc) : null; }

  async _player(uuid) {
    return this._doc(this._q('SELECT doc FROM players WHERE uuid = ?').get(uuid));
  }

  async _allPlayers() {
    return this._q('SELECT doc FROM players').all().map((r) => JSON.parse(r.doc));
  }

  async _company(id) {
    return this._doc(this._q('SELECT doc FROM companies WHERE id = ?').get(id));
  }

  async _allCompanies() {
    return this._q('SELECT doc FROM companies').all().map((r) => JSON.parse(r.doc));
  }

  async _dossier(id) {
    return this._doc(this._q('SELECT doc FROM dossiers WHERE id = ?').get(id));
  }

  async _global(key) {
    if (key === 'news') {
      // News is gestionale-owned (section 10): read the dedicated table.
      return this._q(
        'SELECT id, title, body, author_uuid AS author, published_at, pinned FROM gestionale_news'
      ).all().map((r) => Object.assign({}, r, { pinned: !!r.pinned }));
    }
    return this._doc(this._q('SELECT doc FROM globals WHERE key = ?').get(key));
  }

  async _isNotificationRead(uuid, id) {
    const row = this._q(
      'SELECT 1 AS hit FROM gestionale_notifications_read WHERE player_uuid = ? AND notification_id = ?'
    ).get(uuid, id);
    return !!row;
  }

  async _setNotificationRead(uuid, id) {
    this._q(
      'INSERT OR IGNORE INTO gestionale_notifications_read (player_uuid, notification_id, read_at) VALUES (?, ?, ?)'
    ).run(uuid, id, Date.now());
  }

  async _otp(uuid) {
    return this._q(
      'SELECT otp_hash, expires_at, used FROM gestionale_otp WHERE player_uuid = ?'
    ).get(uuid) || null;
  }

  async _consumeOtp(uuid) {
    this._q('UPDATE gestionale_otp SET used = 1 WHERE player_uuid = ?').run(uuid);
  }

  close() {
    try { this.db.close(); } catch (_) { /* already closed */ }
  }
}

module.exports = { SqliteDataSource };
