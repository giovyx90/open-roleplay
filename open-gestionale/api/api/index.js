'use strict';

const path = require('path');
const { loadConfig, loadLayout } = require('../src/config');
const { createDataSource } = require('../src/datasource');
const { createApp } = require('../src/app');

let app;

function getApp() {
  if (!app) {
    const env = Object.assign({}, process.env, {
      GESTIONALE_CONFIG: process.env.GESTIONALE_CONFIG ||
        path.join(__dirname, '..', 'config', 'gestionale.demo.vercel.yml'),
      GESTIONALE_LAYOUT: process.env.GESTIONALE_LAYOUT ||
        path.join(__dirname, '..', 'config', 'layout.demo.vercel.yml'),
      GESTIONALE_SEED: process.env.GESTIONALE_SEED ||
        path.join(__dirname, '..', 'config', 'seed.demo.vercel.json'),
      GESTIONALE_MODE: process.env.GESTIONALE_MODE || 'demo',
    });
    const config = loadConfig({ env, configPath: env.GESTIONALE_CONFIG });
    const layout = loadLayout(config.layoutPath);
    const dataSource = createDataSource(config);
    app = createApp({ config, dataSource, layout });
  }
  return app;
}

module.exports = (req, res) => getApp()(req, res);
