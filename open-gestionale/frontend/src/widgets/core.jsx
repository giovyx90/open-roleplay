import { api } from '../api/client.js';
import { Empty, KV, HeroValue } from '../components/ui.jsx';
import { date, dateTime, duration } from '../components/format.js';

function Profile({ data }) {
  if (!data) return <Empty />;
  return (
    <KV rows={[
      ['Nome', data.display_name],
      ['Grado', data.rank_label || data.rank],
      ['Corpo', data.corps_label || data.corps],
      ['Registrato', date(data.registered_at)],
      ['Ultimo accesso', dateTime(data.last_seen)],
    ]} />
  );
}

function WeeklyTime({ data, label, variant }) {
  const minutes = (data && data.minutes) || 0;
  if (variant === 'hero') return <HeroValue label={label} value={duration(minutes)} sub="ultimi 7 giorni" />;
  return <KV rows={[['Tempo settimanale', duration(minutes)], ['Ore', data ? data.hours : 0]]} />;
}

function Notifications({ data, reload }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna notifica.</Empty>;
  async function markRead(id) {
    try { await api.markRead(id); reload(); } catch (_) { /* keep silent in UI */ }
  }
  return (
    <ul className="feed">
      {items.map((n) => (
        <li key={n.id} className={`feed__item ${n.read ? 'is-read' : ''}`}>
          <div className="feed__main">
            <span className={`dot dot--${n.kind || 'info'}`} aria-hidden="true" />
            <div>
              <p className="feed__title">{n.title}</p>
              <p className="feed__body">{n.body}</p>
              <time className="feed__time">{dateTime(n.ts)}</time>
            </div>
          </div>
          {!n.read && (
            <button className="link-btn" type="button" onClick={() => markRead(n.id)}>Segna letta</button>
          )}
        </li>
      ))}
    </ul>
  );
}

function News({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessuna notizia.</Empty>;
  return (
    <ul className="news">
      {items.map((n) => (
        <li key={n.id} className="news__item">
          <p className="news__title">{n.pinned && <span className="pin" title="In evidenza">📌</span>} {n.title}</p>
          <p className="news__body">{n.body}</p>
          <p className="news__meta">{n.author} · {date(n.published_at)}</p>
        </li>
      ))}
    </ul>
  );
}

export const widgets = {
  'player.profile': Profile,
  'player.weekly_time': WeeklyTime,
  'notifications.feed': Notifications,
  'server.news': News,
};
