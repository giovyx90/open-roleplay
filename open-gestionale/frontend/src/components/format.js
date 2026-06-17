/* Formatting helpers. Tolerant of both ISO strings and epoch numbers, because
   the demo source emits ISO and a real DB may emit epochs. */

export function money(amount, currency = 'EUR') {
  if (amount == null || isNaN(Number(amount))) return '—';
  try {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency }).format(Number(amount));
  } catch (_) {
    return `${Number(amount).toFixed(2)} ${currency}`;
  }
}

export function date(value) {
  const d = toDate(value);
  if (!d) return '—';
  return d.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function dateTime(value) {
  const d = toDate(value);
  if (!d) return '—';
  return d.toLocaleString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function duration(minutes) {
  const m = Number(minutes) || 0;
  const h = Math.floor(m / 60);
  const r = m % 60;
  return h > 0 ? `${h}h ${r}m` : `${r}m`;
}

function toDate(value) {
  if (value == null) return null;
  const d = typeof value === 'number' ? new Date(value) : new Date(String(value));
  return isNaN(d.getTime()) ? null : d;
}
