import { useEffect, useState } from "react";
import { Activity, Check, MessageSquareText, Search, ThumbsDown, ThumbsUp } from "lucide-react";
import { api } from "../api.js";
import { displayDate } from "../utils/format.js";
import { ErrorBanner, StatusBadge, SuccessBanner } from "./Common.jsx";

export function TrackingView({ initialTrackingNumber = "" }) {
  const [trackingNumber, setTrackingNumber] = useState(initialTrackingNumber);
  const [status, setStatus] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [evaluated, setEvaluated] = useState(false);

  useEffect(() => {
    if (initialTrackingNumber) {
      setTrackingNumber(initialTrackingNumber);
      fetchStatus(initialTrackingNumber);
    }
  }, [initialTrackingNumber]);

  async function fetchStatus(code) {
    const trimmed = code.trim();
    if (!trimmed) return;
    setBusy(true);
    setStatus(null);
    setFeedback(null);
    setEvaluated(false);
    setError("");
    try {
      const result = await api(`/api/public/surveys/tracking/${encodeURIComponent(trimmed)}`);
      setStatus(result);
      if (result.status === "APPROVED") {
        const fb = await api(`/api/public/surveys/tracking/${encodeURIComponent(trimmed)}/feedback`);
        setFeedback(fb);
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function submitSatisfaction(satisfied) {
    setBusy(true);
    setError("");
    try {
      await api(`/api/public/surveys/tracking/${encodeURIComponent(trackingNumber.trim())}/feedback/satisfaction`, {
        method: "POST",
        body: { satisfied },
      });
      setEvaluated(true);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  const currentStep = !status
    ? 0
    : evaluated
      ? 4
      : status.status === "APPROVED"
        ? 3
        : 2;

  return (
    <section className="tracking-page">
      <div className="tracking-heading">
        <p className="eyebrow">ANONİM GERİ BİLDİRİM TAKİBİ</p>
        <h1>Takip merkeziniz.</h1>
        <p className="muted">
          Anket gönderimi sonrasında size verilen takip numarasıyla geri bildiriminizin durumunu kimlik bilgisi
          paylaşmadan sorgulayabilirsiniz.
        </p>
      </div>

      <form
        className="card tracking-search"
        onSubmit={(event) => {
          event.preventDefault();
          fetchStatus(trackingNumber);
        }}
      >
        <label htmlFor="tracking-number">Takip numarası</label>
        <div className="tracking-search-row">
          <input
            id="tracking-number"
            value={trackingNumber}
            onChange={(event) => setTrackingNumber(event.target.value)}
            placeholder="TRK-…"
            required
          />
          <button className="button button-primary" disabled={busy}>
            <Search size={16} />
            {busy ? "Sorgulanıyor…" : "Durumu görüntüle"}
          </button>
        </div>
        <ErrorBanner message={error} />
      </form>

      {status && (
        <div className="card tracking-result">
          <ol className="step-timeline" aria-label="Geri bildirim süreci adımları">
            {[
              { n: 1, label: "Anket Alındı" },
              { n: 2, label: "İnceleme Sürecinde" },
              { n: 3, label: "Yanıt Yayımlandı" },
              { n: 4, label: "Değerlendirildi" },
            ].map((step) => (
              <li key={step.n} className={currentStep >= step.n ? "done" : ""}>
                <span>{currentStep >= step.n ? <Check size={13} /> : step.n}</span>
                <small>{step.label}</small>
              </li>
            ))}
          </ol>

          <div className="tracking-state">
            <span className="state-icon">
              <Activity size={20} />
            </span>
            <div>
              <span className="eyebrow">GÜNCEL DURUM</span>
              <h2>
                {status.status === "APPROVED"
                  ? "Yanıtınız hazır"
                  : status.status === "REVISION_REQUIRED"
                    ? "Yanıtınız güncelleniyor"
                    : status.status === "REJECTED"
                      ? "Süreç tamamlandı"
                      : "İnceleme sürüyor"}
              </h2>
              <p className="muted">Son güncelleme: {displayDate(status.updatedAt)}</p>
            </div>
            <StatusBadge status={status.status ?? "PENDING"} />
          </div>

          {status.notes && (
            <div className="review-note">
              <strong>Hasta Hakları Notu</strong>
              <p>{status.notes}</p>
            </div>
          )}

          {feedback && (
            <div className="published-feedback">
              <p className="eyebrow">
                <MessageSquareText size={14} /> SİZE ÖZEL ONAYLI GERİ BİLDİRİM
              </p>
              <p className="feedback-quote">{feedback.comment}</p>
            </div>
          )}

          {feedback && !evaluated && (
            <div className="satisfaction-actions">
              <span>Bu geri bildirimden memnun kaldınız mı? (Yalnızca bir kez değerlendirilebilir)</span>
              <div className="satisfaction-buttons">
                <button
                  type="button"
                  className="button button-outline"
                  onClick={() => submitSatisfaction("SATISFIED")}
                  disabled={busy}
                >
                  <ThumbsUp size={15} /> Evet, teşekkürler
                </button>
                <button
                  type="button"
                  className="button button-outline"
                  onClick={() => submitSatisfaction("NOT_SATISFIED")}
                  disabled={busy}
                >
                  <ThumbsDown size={15} /> Hayır
                </button>
              </div>
            </div>
          )}

          {evaluated && <SuccessBanner message="Değerlendirmeniz kaydedildi. Katkınız için teşekkür ederiz." />}
        </div>
      )}
    </section>
  );
}
