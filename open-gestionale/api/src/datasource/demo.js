'use strict';

const fs = require('fs');
const { BaseDataSource } = require('./base');

/*
 * Demo source: the whole world is the seed file held in memory. No real
 * database is touched. Mutations (notification read-state, simulated transfers)
 * live only for the lifetime of the process and reset on restart — exactly what
 * a public, login-free demo wants.
 */

class DemoDataSource extends BaseDataSource {
  constructor(seed) {
    super();
    this.seed = seed;
    this._players = new Map((seed.players || []).map((p) => [p.uuid, p]));
    this._companies = seed.companies || [];
    this._dossiers = new Map((seed.dossiers || []).map((d) => [d.id, d]));
    this._read = new Map(); // uuid -> Set(notificationId)
    this.demo = true;
  }

  static fromFile(file) {
    return new DemoDataSource(JSON.parse(fs.readFileSync(file, 'utf8')));
  }

  async _player(uuid) { return this._players.get(uuid) || null; }
  async _allPlayers() { return [...this._players.values()]; }
  async _company(id) { return this._companies.find((c) => c.id === id) || null; }
  async _allCompanies() { return this._companies; }
  async _dossier(id) { return this._dossiers.get(id) || null; }
  async _global(key) { return this.seed[key] || null; }

  async _isNotificationRead(uuid, id) {
    const set = this._read.get(uuid);
    return !!(set && set.has(id));
  }

  async _setNotificationRead(uuid, id) {
    if (!this._read.has(uuid)) this._read.set(uuid, new Set());
    this._read.get(uuid).add(id);
  }

  async _recordTransfer(uuid, tx) {
    const player = this._players.get(uuid);
    if (!player || !player.economy) throw new Error('Conto non disponibile.');
    const amount = Number(tx.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      const err = new Error('Importo non valido.'); err.code = 'BAD_REQUEST'; throw err;
    }
    const bal = player.economy.balance || {};
    const entry = {
      id: `demo-${Date.now()}`,
      ts: new Date().toISOString(),
      type: 'transfer_out',
      amount: -amount,
      currency: bal.currency || 'EUR',
      counterparty: tx.to || 'Sconosciuto',
      description: tx.note || 'Bonifico (demo)',
    };
    player.economy.transactions = [entry, ...(player.economy.transactions || [])];
    if (typeof bal.bank === 'number') bal.bank = Math.round((bal.bank - amount) * 100) / 100;
    return { ok: true, transaction: entry, balance: bal };
  }
}

module.exports = { DemoDataSource };
