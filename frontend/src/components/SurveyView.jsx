import { useEffect, useState } from "react";
import { ArrowUpRight, Building2, Check, ChevronRight, KeyRound, ShieldCheck } from "lucide-react";
import { api } from "../api.js";
import { HumaBirdIcon } from "./HumaBirdIcon.jsx";
import { CopyValue, ErrorBanner } from "./Common.jsx";

const RATING_LABELS = {
  1: "Çok kötü",
  2: "Kötü",
  3: "Orta",
  4: "İyi",
  5: "Çok iyi",
};

export function SurveyView({ accessToken, setAccessToken, onOpenTracking }) {
  const [tokenInput, setTokenInput] = useState(accessToken);
  const [surveyInfo, setSurveyInfo] = useState(null);
  const [questions, setQuestions] = useState(null);
  const [answers, setAnswers] = useState({});
  const [trackingNumber, setTrackingNumber] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (accessToken) loadQuestions(accessToken);
  }, []);

  async function loadQuestions(token) {
    if (!token) return;
    setBusy(true);
    setError("");
    try {
      const [infoResult, questionResult] = await Promise.all([
        api(`/api/public/surveys/access/${encodeURIComponent(token)}`).catch(() => null),
        api(`/api/public/surveys/access/${encodeURIComponent(token)}/questions`),
      ]);
      setSurveyInfo(infoResult);
      setQuestions(questionResult);
      setAccessToken(token);
      const url = new URL(window.location.href);
      url.searchParams.set("access", token);
      window.history.replaceState({}, "", url);
    } catch (requestError) {
      setQuestions(null);
      setSurveyInfo(null);
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function submitSurvey(event) {
    event.preventDefault();
    const missing = questions.find((question) => question.required && !answers[question.id]);
    if (missing) {
      setError(`Lütfen zorunlu soruyu yanıtlayın: ${missing.text}`);
      return;
    }
    setBusy(true);
    setError("");
    try {
      const requestAnswers = questions
        .map((question) => {
          const answer = answers[question.id];
          return question.type === "RATING"
            ? { questionId: question.id, score: Number(answer), answerValue: String(answer) }
            : { questionId: question.id, answerValue: answer?.trim() || null };
        })
        .filter((answer) => answer.answerValue || answer.score);

      const surveyId = surveyInfo?.surveyId ?? questions[0]?.surveyId;
      if (!surveyId) throw new Error("Anket bilgisi eksik. Yöneticiye bildirin.");

      const result = await api(`/api/public/surveys/${encodeURIComponent(accessToken)}/submissions`, {
        method: "POST",
        body: { surveyId, answers: requestAnswers },
      });
      setTrackingNumber(result.trackingNumber);
      setAnswers({});
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  if (trackingNumber) {
    return (
      <section className="card success-screen">
        <span className="success-icon">
          <Check size={28} />
        </span>
        <p className="eyebrow">YANIT KAYDEDİLDİ</p>
        <h1>Teşekkür ederiz.</h1>
        <p className="muted">
          Anonim değerlendirmeniz güvenli biçimde kaydedildi. Geri bildirim sürecinizi aşağıdaki takip numarasıyla
          istediğiniz zaman sorgulayabilirsiniz.
        </p>
        <CopyValue value={trackingNumber} />
        <div className="success-actions">
          {onOpenTracking && (
            <button
              type="button"
              className="button button-primary"
              onClick={() => onOpenTracking(trackingNumber)}
            >
              Takip ekranına git <ChevronRight size={16} />
            </button>
          )}
          <button
            type="button"
            className="button button-outline"
            onClick={() => {
              setTrackingNumber("");
              setQuestions(null);
              setSurveyInfo(null);
            }}
          >
            Yeni anket başlat
          </button>
        </div>
      </section>
    );
  }

  const answeredCount = questions
    ? questions.filter((q) => {
        const val = answers[q.id];
        return typeof val === "number" ? val > 0 : Boolean(val && String(val).trim());
      }).length
    : 0;
  const progressPercent = questions?.length
    ? Math.max(6, Math.round((answeredCount / questions.length) * 100))
    : 0;

  return (
    <div className="survey-layout">
      <section className="survey-intro">
        <div className="intro-ribbon">
          <span className="ribbon-brand">
            <HumaBirdIcon size={16} /> HÜMA HASTA DENEYİMİ
          </span>
          <span>ANONİM ANKET</span>
        </div>
        <div className="intro-content">
          <p className="eyebrow">GÖRÜŞLERİNİZ HİZMET KALİTEMİZİ YÜKSELTİR</p>
          <h1>
            Daha iyi bir sağlık deneyimi <em>sizinle</em> başlar.
          </h1>
          <p className="intro-copy">
            Aldığınız sağlık hizmetini değerlendirin. Ad, soyad veya T.C. kimlik numarası gibi hiçbir kişisel bilgi
            istenmez; tüm yanıtlarınız tamamen anonim olarak kaydedilir.
          </p>
          <div className="intro-highlights">
            <div>
              <strong>%100 Anonim</strong>
              <span>Kimlik bilgisi toplanmaz</span>
            </div>
            <div>
              <strong>2 Dakika</strong>
              <span>Hızlı ve mobil uyumlu</span>
            </div>
          </div>
          <div className="intro-foot">
            <span className="privacy-seal">
              <ShieldCheck size={16} /> KVKK UYUMLU ANONİM DEĞERLENDİRME
            </span>
          </div>
        </div>
        <HumaBirdIcon size={150} className="intro-watermark" />
      </section>

      <section className="survey-panel" aria-labelledby="survey-title">
        {!questions ? (
          <div className="token-gate">
            <div className="section-index">
              <span>ANKET ERİŞİMİ</span>
              <span>QR / BAĞLANTI</span>
            </div>
            <h2 id="survey-title">Anket bağlantınız hazır mı?</h2>
            <p className="muted">
              Birimdeki QR kodu okutarak doğrudan başlayabilir veya size iletilen erişim kodunu aşağıya girebilirsiniz.
            </p>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                loadQuestions(tokenInput.trim());
              }}
            >
              <label htmlFor="access-token">Erişim kodu</label>
              <div className="input-with-icon">
                <KeyRound size={17} />
                <input
                  id="access-token"
                  value={tokenInput}
                  onChange={(event) => setTokenInput(event.target.value)}
                  placeholder="Birim erişim kodunu girin"
                  required
                />
              </div>
              <ErrorBanner message={error} />
              <button className="button button-primary full-width" disabled={busy}>
                {busy ? (
                  "Sorular yükleniyor…"
                ) : (
                  <>
                    Ankete başla <ArrowUpRight size={17} />
                  </>
                )}
              </button>
            </form>
            <p className="privacy-note">
              <ShieldCheck size={15} /> Kimliğinizi belirleyen hiçbir bilgi talep edilmez.
            </p>
          </div>
        ) : (
          <form className="question-form" onSubmit={submitSurvey}>
            <div className="section-index">
              <span>{surveyInfo?.surveyTitle ?? "HÜMA HASTA MEMNUNİYET ANKETİ"}</span>
              <span>
                {answeredCount} / {questions.length} YANITLANDI
              </span>
            </div>
            {surveyInfo?.departmentName && (
              <div className="department-pill">
                <Building2 size={15} />
                <span>{surveyInfo.departmentName}</span>
                {surveyInfo.surveyVersionNumber && <small>Sürüm v{surveyInfo.surveyVersionNumber}</small>}
              </div>
            )}
            <h2 id="survey-title">Ziyaretiniz nasıldı?</h2>
            <p className="muted">
              Her yanıtınız anonim olarak kaydedilir ve yalnızca hizmet kalitesini iyileştirmek için kullanılır.
            </p>

            <div
              className="survey-progress"
              role="progressbar"
              aria-label="Anket ilerleme durumu"
              aria-valuenow={progressPercent}
              aria-valuemin={0}
              aria-valuemax={100}
            >
              <span style={{ width: `${progressPercent}%` }} />
            </div>

            {["COMMON", "DEPARTMENT_SPECIFIC"].map((scope) => {
              const groupedQuestions = questions.filter((question) => (question.scope ?? "COMMON") === scope);
              if (groupedQuestions.length === 0) return null;
              return (
                <div className="question-group" key={scope}>
                  <p className="question-group-label">
                    {scope === "COMMON" ? "GENEL DEĞERLENDİRME SORULARI" : "BİRİME ÖZEL SORULAR"}
                  </p>
                  {groupedQuestions.map((question, index) => (
                    <fieldset className="question-block" key={question.id}>
                      <legend>
                        <span className="question-number">{String(index + 1).padStart(2, "0")}</span>
                        <span className="question-text">
                          {question.text}
                          {question.required && <span className="required-mark"> *</span>}
                        </span>
                      </legend>
                      {question.type === "RATING" ? (
                        <div className="rating-row" role="radiogroup" aria-label={question.text}>
                          {[1, 2, 3, 4, 5].map((score) => (
                            <label
                              key={score}
                              data-score={score}
                              className={answers[question.id] === score ? "rating-option selected" : "rating-option"}
                            >
                              <input
                                type="radio"
                                name={`question-${question.id}`}
                                value={score}
                                required={question.required}
                                checked={answers[question.id] === score}
                                onChange={() => setAnswers({ ...answers, [question.id]: score })}
                              />
                              <strong>{score}</strong>
                              <small>{RATING_LABELS[score]}</small>
                            </label>
                          ))}
                        </div>
                      ) : (
                        <div className="textarea-wrap">
                          <textarea
                            maxLength={500}
                            placeholder="Kişisel bilgi (ad, telefon, T.C. kimlik no) paylaşmadan görüş veya önerinizi yazabilirsiniz."
                            value={answers[question.id] ?? ""}
                            onChange={(event) => setAnswers({ ...answers, [question.id]: event.target.value })}
                          />
                          <small className="char-counter">{(answers[question.id] ?? "").length} / 500</small>
                        </div>
                      )}
                    </fieldset>
                  ))}
                </div>
              );
            })}

            <ErrorBanner message={error} />
            <div className="form-actions">
              <span className="muted">* Zorunlu alan</span>
              <button className="button button-primary" disabled={busy}>
                {busy ? (
                  "Gönderiliyor…"
                ) : (
                  <>
                    Yanıtı gönder <ChevronRight size={17} />
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
