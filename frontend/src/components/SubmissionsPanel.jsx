import { useEffect, useMemo, useState } from "react";
import { Check, ChevronLeft, ChevronRight, MessageSquare, Search, Send } from "lucide-react";
import { api } from "../api.js";
import { displayDate, satisfactionLabel } from "../utils/format.js";
import { EmptyState, InfoBanner, LoadingState, ScorePill, StatusBadge, SuccessBanner } from "./Common.jsx";

const RESPONSE_TEMPLATES = [
  "Geri bildiriminiz için teşekkür ederiz. Belirttiğiniz konu ilgili birim yöneticilerimize aktarılmıştır.",
  "Yaşadığınız bekleme süresi nedeniyle üzgünüz. Süreçlerimizin hızlandırılması için gerekli planlama yapılmıştır.",
  "Hizmet kalitemize yönelik olumlu değerlendirmeniz için teşekkür eder, sağlıklı günler dileriz.",
];

const PAGE_SIZE = 10;

export function SubmissionsPanel({ session, setError }) {
  const [submissions, setSubmissions] = useState([]);
  const [selected, setSelected] = useState(null);
  const [comment, setComment] = useState("");
  const [status, setStatus] = useState("APPROVED");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [search, setSearch] = useState("");
  const [scoreFilter, setScoreFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);

  async function refresh() {
    setBusy(true);
    setError("");
    try {
      const list = await api("/api/staff/submissions");
      setSubmissions(list);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const filtered = useMemo(() => {
    return submissions.filter((item) => {
      if (search.trim()) {
        const q = search.trim().toLocaleLowerCase("tr-TR");
        const dept = (item.departmentName ?? "").toLocaleLowerCase("tr-TR");
        const idText = String(item.submissionId);
        if (!dept.includes(q) && !idText.includes(q)) return false;
      }
      const score = Number(item.overallScore ?? 0);
      if (scoreFilter === "LOW" && score > 3) return false;
      if (scoreFilter === "CRITICAL" && score > 2) return false;
      if (statusFilter !== "ALL") {
        const eff = item.evaluationStatus ?? "PENDING";
        if (eff !== statusFilter) return false;
      }
      return true;
    });
  }, [submissions, search, scoreFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pageItems = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  async function selectSubmission(item, keepNotice = false) {
    setSelected(item);
    if (!keepNotice) setNotice("");
    setError("");
    try {
      const detail = await api(`/api/staff/submissions/${item.submissionId}`);
      setSelected(detail);
      setComment(detail.staffFeedbackComment ?? "");
      setStatus(detail.evaluationStatus && detail.evaluationStatus !== "PENDING" ? detail.evaluationStatus : "APPROVED");
      setNotes(detail.evaluationNotes ?? "");
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function submitStaffFeedback(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api("/api/staff/feedback", {
        method: "POST",
        body: { submissionId: selected.submissionId, comment },
      });
      await selectSubmission(selected, true);
      setNotice("Geri bildirim taslağı oluşturuldu.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function submitEvaluation(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api("/api/patient-rights/evaluations", {
        method: "POST",
        body: { submissionId: selected.submissionId, status, notes },
      });
      await Promise.all([refresh(), selectSubmission(selected, true)]);
      setNotice("Hasta Hakları kararı kaydedildi.");
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
          <p className="eyebrow">ANKET SONUÇLARI VE GERİ BİLDİRİM</p>
          <h1>Sonuç inceleme</h1>
          <p className="muted">Yetkili olduğunuz birimlere ait anonim anket gönderimleri ve yanıt taslakları</p>
        </div>
        <button type="button" className="button button-outline" onClick={refresh} disabled={busy}>
          Yenile
        </button>
      </div>

      <div className="card filter-bar compact-filters">
        <div className="filter-field grow">
          <label htmlFor="sub-search">Birim veya Gönderim # ara</label>
          <div className="input-with-icon no-margin">
            <Search size={15} />
            <input
              id="sub-search"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder="Örn. Acil Servis veya #12"
            />
          </div>
        </div>
        <div className="filter-field">
          <label htmlFor="sub-score-filter">Puan</label>
          <select
            id="sub-score-filter"
            value={scoreFilter}
            onChange={(e) => {
              setScoreFilter(e.target.value);
              setPage(1);
            }}
          >
            <option value="ALL">Tüm puanlar</option>
            <option value="LOW">Düşük (≤ 3)</option>
            <option value="CRITICAL">Kritik (≤ 2)</option>
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="sub-status-filter">Onay Durumu</label>
          <select
            id="sub-status-filter"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
          >
            <option value="ALL">Tümü</option>
            <option value="PENDING">İncelemede</option>
            <option value="REVISION_REQUIRED">Revizyon İstendi</option>
            <option value="APPROVED">Onaylandı</option>
            <option value="REJECTED">Reddedildi</option>
          </select>
        </div>
      </div>

      <div className="card data-layout">
        <div className="submission-list">
          {busy && submissions.length === 0 ? (
            <LoadingState label="Kayıtlar yükleniyor…" />
          ) : pageItems.length === 0 ? (
            <EmptyState title="Kayıt bulunamadı" text="Filtrelere uygun anonim anket gönderimi bulunmuyor." />
          ) : (
            <>
              {pageItems.map((item) => (
                <button
                  type="button"
                  key={item.submissionId}
                  className={selected?.submissionId === item.submissionId ? "submission-row active" : "submission-row"}
                  onClick={() => selectSubmission(item)}
                >
                  <ScorePill score={item.overallScore} />
                  <span className="submission-meta">
                    <strong>{item.departmentName ?? "Birim"}</strong>
                    <small>
                      #{item.submissionId} · {displayDate(item.createdAt)}
                    </small>
                  </span>
                  <StatusBadge status={item.evaluationStatus ?? "PENDING"} />
                  <ChevronRight size={15} />
                </button>
              ))}
              {totalPages > 1 && (
                <div className="pagination-bar">
                  <button
                    type="button"
                    className="button button-outline compact"
                    disabled={safePage <= 1}
                    onClick={() => setPage((p) => Math.max(1, p - 1))}
                  >
                    <ChevronLeft size={14} /> Önceki
                  </button>
                  <span className="muted">
                    Sayfa {safePage} / {totalPages} ({filtered.length} kayıt)
                  </span>
                  <button
                    type="button"
                    className="button button-outline compact"
                    disabled={safePage >= totalPages}
                    onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  >
                    Sonraki <ChevronRight size={14} />
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        <div className="detail-panel">
          {!selected ? (
            <EmptyState
              title="Bir gönderim seçin"
              text="Soru puanlarını, yorumları ve geri bildirim onay sürecini görmek için soldan bir kayıt seçin."
            />
          ) : (
            <>
              <div className="detail-head">
                <div>
                  <p className="eyebrow">
                    GÖNDERİM #{selected.submissionId}
                    {selected.surveyVersionNumber ? ` · SÜRÜM v${selected.surveyVersionNumber}` : ""}
                  </p>
                  <h2>{selected.departmentName ?? "Hasta geri bildirimi"}</h2>
                  <p className="muted">{displayDate(selected.createdAt)}</p>
                </div>
                <div className="detail-badges">
                  <ScorePill score={selected.overallScore} suffix=" / 5" />
                  <StatusBadge status={selected.evaluationStatus ?? "PENDING"} />
                </div>
              </div>

              <div className="answer-list">
                {(selected.answers ?? []).map((answer) => (
                  <div key={answer.questionId} className="answer-item">
                    <span>{answer.questionText}</span>
                    <strong>{answer.score != null ? `${answer.score} / 5` : answer.answerValue}</strong>
                  </div>
                ))}
              </div>

              {selected.staffFeedbackComment && (
                <div className="draft-preview">
                  <div className="draft-preview-head">
                    <strong>
                      <MessageSquare size={14} /> Mevcut Yanıt Taslağı
                    </strong>
                    <small>
                      {selected.staffFeedbackAuthor ?? "Personel"} · {displayDate(selected.staffFeedbackCreatedAt)}
                    </small>
                  </div>
                  <p>{selected.staffFeedbackComment}</p>
                </div>
              )}

              {selected.evaluationNotes && (
                <div className="review-note inline-note">
                  <strong>Hasta Hakları Değerlendirme Notu ({selected.evaluatedByName ?? "Yetkili"})</strong>
                  <p>{selected.evaluationNotes}</p>
                </div>
              )}

              {selected.patientSatisfaction && (
                <div className="satisfaction-pill-row">
                  <span>Hasta yanıt değerlendirmesi:</span>
                  <strong className={selected.patientSatisfaction === "SATISFIED" ? "text-good" : "text-critical"}>
                    {satisfactionLabel(selected.patientSatisfaction)}
                  </strong>
                </div>
              )}

              <SuccessBanner message={notice} />

              {(session.role === "STAFF" || session.role === "ADMIN") &&
                selected.evaluationStatus !== "APPROVED" &&
                (selected.staffFeedbackAuthorId &&
                session.userId &&
                selected.staffFeedbackAuthorId !== session.userId &&
                session.role !== "ADMIN" ? (
                  <InfoBanner
                    message={`Bu gönderim için yanıt taslağı ${selected.staffFeedbackAuthor ?? "başka bir personel"} tarafından oluşturuldu. Aynı birimdeki başka bir personelin taslağı yalnızca taslağı oluşturan kişi veya sistem yöneticisi tarafından düzenlenebilir.`}
                  />
                ) : (
                  <form className="workflow-form" onSubmit={submitStaffFeedback}>
                    <label htmlFor="staff-comment">Yanıt taslağı</label>
                    <div className="template-chips">
                      {RESPONSE_TEMPLATES.map((tpl, idx) => (
                        <button
                          key={idx}
                          type="button"
                          className="chip-button"
                          onClick={() => setComment(tpl)}
                        >
                          Hazır şablon {idx + 1}
                        </button>
                      ))}
                    </div>
                    <textarea
                      id="staff-comment"
                      value={comment}
                      onChange={(event) => setComment(event.target.value)}
                      maxLength={2000}
                      required
                      placeholder="Hastaya iletilmesini önerdiğiniz kurumsal yanıt taslağı"
                    />
                    <button className="button button-primary" disabled={busy}>
                      <Send size={15} /> Taslağı kaydet
                    </button>
                  </form>
                ))}

              {(session.role === "PATIENT_RIGHTS_OFFICER" || session.role === "ADMIN") && (
                <form className="workflow-form" onSubmit={submitEvaluation}>
                  <label htmlFor="evaluation-status">Hasta Hakları kararı</label>
                  <select
                    id="evaluation-status"
                    value={status}
                    onChange={(event) => setStatus(event.target.value)}
                  >
                    <option value="APPROVED">Onayla ve hastaya yayımla</option>
                    <option value="REVISION_REQUIRED">Revizyon iste</option>
                    <option value="REJECTED">Reddet</option>
                  </select>
                  <label htmlFor="evaluation-notes">İç değerlendirme notu</label>
                  <textarea
                    id="evaluation-notes"
                    value={notes}
                    onChange={(event) => setNotes(event.target.value)}
                    maxLength={2000}
                    placeholder="Revizyon gerekçesi veya onay notu"
                  />
                  <button className="button button-primary" disabled={busy}>
                    <Check size={15} /> Kararı kaydet
                  </button>
                </form>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
