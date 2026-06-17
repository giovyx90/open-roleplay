'use strict';

const { DemoDataSource } = require('./demo');

/*
 * Picks the data source from config. Demo mode reads the seed; real mode opens
 * the server database. MySQL/MariaDB are declared in config for parity with the
 * doc but not implemented here — the SQLite path is the reference. A server on
 * MySQL drops in its own BaseDataSource subclass (same primitives) without the
 * widgets noticing.
 */

function createDataSource(config) {
  if (config.mode === 'demo') {
    if (!config.seedPath) throw new Error('Modalità demo senza demo.seed configurato.');
    return DemoDataSource.fromFile(config.seedPath);
  }
  const type = config.database.type;
  if (type === 'sqlite') {
    if (!config.database.path) throw new Error('database.path mancante per SQLite.');
    // Lazy require so demo mode never loads node:sqlite (avoids the experimental warning).
    const { SqliteDataSource } = require('./sqlite');
    return new SqliteDataSource(config.database.path, { readonly: false });
  }
  throw new Error(
    `database.type "${type}" non implementato in questa build. ` +
    'Usa sqlite, oppure fornisci un BaseDataSource per il tuo DB.'
  );
}

// SqliteDataSource is intentionally not re-exported here so demo mode never
// loads node:sqlite. Import it directly from './sqlite' when needed.
module.exports = { createDataSource, DemoDataSource };
