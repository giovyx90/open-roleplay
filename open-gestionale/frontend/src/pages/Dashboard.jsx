import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import Widget from '../widgets/Widget.jsx';

const ICONS = {
  wallet: '💳', key: '🔑', scale: '⚖️', landmark: '🏛️', heart: '❤️', cap: '🎓',
  briefcase: '💼', building: '🏢', id: '🪪', user: '👤', trophy: '🏆',
};

/*
 * The dashboard renders whatever the resolved layout contains — it never refers
 * to a specific widget or module by name. Tabs and pages become a single side
 * nav; the sidebar (notifications, news) stays pinned on the right.
 */
export default function Dashboard() {
  const { server, player, layout, logout } = useAuth();
  const dash = (layout && layout.dashboard) || {};
  const sections = [...(dash.tabs || []), ...(dash.pages || [])];
  const [active, setActive] = useState(sections.length ? sections[0].id : null);
  const current = sections.find((s) => s.id === active) || sections[0];

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar__brand">
          {server && server.logo_url
            ? <img src={server.logo_url} alt="" className="topbar__logo" />
            : <span className="topbar__logo topbar__logo--ph" aria-hidden="true" />}
          <strong>{(layout && layout.server_name) || (server && server.name) || 'Gestionale'}</strong>
        </div>
        <div className="topbar__right">
          <div className="topbar__player">
            <strong>{player && player.display_name}</strong>
            <span className="muted">{player && (player.rank_label || player.rank)}{player && player.corps_label ? ` · ${player.corps_label}` : ''}</span>
          </div>
          <button className="btn btn--ghost" type="button" onClick={logout}>Esci</button>
        </div>
      </header>

      {server && server.mode === 'demo' && (
        <div className="app__demobar">Stai esplorando una demo con dati fittizi.</div>
      )}

      <main className="app__main">
        {dash.hero && dash.hero.length > 0 && (
          <div className="hero-row">
            {dash.hero.map((w) => <Widget key={w.widget} widgetId={w.widget} label={w.label} variant="hero" />)}
          </div>
        )}

        <div className="layout">
          <aside className="sidenav" aria-label="Sezioni">
            <nav>
              {sections.map((s) => (
                <button key={s.id} type="button"
                  className={`sidenav__item ${active === s.id ? 'is-active' : ''}`}
                  onClick={() => setActive(s.id)}>
                  <span className="sidenav__icon" aria-hidden="true">{ICONS[s.icon] || '•'}</span>
                  {s.label}
                </button>
              ))}
            </nav>
          </aside>

          <section className="content">
            {!current && <p className="muted">Nessuna sezione disponibile per il tuo profilo.</p>}
            {current && (
              <>
                <h2 className="content__title">{current.label}</h2>
                <div className="content__grid">
                  {current.widgets.map((w) => (
                    <Widget key={w.widget} widgetId={w.widget} label={w.label} variant="panel" />
                  ))}
                </div>
              </>
            )}
          </section>

          {dash.sidebar && dash.sidebar.length > 0 && (
            <aside className="rail" aria-label="Aggiornamenti">
              {dash.sidebar.map((w) => (
                <Widget key={w.widget} widgetId={w.widget} label={w.label} variant="panel" />
              ))}
            </aside>
          )}
        </div>
      </main>
    </div>
  );
}
