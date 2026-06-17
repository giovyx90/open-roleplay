import { useState } from 'react';
import { api } from '../api/client.js';
import { Empty, Tag, HeroValue } from '../components/ui.jsx';
import { money, dateTime } from '../components/format.js';

function Balance({ data, label, variant }) {
  if (!data) return <Empty />;
  const total = (Number(data.bank) || 0) + (Number(data.cash) || 0);
  if (variant === 'hero') {
    return <HeroValue label={label} value={money(total, data.currency)} sub={`Banca ${money(data.bank, data.currency)}`} />;
  }
  return (
    <div className="balance">
      <div className="balance__total">{money(total, data.currency)}</div>
      <ul className="balance__accounts">
        {(data.accounts || []).map((a) => (
          <li key={a.id}><span>{a.label}</span><strong>{money(a.amount, data.currency)}</strong></li>
        ))}
      </ul>
    </div>
  );
}

function Transactions({ data }) {
  const [page, setPage] = useState((data && data.page) || 1);
  const [state, setState] = useState(data || { items: [], pages: 1 });
  const [busy, setBusy] = useState(false);

  async function go(p) {
    setBusy(true);
    try { setState(await api.widget('economy.transactions', { page: p, limit: state.limit || 20 })); setPage(p); }
    finally { setBusy(false); }
  }

  const items = state.items || [];
  if (!items.length) return <Empty>Nessuna transazione.</Empty>;
  return (
    <div className={busy ? 'is-busy' : ''}>
      <table className="table">
        <thead><tr><th>Data</th><th>Movimento</th><th className="num">Importo</th></tr></thead>
        <tbody>
          {items.map((t) => (
            <tr key={t.id}>
              <td data-label="Data">{dateTime(t.ts)}</td>
              <td data-label="Movimento"><strong>{t.counterparty}</strong><br /><span className="muted">{t.description}</span></td>
              <td data-label="Importo" className={`num ${t.amount < 0 ? 'neg' : 'pos'}`}>{money(t.amount, t.currency)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {state.pages > 1 && (
        <div className="pager">
          <button disabled={page <= 1 || busy} onClick={() => go(page - 1)} type="button">‹</button>
          <span>{page} / {state.pages}</span>
          <button disabled={page >= state.pages || busy} onClick={() => go(page + 1)} type="button">›</button>
        </div>
      )}
    </div>
  );
}

function Cards({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna carta.</Empty>;
  return (
    <div className="cards-grid">
      {items.map((c) => (
        <div key={c.id} className={`pay-card pay-card--${c.kind}`}>
          <span className="pay-card__kind">{c.kind === 'company' ? 'Aziendale' : 'Personale'}</span>
          <span className="pay-card__num">{c.number_masked}</span>
          <span className="pay-card__holder">{c.company || c.holder}</span>
          <Tag tone={c.status === 'active' ? 'green' : 'grey'}>{c.status}</Tag>
        </div>
      ))}
    </div>
  );
}

function Transfers({ data, reload }) {
  const [to, setTo] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setMsg(null);
    try {
      await api.transfer({ to, amount: Number(amount), note });
      setMsg({ ok: true, text: 'Bonifico eseguito (demo).' });
      setTo(''); setAmount(''); setNote('');
      reload();
    } catch (err) {
      setMsg({ ok: false, text: err.message || 'Bonifico non riuscito.' });
    } finally { setBusy(false); }
  }

  return (
    <form className="form" onSubmit={submit}>
      <p className="muted">Unica operazione di scrittura. Nella demo è simulata; in produzione richiede un adapter economy abilitato alla scrittura.</p>
      <label>Destinatario<input value={to} onChange={(e) => setTo(e.target.value)} required placeholder="Nome o IBAN RP" /></label>
      <label>Importo<input value={amount} onChange={(e) => setAmount(e.target.value)} required type="number" min="0.01" step="0.01" /></label>
      <label>Causale<input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Opzionale" /></label>
      <button className="btn" disabled={busy} type="submit">{busy ? '…' : 'Invia bonifico'}</button>
      {msg && <p className={msg.ok ? 'ok-text' : 'error-text'}>{msg.text}</p>}
    </form>
  );
}

export const widgets = {
  'economy.balance': Balance,
  'economy.transactions': Transactions,
  'economy.cards': Cards,
  'economy.transfers': Transfers,
};
