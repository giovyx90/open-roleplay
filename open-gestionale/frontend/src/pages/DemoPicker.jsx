import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import Splash from '../components/Splash.jsx';

/*
 * Login-free demo landing: pick one of the seeded profiles and explore. Each
 * profile sees different widgets depending on its capabilities — exactly like
 * a real player would.
 */
export default function DemoPicker() {
  const { server, demoLogin } = useAuth();
  const [profiles, setProfiles] = useState(null);
  const [busy, setBusy] = useState(null);
  const [err, setErr] = useState(null);

  useEffect(() => {
    api.demoProfiles().then((d) => setProfiles(d.items)).catch((e) => setErr(e));
  }, []);

  async function pick(uuid) {
    setBusy(uuid);
    try { await demoLogin(uuid); }
    catch (e) { setErr(e); setBusy(null); }
  }

  if (err) return <Splash tone="error" label="Demo non disponibile" detail={err.message} />;
  if (!profiles) return <Splash label="Caricamento profili…" />;

  return (
    <div className="demo">
      <div className="demo__banner">Demo con dati fittizi · nessun login · nessun dato reale</div>
      <div className="demo__inner">
        <h1>{server ? server.name : 'Open Gestionale'}</h1>
        <p className="muted">Scegli un personaggio per esplorare il gestionale. Ogni profilo vede widget diversi in base al ruolo.</p>
        <div className="demo__grid">
          {profiles.map((p) => (
            <button key={p.uuid} className="profile-card" type="button" disabled={!!busy} onClick={() => pick(p.uuid)}>
              <span className="profile-card__avatar" aria-hidden="true">{initials(p.display_name)}</span>
              <strong>{p.display_name}</strong>
              <span className="profile-card__rank">{p.rank_label || p.rank}{p.corps_label ? ` · ${p.corps_label}` : ''}</span>
              <span className="profile-card__blurb">{p.blurb}</span>
              <span className="profile-card__cta">{busy === p.uuid ? 'Accesso…' : 'Esplora →'}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function initials(name) {
  return String(name || '?').split(/\s+/).map((w) => w[0]).slice(0, 2).join('').toUpperCase();
}
