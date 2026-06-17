import { useState } from 'react';
import { api } from '../api/client.js';
import { Empty, Tag, statusTone } from '../components/ui.jsx';
import { date } from '../components/format.js';

function Charges({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna carica pubblica.</Empty>;
  return (
    <ul className="charges">
      {items.map((c) => (
        <li key={c.id}><strong>{c.office}</strong><span className="muted"> · {c.area}</span>
          <time className="muted"> · dal {date(c.since)}</time></li>
      ))}
    </ul>
  );
}

function Laws({ data }) {
  const [items, setItems] = useState((data && data.items) || []);
  const [filter, setFilter] = useState('');
  const [busy, setBusy] = useState(false);

  async function apply(status) {
    setBusy(true); setFilter(status);
    try { const d = await api.widget('politics.laws', status ? { status } : undefined); setItems(d.items || []); }
    finally { setBusy(false); }
  }

  return (
    <div className={busy ? 'is-busy' : ''}>
      <div className="seg">
        {[['', 'Tutte'], ['active', 'Vigenti'], ['archived', 'Archiviate']].map(([v, l]) => (
          <button key={v} type="button" className={filter === v ? 'is-active' : ''} onClick={() => apply(v)}>{l}</button>
        ))}
      </div>
      {!items.length ? <Empty>Nessuna legge.</Empty> : (
        <ul className="laws">
          {items.map((l) => (
            <li key={l.id} className="laws__item">
              <div><strong>L. {l.number} — {l.title}</strong><br /><span className="muted">{l.summary}</span></div>
              <div className="laws__right"><Tag tone={statusTone(l.status)}>{l.status === 'active' ? 'Vigente' : 'Archiviata'}</Tag>
                <time className="muted">{date(l.enacted_at)}</time></div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Elections({ data }) {
  if (!data) return <Empty>Nessuna elezione in programma.</Empty>;
  const max = Math.max(1, ...(data.candidates || []).map((c) => c.poll || 0));
  return (
    <div className="election">
      <div className="election__head">
        <strong>{data.title}</strong>
        <Tag tone={statusTone(data.status)}>{data.status}</Tag>
      </div>
      <p className="muted">Dal {date(data.opens_at)} al {date(data.closes_at)} · affluenza {data.turnout}%</p>
      <ul className="polls">
        {(data.candidates || []).map((c, i) => (
          <li key={i} className="polls__row">
            <div className="polls__label"><strong>{c.name}</strong><span className="muted"> {c.party}</span></div>
            <div className="polls__bar"><span style={{ width: `${(c.poll / max) * 100}%` }} /></div>
            <div className="polls__val">{c.poll}%</div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Decrees({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun decreto.</Empty>;
  return (
    <ul className="laws">
      {items.map((d) => (
        <li key={d.id} className="laws__item">
          <div><strong>{d.number} — {d.title}</strong><br /><span className="muted">{d.authority}</span></div>
          <time className="muted">{date(d.issued_at)}</time>
        </li>
      ))}
    </ul>
  );
}

export const widgets = {
  'politics.charges': Charges,
  'politics.laws': Laws,
  'politics.elections': Elections,
  'politics.decrees': Decrees,
};
