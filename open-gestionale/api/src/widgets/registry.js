'use strict';

/*
 * Widget registry — the single source of truth that maps every widget id to the
 * module that provides it and the capability (if any) needed to see it.
 *
 * The gestionale itself is ambientazione-neutral: it does not know what a widget
 * shows. It only knows which module a widget belongs to and which capability a
 * player needs. Everything else (label, position, ordering) comes from the
 * server layout. If a module is not active, every widget that belongs to it is
 * removed from the resolved layout and its endpoint answers WIDGET_NOT_AVAILABLE.
 *
 * kind:
 *   - "singleton"   one object for the current player (balance, profile, ...)
 *   - "collection"  a list, usually paginated (transactions, dossiers, ...)
 *   - "detail"      a single resource addressed by ?id= (company.detail, ...)
 *
 * capability (optional): a string the player must carry to see the widget. These
 * are read from the player record in the server DB — the gestionale never owns a
 * permission model of its own.
 */

const MODULES = Object.freeze([
  'core',
  'economy',
  'companies',
  'openfdo',
  'politics',
  'jobs',
  'health',
  'identity',
  'education',
]);

const WIDGETS = Object.freeze({
  // ---- Core (always available) -------------------------------------------
  'player.profile':       { module: 'core',      kind: 'singleton',  label: 'Profilo' },
  'player.weekly_time':   { module: 'core',      kind: 'singleton',  label: 'Tempo settimanale' },
  'notifications.feed':   { module: 'core',      kind: 'collection', label: 'Notifiche' },
  'server.news':          { module: 'core',      kind: 'collection', label: 'Notizie' },

  // ---- Open Economy / Open Bank ------------------------------------------
  'economy.balance':      { module: 'economy',   kind: 'singleton',  label: 'Bilancio' },
  'economy.transactions': { module: 'economy',   kind: 'collection', label: 'Transazioni' },
  'economy.cards':        { module: 'economy',   kind: 'collection', label: 'Carte' },
  'economy.transfers':    { module: 'economy',   kind: 'singleton',  label: 'Bonifico', write: true },

  // ---- Open Companies -----------------------------------------------------
  'company.mine':         { module: 'companies', kind: 'collection', label: 'Le mie aziende' },
  'company.detail':       { module: 'companies', kind: 'detail',     label: 'Azienda' },
  'company.registry':     { module: 'companies', kind: 'collection', label: 'Registro aziende' },

  // ---- OpenFDO ------------------------------------------------------------
  'justice.casellario':     { module: 'openfdo', kind: 'singleton',  label: 'Casellario' },
  'justice.dossiers':       { module: 'openfdo', kind: 'collection', label: 'Fascicoli' },
  'justice.dossiers.agent': { module: 'openfdo', kind: 'collection', label: 'Fascicoli gestiti', capability: 'fdo.agent' },
  'justice.wanted':         { module: 'openfdo', kind: 'collection', label: 'Ricercati',         capability: 'fdo.agent' },
  'justice.service_sheet':  { module: 'openfdo', kind: 'singleton',  label: 'Foglio di servizio', capability: 'fdo.agent' },

  // ---- Open Politics ------------------------------------------------------
  'politics.charges':     { module: 'politics',  kind: 'collection', label: 'Cariche pubbliche' },
  'politics.laws':        { module: 'politics',  kind: 'collection', label: 'Leggi' },
  'politics.elections':   { module: 'politics',  kind: 'singleton',  label: 'Elezioni' },
  'politics.decrees':     { module: 'politics',  kind: 'collection', label: 'Decreti' },

  // ---- Open Jobs ----------------------------------------------------------
  'jobs.current':         { module: 'jobs',      kind: 'singleton',  label: 'Lavoro' },
  'jobs.history':         { module: 'jobs',      kind: 'collection', label: 'Storico impieghi' },
  'jobs.postings':        { module: 'jobs',      kind: 'collection', label: 'Offerte di lavoro' },

  // ---- Open Health --------------------------------------------------------
  'health.record':        { module: 'health',    kind: 'singleton',  label: 'Cartella clinica' },
  'health.appointments':  { module: 'health',    kind: 'collection', label: 'Appuntamenti' },

  // ---- Open Identity ------------------------------------------------------
  'identity.documents':   { module: 'identity',  kind: 'collection', label: 'Documenti' },
  'identity.phone':       { module: 'identity',  kind: 'singleton',  label: 'Telefono' },

  // ---- Open Education -----------------------------------------------------
  'education.degrees':    { module: 'education', kind: 'collection', label: 'Titoli di studio' },
  'education.enrollments':{ module: 'education', kind: 'collection', label: 'Corsi attivi' },
});

function getWidget(id) {
  return WIDGETS[id] || null;
}

function isKnownModule(id) {
  return MODULES.includes(id);
}

module.exports = { MODULES, WIDGETS, getWidget, isKnownModule };
