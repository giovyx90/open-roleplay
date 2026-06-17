import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';

/*
 * Real login. The gestionale has no password: the player asks the in-game
 * plugin for a one-time code and types it here together with their UUID.
 */
export default function Login() {
  const { server, login } = useAuth();
  const [uuid, setUuid] = useState('');
  const [otp, setOtp] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try { await login(uuid.trim(), otp.trim()); }
    catch (e2) { setErr(e2.message || 'Accesso non riuscito.'); setBusy(false); }
  }

  return (
    <div className="auth">
      <div className="auth__card">
        <h1>{server ? server.name : 'Gestionale'}</h1>
        <p className="muted">Accedi con il tuo account Minecraft.</p>
        <ol className="auth__steps">
          <li>Entra nel server e digita <code>/gestionale login</code></li>
          <li>Copia il codice a 6 cifre mostrato in gioco</li>
          <li>Inseriscilo qui sotto insieme al tuo UUID</li>
        </ol>
        <form className="form" onSubmit={submit}>
          <label>UUID Minecraft<input value={uuid} onChange={(e) => setUuid(e.target.value)} required placeholder="xxxxxxxx-xxxx-..." /></label>
          <label>Codice (OTP)<input value={otp} onChange={(e) => setOtp(e.target.value)} required inputMode="numeric" placeholder="000000" maxLength={6} /></label>
          <button className="btn" type="submit" disabled={busy}>{busy ? 'Accesso…' : 'Accedi'}</button>
          {err && <p className="error-text">{err}</p>}
        </form>
      </div>
    </div>
  );
}
