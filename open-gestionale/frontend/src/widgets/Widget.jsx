import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { getWidgetComponent } from './registry.jsx';

/*
 * A self-contained widget. It knows its own endpoint (via the widget id),
 * manages its four states, and never knows about other widgets. If the module
 * is missing the API answers WIDGET_NOT_AVAILABLE and we render the graceful
 * "non disponibile" state instead of crashing.
 */
export default function Widget({ widgetId, label, variant = 'panel', params }) {
  const [state, setState] = useState({ status: 'loading' });
  const paramsKey = params ? JSON.stringify(params) : '';

  const load = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const data = await api.widget(widgetId, params);
      setState({ status: 'data', data });
    } catch (err) {
      if (err.code === 'WIDGET_NOT_AVAILABLE' || err.code === 'WIDGET_FORBIDDEN') {
        setState({ status: 'unavailable', err });
      } else {
        setState({ status: 'error', err });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [widgetId, paramsKey]);

  useEffect(() => { load(); }, [load]);

  const Comp = getWidgetComponent(widgetId);

  if (variant === 'hero') {
    return (
      <div className={`hero-card hero-card--${state.status}`}>
        {state.status === 'loading' && <div className="skeleton skeleton--hero" />}
        {state.status === 'unavailable' && <HeroUnavailable label={label} />}
        {state.status === 'error' && <HeroError label={label} onRetry={load} />}
        {state.status === 'data' && Comp &&
          <Comp data={state.data} label={label} variant="hero" reload={load} />}
      </div>
    );
  }

  return (
    <section className="panel" aria-busy={state.status === 'loading'}>
      <header className="panel__head">
        <h3>{label}</h3>
        {state.status === 'error' && (
          <button className="panel__retry" onClick={load} type="button">Riprova</button>
        )}
      </header>
      <div className="panel__body">
        {state.status === 'loading' && <PanelSkeleton />}
        {state.status === 'unavailable' && (
          <p className="muted">Questo modulo non è disponibile su questo server.</p>
        )}
        {state.status === 'error' && (
          <p className="error-text">{(state.err && state.err.message) || 'Errore di caricamento.'}</p>
        )}
        {state.status === 'data' && Comp && (
          <Comp data={state.data} label={label} variant="panel" reload={load} />
        )}
        {state.status === 'data' && !Comp && (
          <pre className="rawdata">{JSON.stringify(state.data, null, 2)}</pre>
        )}
      </div>
    </section>
  );
}

function PanelSkeleton() {
  return (
    <div className="skeleton-stack">
      <div className="skeleton" /><div className="skeleton" style={{ width: '70%' }} />
      <div className="skeleton" style={{ width: '85%' }} />
    </div>
  );
}

function HeroUnavailable({ label }) {
  return (<><span className="hero-card__label">{label}</span><strong className="hero-card__value muted">—</strong></>);
}
function HeroError({ label, onRetry }) {
  return (<><span className="hero-card__label">{label}</span>
    <button className="hero-card__retry" onClick={onRetry} type="button">Riprova</button></>);
}
