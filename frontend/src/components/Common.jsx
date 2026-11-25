import { useState } from "react";
import { Check, Copy, FileText, Info, TriangleAlert } from "lucide-react";
import { displayShortDate, scoreTone, statusLabel } from "../utils/format.js";

export function CopyValue({ value }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2200);
  }

  return (
    <div className="copy-field">
      <code>{value}</code>
      <button type="button" className="icon-button" onClick={copy} title="Kopyala" aria-label="Kopyala">
        {copied ? <Check size={16} /> : <Copy size={16} />}
      </button>
    </div>
  );
}

export function LoadingState({ label = "Yükleniyor…" }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <span className="loader" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ title, text, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <FileText size={20} />
      </span>
      <strong>{title}</strong>
      <p>{text}</p>
      {action}
    </div>
  );
}

export function ErrorBanner({ message }) {
  if (!message) return null;
  return (
    <div className="banner banner-error" role="alert">
      <TriangleAlert size={16} />
      <span>{message}</span>
    </div>
  );
}

export function SuccessBanner({ message }) {
  if (!message) return null;
  return (
    <div className="banner banner-success" role="status">
      <Check size={16} />
      <span>{message}</span>
    </div>
  );
}

export function InfoBanner({ message }) {
  if (!message) return null;
  return (
    <div className="banner banner-info">
      <Info size={16} />
      <span>{message}</span>
    </div>
  );
}

export function StatusBadge({ status }) {
  const normalized = (status ?? "PENDING").toLowerCase();
  return <span className={`badge badge-${normalized}`}>{statusLabel(status)}</span>;
}

export function ScorePill({ score, suffix = "" }) {
  const tone = scoreTone(score);
  return (
    <span className={`score-pill score-${tone}`}>
      {Number(score ?? 0).toFixed(2)}
      {suffix}
    </span>
  );
}

export function StatCard({ label, value, hint, icon: Icon, tone = "primary" }) {
  return (
    <article className={`kpi-card kpi-${tone}`}>
      <div className="kpi-head">
        <span>{label}</span>
        {Icon && (
          <span className="kpi-icon">
            <Icon size={17} />
          </span>
        )}
      </div>
      <strong className="kpi-value">{value}</strong>
      {hint && <small className="kpi-hint">{hint}</small>}
    </article>
  );
}

export function SimpleTrendChart({ points }) {
  if (!points || points.length === 0) {
    return <p className="muted">Seçilen tarih aralığında trend verisi bulunmuyor.</p>;
  }

  const width = 560;
  const height = 190;
  const padX = 36;
  const padY = 20;
  const plotW = width - padX * 2;
  const plotH = height - padY * 2;

  const coords = points.map((point, index) => {
    const x = points.length === 1 ? padX + plotW / 2 : padX + (index / (points.length - 1)) * plotW;
    const score = Math.max(0, Math.min(5, Number(point.averageScore ?? 0)));
    const y = padY + plotH - (score / 5) * plotH;
    return { ...point, x, y, score };
  });

  const polyline = coords.map((c) => `${c.x},${c.y}`).join(" ");
  const area = `${coords[0].x},${padY + plotH} ${polyline} ${coords[coords.length - 1].x},${padY + plotH}`;

  return (
    <div className="trend-chart-wrap">
      <svg viewBox={`0 0 ${width} ${height}`} className="trend-svg" role="img" aria-label="Zaman içindeki memnuniyet puanı grafiği">
        {[1, 2, 3, 4, 5].map((tick) => {
          const y = padY + plotH - (tick / 5) * plotH;
          return (
            <g key={tick}>
              <line x1={padX} y1={y} x2={width - padX} y2={y} className="trend-grid" />
              <text x={padX - 10} y={y + 4} textAnchor="end" className="trend-axis-label">
                {tick}
              </text>
            </g>
          );
        })}
        {coords.length > 1 && <polygon points={area} className="trend-area" />}
        {coords.length > 1 && <polyline points={polyline} className="trend-line" />}
        {coords.map((c) => (
          <g key={c.date}>
            <circle cx={c.x} cy={c.y} r="4.5" className="trend-dot">
              <title>
                {displayShortDate(c.date)}: {c.score.toFixed(2)} / 5 ({c.submissionCount} anket)
              </title>
            </circle>
          </g>
        ))}
      </svg>
      <div className="trend-footer">
        <span>{displayShortDate(points[0].date)}</span>
        <span>{points.length} gün verisi</span>
        <span>{displayShortDate(points[points.length - 1].date)}</span>
      </div>
    </div>
  );
}
