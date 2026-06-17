import { useState } from 'react';
import { api } from '../api/client.js';
import { Empty, Tag, KV, statusTone } from '../components/ui.jsx';
import { money, date } from '../components/format.js';

const ROLE_LABEL = { owner: 'Titolare', member: 'Membro' };

function CompanyDetail({ data }) {
  if (!data) return <Empty />;
  return (
    <div className="company-detail">
      <KV rows={[
        ['Settore', data.sector],
        ['Sede', data.hq],
        ['Fondata', date(data.founded_at)],
        ['Tesoreria', data.balance != null ? money(data.balance) : null],
        ['Licenze', (data.licenses || []).join(', ')],
      ]} />
      {data.assets && data.assets.length > 0 && (
        <>
          <h4>Asset</h4>
          <ul className="chips">{data.assets.map((a, i) => <li key={i} className="chip">{a.label}</li>)}</ul>
        </>
      )}
      {data.members && data.members.length > 0 && (
        <>
          <h4>Organico</h4>
          <ul className="roster">
            {data.members.map((m, i) => (
              <li key={i}><span>{m.name}</span><Tag>{ROLE_LABEL[m.role] || m.role}</Tag></li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

function CompanyMine({ data }) {
  const items = (data && data.items) || [];
  const [open, setOpen] = useState(null);
  const [detail, setDetail] = useState({});

  async function toggle(id) {
    if (open === id) { setOpen(null); return; }
    setOpen(id);
    if (!detail[id]) {
      try { const d = await api.widget('company.detail', { id }); setDetail((m) => ({ ...m, [id]: d })); }
      catch (_) { /* leave undefined */ }
    }
  }

  if (!items.length) return <Empty>Non fai parte di nessuna azienda.</Empty>;
  return (
    <ul className="acc">
      {items.map((c) => (
        <li key={c.id} className="acc__item">
          <button className="acc__head" type="button" onClick={() => toggle(c.id)} aria-expanded={open === c.id}>
            <span><strong>{c.name}</strong> <span className="muted">· {c.sector}</span></span>
            <span className="acc__right">
              <Tag>{ROLE_LABEL[c.role] || c.role}</Tag>
              <Tag tone={statusTone(c.status)}>{c.status}</Tag>
            </span>
          </button>
          {open === c.id && <div className="acc__body">{detail[c.id]
            ? <CompanyDetail data={detail[c.id]} /> : <p className="muted">Caricamento…</p>}</div>}
        </li>
      ))}
    </ul>
  );
}

function CompanyRegistry({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna azienda registrata.</Empty>;
  return (
    <table className="table">
      <thead><tr><th>Azienda</th><th>Settore</th><th>Sede</th><th>Stato</th></tr></thead>
      <tbody>
        {items.map((c) => (
          <tr key={c.id}>
            <td><strong>{c.name}</strong></td><td>{c.sector}</td><td>{c.hq || '—'}</td>
            <td><Tag tone={statusTone(c.status)}>{c.status}</Tag></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const widgets = {
  'company.mine': CompanyMine,
  'company.registry': CompanyRegistry,
  'company.detail': CompanyDetail,
};
