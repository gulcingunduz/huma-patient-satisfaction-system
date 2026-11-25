import { useEffect, useState } from "react";
import { Activity, AlertTriangle, BarChart3, Building2, Clock, Sparkles, TrendingUp } from "lucide-react";
import { api } from "../api.js";
import { daysAgoIsoDate, displayShortDate, todayIsoDate } from "../utils/format.js";
import { EmptyState, InfoBanner, LoadingState, ScorePill, SimpleTrendChart, StatCard } from "./Common.jsx";

export function DashboardPanel({ session, setError, onNavigate }) {
  const [departments, setDepartments] = useState([]);
  const [departmentId, setDepartmentId] = useState("");
  const [dateFrom, setDateFrom] = useState(daysAgoIsoDate(29));
  const [dateTo, setDateTo] = useState(todayIsoDate());
  const [overview, setOverview] = useState(null);
  const [latestSummary, setLatestSummary] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/api/departments/accessible")
      .then((list) => {
        setDepartments(list);
        if (session.role === "STAFF" && list[0]) {
          setDepartmentId(String(list[0].id));
        }
      })
      .catch((err) => setError(err.message));
  }, []);

  async function loadDashboard() {
    setBusy(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (departmentId) params.set("departmentId", departmentId);
      if (dateFrom) params.set("dateFrom", dateFrom);
      if (dateTo) params.set("dateTo", dateTo);
      const data = await api(`/api/reports/dashboard?${params.toString()}`);
      setOverview(data);

      const summaryDeptId = departmentId || departments[0]?.id;
      if (summaryDeptId) {
        const summaries = await api(`/api/ai-summaries?departmentId=${summaryDeptId}`).catch(() => []);
        setLatestSummary(summaries.find((item) => item.status === "COMPLETED") ?? summaries[0] ?? null);
      } else {
        setLatestSummary(null);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    loadDashboard();
  }, [departmentId, departments.length]);

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">YÖNETİCİ VE BİRİM ÖZETİ</p>
          <h1>Genel Bakış</h1>
          <p className="muted">Departman memnuniyeti, zaman içindeki eğilim, şikâyet temaları ve AI özeti</p>
        </div>
        <button type="button" className="button button-outline" onClick={loadDashboard} disabled={busy}>
          Yenile
        </button>
      </div>

      <form
        className="card filter-bar"
        onSubmit={(event) => {
          event.preventDefault();
          loadDashboard();
        }}
      >
        <div className="filter-field">
          <label htmlFor="dash-department">Birim</label>
          <select
            id="dash-department"
            value={departmentId}
            onChange={(event) => setDepartmentId(event.target.value)}
            disabled={session.role === "STAFF" && departments.length <= 1}
          >
            {session.role !== "STAFF" && <option value="">Tüm birimler</option>}
            {departments.map((dept) => (
              <option key={dept.id} value={dept.id}>
                {dept.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="dash-from">Başlangıç</label>
          <input id="dash-from" type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} required />
        </div>
        <div className="filter-field">
          <label htmlFor="dash-to">Bitiş</label>
          <input id="dash-to" type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} required />
        </div>
        <button className="button button-primary" disabled={busy}>
          <BarChart3 size={16} /> Filtrele
        </button>
      </form>

      {busy && !overview ? (
        <LoadingState label="Dashboard verileri hesaplanıyor…" />
      ) : !overview ? (
        <EmptyState title="Veri yüklenemedi" text="Filtre seçerek dashboard özetini yenileyin." />
      ) : (
        <>
          {!overview.sufficientData && overview.totalSubmissions > 0 && (
            <InfoBanner
              message={`Seçilen aralıkta ${overview.totalSubmissions} anket bulunuyor (yeterli istatistiksel güven eşiği: ${overview.minimumDataThreshold}). Sonuçlar ön izleme niteliğindedir.`}
            />
          )}

          <div className="kpi-grid">
            <StatCard
              label="Toplam Anket"
              value={overview.totalSubmissions}
              hint={`${displayShortDate(overview.dateFrom)} – ${displayShortDate(overview.dateTo)}`}
              icon={Activity}
              tone="primary"
            />
            <StatCard
              label="Ortalama Memnuniyet"
              value={overview.totalSubmissions > 0 ? `${Number(overview.overallAverage).toFixed(2)} / 5` : "—"}
              hint={overview.sufficientData ? "Yeterli örneklem" : "Düşük örneklem"}
              icon={TrendingUp}
              tone="teal"
            />
            <StatCard
              label="Düşük / Kritik Puan"
              value={`${overview.lowScoreCount} / ${overview.criticalScoreCount}`}
              hint="≤ 3 düşük · ≤ 2 kritik"
              icon={AlertTriangle}
              tone={overview.criticalScoreCount > 0 ? "danger" : "warning"}
            />
            <StatCard
              label="İnceleme Bekleyen"
              value={overview.pendingFeedbackCount}
              hint="Onaylanmamış geri bildirim"
              icon={Clock}
              tone="neutral"
            />
          </div>

          <div className="dashboard-grid">
            <section className="card section-card">
              <div className="section-head">
                <h2>Zaman İçindeki Değişim</h2>
                <span className="muted">Günlük ortalama puan</span>
              </div>
              <SimpleTrendChart points={overview.trend} />
            </section>

            <section className="card section-card">
              <div className="section-head">
                <h2>Departman Bazlı Memnuniyet</h2>
                <Building2 size={16} className="muted" />
              </div>
              {overview.departments.length === 0 ? (
                <p className="muted">Bu tarih aralığında departman verisi bulunmuyor.</p>
              ) : (
                <div className="dept-bars">
                  {overview.departments.map((dept) => {
                    const width = Math.max(4, Math.min(100, (Number(dept.averageScore) / 5) * 100));
                    return (
                      <div className="dept-bar-row" key={dept.departmentId}>
                        <div className="dept-bar-head">
                          <div>
                            <strong>{dept.departmentName}</strong>
                            <small>
                              {dept.submissionCount} anket · {dept.lowScoreCount} düşük puan
                              {!dept.sufficientData && " · Yetersiz veri"}
                            </small>
                          </div>
                          <ScorePill score={dept.averageScore} suffix=" / 5" />
                        </div>
                        <div className="bar-track">
                          <span style={{ width: `${width}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </div>

          <div className="dashboard-grid">
            <section className="card section-card">
              <div className="section-head">
                <h2>En Sık Tekrarlanan Şikâyetler</h2>
                <button type="button" className="link-button" onClick={() => onNavigate?.("reports")}>
                  Detaylı rapor
                </button>
              </div>
              {overview.topComplaints.length === 0 ? (
                <p className="muted">Seçilen dönemdeki yorumlarda belirgin bir şikâyet teması tespit edilmedi.</p>
              ) : (
                <div className="complaint-list">
                  {overview.topComplaints.map((item) => (
                    <div className="complaint-row" key={item.category}>
                      <span>{item.category}</span>
                      <span className="count-chip">{item.count} yorum</span>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="card section-card ai-highlight-card">
              <div className="section-head">
                <h2>
                  <Sparkles size={17} /> AI Özet Alanı
                </h2>
                <button type="button" className="link-button" onClick={() => onNavigate?.("summaries")}>
                  Tüm özetler
                </button>
              </div>
              {latestSummary ? (
                <div className="ai-preview">
                  <div className="ai-preview-meta">
                    <strong>{latestSummary.departmentName ?? "Seçili birim"}</strong>
                    <span>
                      {displayShortDate(latestSummary.dateFrom)} – {displayShortDate(latestSummary.dateTo)}
                    </span>
                    {latestSummary.surveyVersionScope && <span>{latestSummary.surveyVersionScope}</span>}
                  </div>
                  <p>{latestSummary.summaryText}</p>
                </div>
              ) : (
                <p className="muted">
                  Henüz oluşturulmuş bir AI özeti bulunmuyor. AI Özeti sekmesinden anonim yorum özeti oluşturabilirsiniz.
                </p>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
}
