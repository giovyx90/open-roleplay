import { Empty, Tag, HeroValue, statusTone } from '../components/ui.jsx';
import { date } from '../components/format.js';

function Phone({ data, label, variant }) {
  if (!data) {
    if (variant === 'hero') return <HeroValue label={label} value="—" />;
    return <Empty>Nessun numero registrato.</Empty>;
  }
  if (variant === 'hero') return <HeroValue label={label} value={data.number} sub={data.carrier} />;
  return <p className="phone">{data.number}<br /><span className="muted">{data.carrier} · dal {date(data.since)}</span></p>;
}

function Documents({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun documento.</Empty>;
  return (
    <table className="table">
      <thead><tr><th>Documento</th><th>Numero</th><th>Scadenza</th><th>Stato</th></tr></thead>
      <tbody>
        {items.map((d) => (
          <tr key={d.id}>
            <td data-label="Documento">{d.type}</td>
            <td data-label="Numero"><code>{d.number}</code></td>
            <td data-label="Scadenza">{date(d.expires_at)}</td>
            <td data-label="Stato"><Tag tone={statusTone(d.status)}>{d.status}</Tag></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const widgets = {
  'identity.phone': Phone,
  'identity.documents': Documents,
};
