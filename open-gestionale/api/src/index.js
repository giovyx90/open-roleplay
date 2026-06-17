'use strict';

const { loadConfig, loadLayout } = require('./config');
const { createDataSource } = require('./datasource');
const { createApp } = require('./app');

/*
 * Process entry point: read config + layout, open the data source, start the
 * server. Everything testable lives elsewhere; this file is just wiring.
 */

function main() {
  const config = loadConfig();
  const layout = loadLayout(config.layoutPath);
  const dataSource = createDataSource(config);
  const app = createApp({ config, dataSource, layout });

  const server = app.listen(config.api.port, () => {
    // eslint-disable-next-line no-console
    console.log(
      `[open-gestionale] API in ascolto su :${config.api.port} ` +
      `(mode=${config.mode}, moduli=${config.modules.active.join(',')})`
    );
  });

  for (const sig of ['SIGINT', 'SIGTERM']) {
    process.on(sig, () => {
      server.close(() => {
        if (dataSource.close) dataSource.close();
        process.exit(0);
      });
    });
  }
}

if (require.main === module) {
  try {
    main();
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('[open-gestionale] avvio fallito:', err.message);
    process.exit(1);
  }
}

module.exports = { main };
