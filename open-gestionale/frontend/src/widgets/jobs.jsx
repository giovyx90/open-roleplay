import { Empty, KV, HeroValue } from '../components/ui.jsx';
import { money, date } from '../components/format.js';

function Current({ data, label, variant }) {
  if (!data) {
    if (variant === 'hero') return <HeroValue label={label} value="Disoccupato" />;
    return <Empty>Nessun impiego attivo.</Empty>;
  }
  if (variant === 'hero') return <HeroValue label={label} value={data.title} sub={data.employer} />;
  return (
    <KV rows={[
      ['Ruolo', data.title],
      ['Datore', data.employer],
      ['Stipendio', data.wage ? money(data.wage, data.currency) : '—'],
      ['Ore/sett.', data.hours_week],
      ['Dal', date(data.since)],
    ]} />
  );
}

function History({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuno storico impieghi.</Empty>;
  return (
    <ul className="timeline">
      {items.map((j, i) => (
        <li key={i}><strong>{j.title}</strong> <span className="muted">· {j.employer}</span>
          <time className="muted"> ({date(j.from)} – {date(j.to)})</time></li>
      ))}
    </ul>
  );
}

function Postings({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna offerta di lavoro.</Empty>;
  return (
    <ul className="postings">
      {items.map((p) => (
        <li key={p.id} className="postings__item">
          <div><strong>{p.title}</strong><br /><span className="muted">{p.employer} · {p.location}</span></div>
          <div className="postings__right"><strong>{money(p.wage, p.currency)}</strong>
            <time className="muted">{date(p.posted_at)}</time></div>
        </li>
      ))}
    </ul>
  );
}

export const widgets = {
  'jobs.current': Current,
  'jobs.history': History,
  'jobs.postings': Postings,
};
