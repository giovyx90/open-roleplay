import * as core from './core.jsx';
import * as economy from './economy.jsx';
import * as companies from './companies.jsx';
import * as justice from './justice.jsx';
import * as politics from './politics.jsx';
import * as jobs from './jobs.jsx';
import * as health from './health.jsx';
import * as identity from './identity.jsx';
import * as education from './education.jsx';

/*
 * Frontend widget registry: widget id -> React component. It mirrors the API
 * registry but lives here because rendering is a frontend concern. A widget id
 * the frontend does not know about falls back to a raw JSON view, so a server
 * that ships a custom widget still shows *something*.
 */
const REGISTRY = {
  ...core.widgets,
  ...economy.widgets,
  ...companies.widgets,
  ...justice.widgets,
  ...politics.widgets,
  ...jobs.widgets,
  ...health.widgets,
  ...identity.widgets,
  ...education.widgets,
};

export function getWidgetComponent(widgetId) {
  return REGISTRY[widgetId] || null;
}
