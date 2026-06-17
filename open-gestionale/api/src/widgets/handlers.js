'use strict';

/*
 * One handler per widget id. A handler turns the authenticated player + query
 * into the widget's payload, reading through the data source. Handlers are
 * shared across every data source, so the demo and a real DB serve the same
 * widget shapes. Handlers never know which module is active — the route checks
 * that before dispatching here.
 */

const HANDLERS = {
  // ---- core ----
  'player.profile':       (p, ds) => ds.profile(p),
  'player.weekly_time':   (p, ds) => ds.weeklyTime(p),
  'notifications.feed':   (p, ds, q) => ds.notifications(p, q),
  'server.news':          (p, ds) => ds.news().then((items) => ({ items })),

  // ---- economy ----
  'economy.balance':      (p, ds) => ds.balance(p),
  'economy.transactions': (p, ds, q) => ds.transactions(p, q),
  'economy.cards':        (p, ds) => ds.cards(p),
  'economy.transfers':    (p, ds) => ds.transferTargets(p),

  // ---- companies ----
  'company.mine':         (p, ds) => ds.companiesOf(p.uuid).then((items) => ({ items })),
  'company.detail':       (p, ds, q) => requireId(q).then((id) => ds.company(id, p.uuid)),
  'company.registry':     (p, ds) => ds.companyRegistry(),

  // ---- justice (OpenFDO) ----
  'justice.casellario':     (p, ds) => ds.casellario(p),
  'justice.dossiers':       (p, ds) => ds.dossiersForSubject(p),
  'justice.dossiers.agent': (p, ds) => ds.dossiersForAgent(p),
  'justice.wanted':         (p, ds) => ds.wanted(),
  'justice.service_sheet':  (p, ds) => ds.serviceSheet(p),

  // ---- politics ----
  'politics.charges':     (p, ds) => ds.charges(p),
  'politics.laws':        (p, ds, q) => ds.laws(q),
  'politics.elections':   (p, ds) => ds.election(),
  'politics.decrees':     (p, ds) => ds.decrees(),

  // ---- jobs ----
  'jobs.current':         (p, ds) => ds.jobCurrent(p),
  'jobs.history':         (p, ds) => ds.jobHistory(p),
  'jobs.postings':        (p, ds) => ds.jobPostings(),

  // ---- health ----
  'health.record':        (p, ds) => ds.healthRecord(p),
  'health.appointments':  (p, ds) => ds.appointments(p),

  // ---- identity ----
  'identity.documents':   (p, ds) => ds.documents(p),
  'identity.phone':       (p, ds) => ds.phone(p),

  // ---- education ----
  'education.degrees':    (p, ds) => ds.degrees(p),
  'education.enrollments':(p, ds) => ds.enrollments(p),
};

function requireId(query) {
  const id = query && query.id;
  if (!id) {
    const err = new Error('Parametro "id" mancante.');
    err.code = 'BAD_REQUEST';
    return Promise.reject(err);
  }
  return Promise.resolve(id);
}

function getHandler(widgetId) {
  return HANDLERS[widgetId] || null;
}

module.exports = { HANDLERS, getHandler };
