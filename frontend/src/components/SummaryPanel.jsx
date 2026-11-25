import { useEffect, useState } from "react";
import { RefreshCw, ShieldCheck, Sparkles } from "lucide-react";
import { api } from "../api.js";
import { daysAgoIsoDate, displayDate, displayShortDate, todayIsoDate } from "../utils/format.js";
import { EmptyState, StatusBadge, SuccessBanner } from "./Common.jsx";

export function SummaryPanel({ setError }) {
  const [departments, setDepartments] = useState([]);
  const [departmentId, setDepartmentId] = useState("");
  const [dateFrom, setDateFrom] = useState(daysAgoIsoDate(29));
  const [dateTo, setDateTo] = useState(todayIsoDate());
  const [lowScoreOnly, setLowScoreOnly] = useState(false);
  const [summaries, setSummaries] = useState([]);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  async function refreshDepartments() {
    try {
      const list = await api("/api/departments/accessible");
      setDepartments(list);
      if (!departmentId && list[0]) setDepartmentId(String(list[0].id));
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function loadSummaries() {
    if (!departmentId) return;
    try {
      setSummaries(await api(`/api/ai-summaries?departmentId=${departmentId}`));
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  useEffect(() => {
    refreshDepartments();
  }, []);

  useEffect(() => {
    loadSummaries();
  }, [departmentId]);

  async function createSummary(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api("/api/ai-summaries", {
        method: "POST",
        body: { departmentId: Number(departmentId), dateFrom, dateTo, lowScoreOnly },
      });
      setNotice("Yeni AI özeti oluşturuldu ve kaydedildi.");
      await loadSummaries();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function retrySummary(id) {
    setBusy(true);
    setError("");
    try {
      await api(`/api/ai-summaries/${id}/retry`, { method: "POST" });
      await loadSummaries();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">YAPAY ZEKÂ DESTEKLİ YORUM ÖZETLEME</p>
          <h1>AI Özetleri</h1>
          <p className="muted">
            Anonim yorumlardan ve metrik ortalamalarından tekrar eden sorunları ve güçlü hizmet alanlarını özetleyin
          </p>
        </div>
      </div>

      <form className="card filter-bar" onSubmit={createSummary}>
        <div className="filter-field">
          <label htmlFor="summary-department">Birim</label>
          <select
            id="summary-department"
            value={departmentId}
            onChange={(event) => setDepartmentId(event.target.value)}
            required
          >
            <option value="">Birim seçin</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="date-from">Başlangıç</label>
          <input
            id="date-from"
            type="date"
            value={dateFrom}
            onChange={(event) => setDateFrom(event.target.value)}
            required
          />
        </div>
        <div className="filter-field">
          <label htmlFor="date-to">Bitiş</label>
          <input
            id="date-to"
            type="date"
            value={dateTo}
            onChange={(event) => setDateTo(event.target.value)}
            required
          />
        </div>
        <label className="toggle-control">
          <input
            type="checkbox"
            checked={lowScoreOnly}
            onChange={(event) => setLowScoreOnly(event.target.checked)}
          />
          Yalnızca ≤ 3 puan
        </label>
        <button className="button button-primary" disabled={busy || !departmentId}>
          <Sparkles size={16} />
          {busy ? "Özet hazırlanıyor…" : "Özet oluştur"}
        </button>
      </form>

      <SuccessBanner message={notice} />

      <div className="summary-list">
        {summaries.length === 0 ? (
          <div className="card">
            <EmptyState
              title="Henüz AI özeti yok"
              text="Birim ve tarih aralığı seçerek tekrarlayan temaları özetleyen yeni bir kayıt oluşturun."
            />
          </div>
        ) : (
          summaries.map((summary) => (
            <article className="card summary-item" key={summary.id}>
              <div className="summary-item-head">
                <StatusBadge status={summary.status} />
                <strong>{summary.departmentName ?? `Birim #${summary.departmentId}`}</strong>
                <span>
                  {displayShortDate(summary.dateFrom)} – {displayShortDate(summary.dateTo)}
                </span>
                <span>{summary.submissionCount} gönderim</span>
                {summary.lowScoreOnly && <span className="pill-warning">Yalnızca ≤ 3 puan</span>}
                {summary.surveyVersionScope && <span className="version-tag">{summary.surveyVersionScope}</span>}
              </div>

              <p className="summary-text">{summary.summaryText}</p>

              {summary.errorMessage && <p className="error-inline">{summary.errorMessage}</p>}

              <div className="summary-item-foot">
                <small className="muted">
                  Oluşturan: {summary.createdByName ?? "Yetkili"} · {displayDate(summary.createdAt)} · Model:{" "}
                  {summary.modelName}
                </small>
                {summary.status === "FAILED" && (
                  <button
                    type="button"
                    className="button button-outline compact"
                    disabled={busy}
                    onClick={() => retrySummary(summary.id)}
                  >
                    <RefreshCw size={14} /> Yeniden dene
                  </button>
                )}
              </div>
            </article>
          ))
        )}
      </div>

      <p className="privacy-note">
        <ShieldCheck size={15} />
        AI servisine hasta takip numarası, cookie, kullanıcı adı veya personel kimliği gönderilmez; serbest metindeki
        olası kişisel bilgiler (TCKN, telefon, e-posta, IBAN) maskelenir. AI çıktısı bildirim veya otomatik yetki
        üretmez.
      </p>
    </div>
  );
}
