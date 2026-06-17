import { Empty } from '../components/ui.jsx';

function Degrees({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun titolo di studio.</Empty>;
  return (
    <ul className="timeline">
      {items.map((d) => (
        <li key={d.id}><strong>{d.title}</strong><span className="muted"> · {d.institution}</span>
          {d.year && <time className="muted"> ({d.year})</time>}</li>
      ))}
    </ul>
  );
}

function Enrollments({ data }) {
  const items = (data && data.items) || [];
  if (!items.length) return <Empty>Nessun corso attivo.</Empty>;
  return (
    <ul className="enrolls">
      {items.map((e) => (
        <li key={e.id} className="enrolls__item">
          <div className="enrolls__top"><strong>{e.course}</strong><span className="muted">{e.institution}</span></div>
          <div className="progress"><span style={{ width: `${Math.max(0, Math.min(100, e.progress || 0))}%` }} /></div>
          <span className="muted">{e.progress || 0}%</span>
        </li>
      ))}
    </ul>
  );
}

export const widgets = {
  'education.degrees': Degrees,
  'education.enrollments': Enrollments,
};
