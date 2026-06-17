/*
 * Thin API client. Knows the envelope shape { data, meta } / { error } and
 * nothing about widgets — exactly like the rest of the frontend. The token is
 * kept in memory and mirrored to localStorage so a refresh keeps the session.
 */

const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:3000').replace(/\/$/, '');
const TOKEN_KEY = 'og-token';

let token = null;
try { token = localStorage.getItem(TOKEN_KEY); } catch (_) { /* private mode */ }

export class ApiError extends Error {
  constructor(code, message, status) {
    super(message || code);
    this.code = code;
    this.status = status;
  }
}

export function getToken() { return token; }
export function setToken(value) {
  token = value || null;
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch (_) { /* ignore */ }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['content-type'] = 'application/json';
  if (auth && token) headers['authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    throw new ApiError('NETWORK', 'Impossibile contattare il server.', 0);
  }

  let json = null;
  try { json = await res.json(); } catch (_) { /* empty body */ }

  if (!res.ok) {
    const e = (json && json.error) || {};
    throw new ApiError(e.code || 'HTTP_' + res.status, e.message || res.statusText, res.status);
  }
  return json ? json.data : null;
}

export const api = {
  base: BASE,
  serverConfig: () => request('/config/server', { auth: false }),
  demoProfiles: () => request('/demo/profiles', { auth: false }),
  demoLogin: (uuid) => request('/auth/demo', { method: 'POST', body: { uuid }, auth: false }),
  login: (uuid, otp) => request('/auth/login', { method: 'POST', body: { uuid, otp }, auth: false }),
  logout: () => request('/auth/logout', { method: 'POST', auth: false }).catch(() => {}),
  me: () => request('/player/me'),
  layout: () => request('/config/layout'),
  widget: (id, params) => request(`/widget/${id}${query(params)}`),
  markRead: (id) => request(`/widget/notifications.feed/${encodeURIComponent(id)}/read`, { method: 'POST' }),
  transfer: (payload) => request('/widget/economy.transfers', { method: 'POST', body: payload }),
};

function query(params) {
  if (!params) return '';
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') usp.append(k, v);
  });
  const s = usp.toString();
  return s ? `?${s}` : '';
}
