import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, setToken, getToken } from '../api/client.js';

/*
 * Holds the session. On boot it loads the public server config (to theme the
 * login screen and decide demo vs real). Once a token exists it loads /player/me
 * and the resolved /config/layout. Promotions in-game are picked up on refresh
 * because the token carries only the uuid.
 */

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [server, setServer] = useState(null);
  const [player, setPlayer] = useState(null);
  const [layout, setLayout] = useState(null);
  const [status, setStatus] = useState('boot'); // boot | anon | loading | auth | error
  const [error, setError] = useState(null);

  const loadSession = useCallback(async () => {
    setStatus('loading');
    try {
      const [me, lay] = await Promise.all([api.me(), api.layout()]);
      setPlayer(me);
      setLayout(lay);
      setStatus('auth');
    } catch (err) {
      // Token invalid/expired: drop it and fall back to the login screen.
      setToken(null);
      setPlayer(null);
      setLayout(null);
      setStatus('anon');
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const cfg = await api.serverConfig();
        if (cancelled) return;
        setServer(cfg);
        applyTheme(cfg.theme_color);
        if (getToken()) await loadSession();
        else setStatus('anon');
      } catch (err) {
        if (!cancelled) { setError(err); setStatus('error'); }
      }
    })();
    return () => { cancelled = true; };
  }, [loadSession]);

  const finishLogin = useCallback(async (result) => {
    setToken(result.token);
    await loadSession();
  }, [loadSession]);

  const demoLogin = useCallback(async (uuid) => finishLogin(await api.demoLogin(uuid)), [finishLogin]);
  const login = useCallback(async (uuid, otp) => finishLogin(await api.login(uuid, otp)), [finishLogin]);

  const logout = useCallback(async () => {
    await api.logout();
    setToken(null);
    setPlayer(null);
    setLayout(null);
    setStatus('anon');
  }, []);

  const value = { server, player, layout, status, error, login, demoLogin, logout };
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth fuori da AuthProvider');
  return ctx;
}

function applyTheme(color) {
  if (color) document.documentElement.style.setProperty('--accent', color);
}
