/* Tiny shared UI primitives used by widgets. No dependency, just classes the
   stylesheet knows about. */

export function Empty({ children }) {
  return <p className="muted empty">{children || 'Nessun dato.'}</p>;
}

export function Tag({ tone, children }) {
  return <span className={`tag ${tone ? `tag--${tone}` : ''}`}>{children}</span>;
}

export function KV({ rows }) {
  return (
    <dl className="kv">
      {rows.filter(([, v]) => v != null && v !== '').map(([k, v]) => (
        <div className="kv__row" key={k}>
          <dt>{k}</dt><dd>{v}</dd>
        </div>
      ))}
    </dl>
  );
}

export function HeroValue({ label, value, sub }) {
  return (
    <>
      <span className="hero-card__label">{label}</span>
      <strong className="hero-card__value">{value}</strong>
      {sub && <span className="hero-card__sub">{sub}</span>}
    </>
  );
}

/* Maps a status-ish string to a colour tone. */
export function statusTone(status) {
  const s = String(status || '').toLowerCase();
  if (['active', 'attivo', 'valido', 'confermato', 'closed', 'chiuso'].includes(s)) return 'green';
  if (['in corso', 'in_istruttoria', 'in agenda', 'open', 'aperto', 'pending'].includes(s)) return 'amber';
  if (['archived', 'archiviato', 'dissolved', 'scaduto', 'revocato'].includes(s)) return 'grey';
  return null;
}
