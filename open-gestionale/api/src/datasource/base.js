'use strict';

/*
 * BaseDataSource holds every bit of domain logic the widgets need (pagination,
 * membership filtering, notification read-state, dossier lookups) expressed in
 * terms of a handful of primitives. Concrete sources only implement the
 * primitives: DemoDataSource over an in-memory seed, SqliteDataSource over SQL.
 * This is why the demo and a real DB return byte-identical widget payloads —
 * proven by test/parity.test.js.
 *
 * The gestionale is read-mostly. The only mutations a source must support are:
 *   - consumeOtp / markNotificationRead  (gestionale-owned state, section 10)
 *   - recordTransfer                     (demo-only simulation of the one write)
 * Gameplay data is never written here.
 */

function paginate(items, query) {
  const all = Array.isArray(items) ? items : [];
  const limit = clampInt(query && query.limit, 20, 1, 200);
  const page = clampInt(query && query.page, 1, 1, 1e9);
  const total = all.length;
  const pages = Math.max(1, Math.ceil(total / limit));
  const start = (page - 1) * limit;
  return { items: all.slice(start, start + limit), page, limit, total, pages };
}

function clampInt(value, fallback, min, max) {
  const n = parseInt(value, 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

class BaseDataSource {
  /* ---- primitives to override --------------------------------------- */
  async _player(/* uuid */) { throw new Error('not implemented'); }
  async _allPlayers() { throw new Error('not implemented'); }
  async _company(/* id */) { throw new Error('not implemented'); }
  async _allCompanies() { throw new Error('not implemented'); }
  async _dossier(/* id */) { throw new Error('not implemented'); }
  async _global(/* key */) { throw new Error('not implemented'); }
  async _isNotificationRead(/* uuid, id */) { return false; }
  async _setNotificationRead(/* uuid, id */) {}
  async _otp(/* uuid */) { return null; }
  async _consumeOtp(/* uuid */) {}
  async _recordTransfer(/* uuid, tx */) { throw new Error('transfers non disponibili'); }

  /* ---- identity & auth ---------------------------------------------- */
  async getPlayer(uuid) {
    return (await this._player(uuid)) || null;
  }

  async listPublicProfiles() {
    const players = await this._allPlayers();
    return players.map((p) => ({
      uuid: p.uuid,
      display_name: p.display_name,
      rank: p.rank,
      rank_label: p.rank_label || null,
      corps: p.corps || null,
      corps_label: p.corps_label || null,
      blurb: p.blurb || null,
    }));
  }

  async getOtpRecord(uuid) { return this._otp(uuid); }
  async consumeOtp(uuid) { return this._consumeOtp(uuid); }

  /* ---- core widgets -------------------------------------------------- */
  profile(player) {
    return {
      uuid: player.uuid,
      display_name: player.display_name,
      rank: player.rank,
      rank_label: player.rank_label || null,
      corps: player.corps || null,
      corps_label: player.corps_label || null,
      registered_at: player.registered_at || null,
      last_seen: player.last_seen || null,
    };
  }

  weeklyTime(player) {
    const minutes = Number(player.weekly_minutes) || 0;
    return { minutes, hours: Math.floor(minutes / 60), remainder: minutes % 60 };
  }

  async notifications(player, query) {
    const list = Array.isArray(player.notifications) ? player.notifications : [];
    const out = [];
    for (const n of list) {
      const read = await this._isNotificationRead(player.uuid, n.id);
      out.push(Object.assign({}, n, { read }));
    }
    const filtered = query && truthy(query.unread) ? out.filter((n) => !n.read) : out;
    return { items: filtered, unread: out.filter((n) => !n.read).length };
  }

  async markNotificationRead(player, id) {
    const list = Array.isArray(player.notifications) ? player.notifications : [];
    if (!list.some((n) => n.id === id)) return false;
    await this._setNotificationRead(player.uuid, id);
    return true;
  }

  async news() {
    const list = (await this._global('news')) || [];
    return [...list].sort((a, b) => Number(b.pinned) - Number(a.pinned) ||
      String(b.published_at).localeCompare(String(a.published_at)));
  }

  /* ---- economy ------------------------------------------------------- */
  balance(player) { return (player.economy && player.economy.balance) || null; }
  transactions(player, query) { return paginate(player.economy && player.economy.transactions, query); }
  cards(player) { return { items: (player.economy && player.economy.cards) || [] }; }

  transferTargets(player) {
    return {
      accounts: ((player.economy && player.economy.balance && player.economy.balance.accounts) || []),
      recent: ((player.economy && player.economy.transactions) || []).slice(0, 5),
    };
  }
  async recordTransfer(player, tx) { return this._recordTransfer(player.uuid, tx); }

  /* ---- companies ----------------------------------------------------- */
  async companiesOf(uuid) {
    const all = await this._allCompanies();
    return all
      .filter((c) => c.owner_uuid === uuid || (c.members || []).some((m) => m.uuid === uuid))
      .map((c) => this._companyMine(c, uuid));
  }

  _companyMine(c, uuid) {
    const role = c.owner_uuid === uuid ? 'owner' :
      ((c.members || []).find((m) => m.uuid === uuid) || {}).role || 'member';
    return { id: c.id, name: c.name, sector: c.sector, status: c.status, role, hq: c.hq || null };
  }

  async company(id, uuid) {
    const c = await this._company(id);
    if (!c) return null;
    const isMember = c.owner_uuid === uuid || (c.members || []).some((m) => m.uuid === uuid);
    // Public registry detail is visible to everyone; treasury/ledger only to members.
    const base = {
      id: c.id, name: c.name, sector: c.sector, status: c.status,
      hq: c.hq || null, licenses: c.licenses || [], founded_at: c.founded_at || null,
      members: (c.members || []).map((m) => ({ name: m.name, role: m.role })),
    };
    if (isMember) {
      base.balance = c.balance != null ? c.balance : null;
      base.assets = c.assets || [];
    }
    return base;
  }

  async companyRegistry() {
    const all = await this._allCompanies();
    return {
      items: all
        .filter((c) => c.status !== 'dissolved')
        .map((c) => ({ id: c.id, name: c.name, sector: c.sector, status: c.status, hq: c.hq || null })),
    };
  }

  /* ---- justice (OpenFDO) -------------------------------------------- */
  casellario(player) {
    return (player.justice && player.justice.casellario) || { clean: true, entries: [] };
  }

  async dossiersForSubject(player) {
    const ids = (player.justice && player.justice.dossier_subject_ids) || [];
    const items = [];
    for (const id of ids) {
      const d = await this._dossier(id);
      if (d) items.push(this._dossierSummary(d));
    }
    return { items, role: 'subject' };
  }

  async dossiersForAgent(player) {
    const ids = (player.justice && player.justice.dossier_agent_ids) || [];
    const items = [];
    for (const id of ids) {
      const d = await this._dossier(id);
      if (d) items.push(this._dossierSummary(d));
    }
    return { items, role: 'agent' };
  }

  async dossier(id) {
    const d = await this._dossier(id);
    return d || null;
  }

  _dossierSummary(d) {
    return {
      id: d.id, subject_name: d.subject_name, corps: d.corps, status: d.status,
      opened_at: d.opened_at, charges: (d.charges || []).map((c) => c.label),
      has_verdict: !!d.verdict,
    };
  }

  async wanted() { return { items: (await this._global('wanted')) || [] }; }

  serviceSheet(player) {
    return (player.justice && player.justice.service_sheet) || null;
  }

  /* ---- politics ------------------------------------------------------ */
  charges(player) { return { items: (player.politics && player.politics.charges) || [] }; }

  async laws(query) {
    const all = (await this._global('laws')) || [];
    const status = query && query.status;
    const items = status ? all.filter((l) => l.status === status) : all;
    return { items };
  }

  async election() { return (await this._global('election')) || null; }
  async decrees() { return { items: (await this._global('decrees')) || [] }; }

  /* ---- jobs ---------------------------------------------------------- */
  jobCurrent(player) { return (player.jobs && player.jobs.current) || null; }
  jobHistory(player) { return { items: (player.jobs && player.jobs.history) || [] }; }
  async jobPostings() { return { items: (await this._global('jobPostings')) || [] }; }

  /* ---- health -------------------------------------------------------- */
  healthRecord(player) { return (player.health && player.health.record) || null; }
  appointments(player) { return { items: (player.health && player.health.appointments) || [] }; }

  /* ---- identity ------------------------------------------------------ */
  documents(player) { return { items: (player.identity && player.identity.documents) || [] }; }
  phone(player) { return (player.identity && player.identity.phone) || null; }

  /* ---- education ----------------------------------------------------- */
  degrees(player) { return { items: (player.education && player.education.degrees) || [] }; }
  enrollments(player) { return { items: (player.education && player.education.enrollments) || [] }; }
}

function truthy(v) {
  return v === true || v === 'true' || v === '1' || v === 1;
}

module.exports = { BaseDataSource, paginate };
