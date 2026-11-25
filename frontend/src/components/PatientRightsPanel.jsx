import { useEffect, useState } from "react";
import { Check, ClipboardCheck, ThumbsDown, ThumbsUp } from "lucide-react";
import { api } from "../api.js";
import { displayDate, satisfactionLabel } from "../utils/format.js";
import { EmptyState, LoadingState, ScorePill, StatusBadge, SuccessBanner } from "./Common.jsx";

const STATUS_TABS = [
  { id: "ALL", label: "Tümü" },
  { id: "PENDING", label: "Bekleyenler" },
  { id: "REVISION_REQUIRED", label: "Revizyon" },
  { id: "APPROVED", label: "Onaylananlar" },
  { id: "REJECTED", label: "Reddedilenler" },
];

export function PatientRightsPanel({ setError }) {
  const [items, setItems] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedId, setSelectedId] = useState(null);
  const [decision, setDecision] = useState("APPROVED");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  async function refresh() {
    setBusy(true);
    setError("");
    try {
      const url =
        statusFilter === "ALL"
          ? "/api/patient-rights/evaluations/queue"
          : `/api/patient-rights/evaluations/queue?status=${statusFilter}`;
      const list = await api(url);
      setItems(list);
      if (selectedId && !list.some((i) => i.submissionId === selectedId)) {
        setSelectedId(list[0]?.submissionId ?? null);
      } else if (!selectedId && list[0]) {
        setSelectedId(list[0].submissionId);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    refresh();
  }, [statusFilter]);

  const selected = items.find((item) => item.submissionId === selectedId) ?? null;

  useEffect(() => {
    if (selected) {
      setDecision(selected.evaluationStatus === "PENDING" ? "APPROVED" : selected.evaluationStatus);
      setNotes(selected.evaluationNotes ?? "");
    }
  }, [selected?.submissionId]);

  async function handleEvaluate(event) {
    event.preventDefault();
    if (!selected) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api("/api/patient-rights/evaluations", {
        method: "POST",
        body: { submissionId: selected.submissionId, status: decision, notes },
      });
      setNotice("Hasta Hakları değerlendirmesi kaydedildi.");
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const satisfiedCount = items.filter((i) => i.patientSatisfaction === "SATISFIED").length;
  const notSatisfiedCount = items.filter((i) => i.patientSatisfaction === "NOT_SATISFIED").length;

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">HASTA HAKLARI ONAY MERKEZİ</p>
          <h1>Geri bildirim onayları</h1>
          <p className="muted">Personel yanıt taslaklarını inceleyin, onaylayın, revize ettirin veya hasta memnuniyetini takip edin</p>
        </div>
        <button type="button" className="button button-outline" onClick={refresh} disabled={busy}>
          Yenile
        </button>
      </div>

      <div className="rights-summary-strip">
        <div className="subtab-row" role="tablist" aria-label="Durum filtresi">
          {STATUS_TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={statusFilter === t.id}
              className={statusFilter === t.id ? "subtab active" : "subtab"}
              onClick={() => setStatusFilter(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>
        <div className="satisfaction-counters">
          <span className="pill-good">
            <ThumbsUp size={13} /> Memnun: {satisfiedCount}
          </span>
          <span className="pill-critical">
            <ThumbsDown size={13} /> Memnun değil: {notSatisfiedCount}
          </span>
        </div>
      </div>

      <SuccessBanner message={notice} />

      <div className="card data-layout">
        <div className="submission-list">
          {busy && items.length === 0 ? (
            <LoadingState label="Onay kuyruğu yükleniyor…" />
          ) : items.length === 0 ? (
            <EmptyState title="Kuyrukta kayıt yok" text="Seçilen durumda inceleme bekleyen personel taslağı bulunmuyor." />
          ) : (
            items.map((item) => (
              <button
                type="button"
                key={item.submissionId}
                className={selectedId === item.submissionId ? "submission-row active" : "submission-row"}
                onClick={() => {
                  setSelectedId(item.submissionId);
                  setNotice("");
                }}
              >
                <ScorePill score={item.overallScore} />
                <span className="submission-meta">
                  <strong>{item.departmentName}</strong>
                  <small>
                    #{item.submissionId} · {item.feedbackAuthor} · {displayDate(item.feedbackCreatedAt)}
                  </small>
                </span>
                <StatusBadge status={item.evaluationStatus} />
              </button>
            ))
          )}
        </div>

        <div className="detail-panel">
          {!selected ? (
            <EmptyState
              title="Bir taslak seçin"
              text="Personel yanıt taslağını incelemek ve karar vermek için listeden bir kayıt açın."
            />
          ) : (
            <>
              <div className="detail-head">
                <div>
                  <p className="eyebrow">GÖNDERİM #{selected.submissionId}</p>
                  <h2>{selected.departmentName}</h2>
                  <p className="muted">Anket tarihi: {displayDate(selected.submissionCreatedAt)}</p>
                </div>
                <div className="detail-badges">
                  <ScorePill score={selected.overallScore} suffix=" / 5" />
                  <StatusBadge status={selected.evaluationStatus} />
                </div>
              </div>

              <div className="draft-preview">
                <div className="draft-preview-head">
                  <strong>Personel Yanıt Taslağı</strong>
                  <small>
                    {selected.feedbackAuthor} · {displayDate(selected.feedbackCreatedAt)}
                  </small>
                </div>
                <p>{selected.feedbackComment}</p>
              </div>

              <div className="satisfaction-pill-row">
                <span>Hasta değerlendirmesi:</span>
                <strong>{satisfactionLabel(selected.patientSatisfaction)}</strong>
                {selected.patientSatisfactionCreatedAt && (
                  <small className="muted">({displayDate(selected.patientSatisfactionCreatedAt)})</small>
                )}
              </div>

              <form className="workflow-form" onSubmit={handleEvaluate}>
                <label htmlFor="rights-decision">Karar</label>
                <select id="rights-decision" value={decision} onChange={(e) => setDecision(e.target.value)}>
                  <option value="APPROVED">Onayla (Hastaya yayımla)</option>
                  <option value="REVISION_REQUIRED">Revizyon iste</option>
                  <option value="REJECTED">Reddet</option>
                </select>

                <label htmlFor="rights-notes">Değerlendirme notu</label>
                <textarea
                  id="rights-notes"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  maxLength={2000}
                  placeholder="Gerekçe veya revizyon talebi (isteğe bağlı)"
                />

                <button className="button button-primary" disabled={busy}>
                  <ClipboardCheck size={16} /> Kararı kaydet
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
