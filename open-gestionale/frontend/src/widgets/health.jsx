import { Empty, Tag, KV, statusTone } from '../components/ui.jsx';
import { date, dateTime } from '../components/format.js';

function Record({ data }) {
  if (!data) return <Empty>Cartella clinica non disponibile.</Empty>;
  return (
    <KV rows={[
      ['Gruppo sanguigno', data.blood_type],
      ['Allergie', (data.allergies || []).join(', ') || 'Nessuna'],
      ['Note', data.notes || '—'],
      ['Aggiornata', date(data.updated_at)],
    ]} />
  );
}

function Appointments({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun appuntamento.</Empty>;
  return (
    <ul className="appts">
      {items.map((a) => (
        <li key={a.id} className="appts__item">
          <div><strong>{a.dept}</strong><br /><span className="muted">{a.doctor}</span></div>
          <div className="appts__right"><time>{dateTime(a.when)}</time><Tag tone={statusTone(a.status)}>{a.status}</Tag></div>
        </li>
      ))}
    </ul>
  );
}

export const widgets = {
  'health.record': Record,
  'health.appointments': Appointments,
};
