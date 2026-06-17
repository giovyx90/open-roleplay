import { Empty, Tag, KV, statusTone } from '../components/ui.jsx';
import { date, dateTime } from '../components/format.js';

const WANTED_TONE = { 1: 'amber', 2: 'amber', 3: 'red' };

function Casellario({ data }) {
  if (!data) return <Empty />;
  if (data.clean && (!data.entries || !data.entries.length)) {
    return <p className="ok-text">Fedina penale pulita.</p>;
  }
  return (
    <ul className="record">
      {(data.entries || []).map((e) => (
        <li key={e.id} className="record__item">
          <div><strong>{e.crime}</strong><br /><span className="muted">{e.outcome}</span></div>
          <div className="record__right"><time>{date(e.date)}</time>{e.dossier_id && <code>{e.dossier_id}</code>}</div>
        </li>
      ))}
    </ul>
  );
}

function Dossiers({ data }) {
  const items = (data && data.items) || [];
  const role = data && data.role;
  if (!items.length) {
    return <Empty>{role === 'agent' ? 'Nessun fascicolo in gestione.' : 'Nessun fascicolo a tuo carico.'}</Empty>;
  }
  return (
    <ul className="dossiers">
      {items.map((d) => (
        <li key={d.id} className="dossiers__item">
          <div className="dossiers__head">
            <code>{d.id}</code>
            <Tag tone={statusTone(d.status)}>{labelStatus(d.status)}</Tag>
          </div>
          <p className="dossiers__subject">{role === 'agent' ? d.subject_name : d.corps}</p>
          <p className="muted">{(d.charges || []).join(' · ') || 'Nessun capo d\'imputazione'}</p>
          <time className="muted">Aperto il {date(d.opened_at)}{d.has_verdict ? ' · con sentenza' : ''}</time>
        </li>
      ))}
    </ul>
  );
}

function Wanted({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun ricercato.</Empty>;
  return (
    <ul className="wanted">
      {items.map((w) => (
        <li key={w.uuid} className="wanted__item">
          <div><strong>{w.name}</strong><br /><span className="muted">{w.reason}</span></div>
          <div className="wanted__right">
            <Tag tone={WANTED_TONE[w.level] || 'amber'}>Livello {w.level} · {w.level_label}</Tag>
            <time className="muted">dal {date(w.since)}</time>
          </div>
        </li>
      ))}
    </ul>
  );
}

function ServiceSheet({ data }) {
  if (!data) return <Empty>Foglio di servizio non disponibile.</Empty>;
  return (
    <div>
      <KV rows={[
        ['Corpo', data.corps],
        ['Grado', data.rank],
        ['Matricola', data.badge],
        ['In servizio', data.active_duty ? 'Sì' : 'No'],
      ]} />
      {data.shifts && data.shifts.length > 0 && (
        <table className="table">
          <thead><tr><th>Data</th><th>Ore</th><th>Zona</th></tr></thead>
          <tbody>{data.shifts.map((s, i) => (
            <tr key={i}>
              <td data-label="Data">{date(s.date)}</td>
              <td data-label="Ore">{s.hours}</td>
              <td data-label="Zona">{s.zone}</td>
            </tr>
          ))}</tbody>
        </table>
      )}
    </div>
  );
}

function labelStatus(s) {
  return { open: 'Aperto', in_istruttoria: 'In istruttoria', closed: 'Chiuso' }[s] || s;
}

export const widgets = {
  'justice.casellario': Casellario,
  'justice.dossiers': Dossiers,
  'justice.dossiers.agent': Dossiers,
  'justice.wanted': Wanted,
  'justice.service_sheet': ServiceSheet,
};
