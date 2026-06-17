export default function Splash({ label, detail, tone }) {
  return (
    <div className={`splash ${tone === 'error' ? 'splash--error' : ''}`}>
      <div className="splash__mark" aria-hidden="true" />
      <p className="splash__label">{label}</p>
      {detail && <p className="splash__detail">{detail}</p>}
    </div>
  );
}
