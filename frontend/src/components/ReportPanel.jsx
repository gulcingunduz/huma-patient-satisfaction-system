import { useEffect, useState } from "react";
import { Activity, AlertCircle, BarChart3, MessageSquareWarning } from "lucide-react";
import { api } from "../api.js";
import { daysAgoIsoDate, todayIsoDate } from "../utils/format.js";
import { EmptyState, InfoBanner, ScorePill, SimpleTrendChart, StatCard } from "./Common.jsx";

export function ReportPanel({ setError }) {
  const [departments, setDepartments] = useState([]);
  const [departmentId, setDepartmentId] = useState("");
  const [dateFrom, setDateFrom] = useState(daysAgoIsoDate(29));
  const [dateTo, setDateTo] = useState(todayIsoDate());
  const [lowScoreOnly, setLowScoreOnly] = useState(false);
  const [report, setReport] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/api/departments/accessible")
      .then((items) => {
        setDepartments(items);
        if (!departmentId && items[0]) setDepartmentId(String(items[0].id));
      })
      .catch((requestError) => setError(requestError.message));
  }, []);

  async function generateReport(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      setReport(
        await api("/api/reports/departments/summary", {
          method: "POST",
          body: { departmentId: Number(departmentId), dateFrom, dateTo, lowScoreOnly },
        })
      );
    } catch (requestError) {
      setError(requestError.message);
      setReport(null);
    } finally {
      setBusy(false);
    }
  }

  const totalRatedAnswers = report
    ? Object.values(report.scoreDistribution ?? {}).reduce((sum, v) => sum + Number(v), 0)
    : 0;

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">METRİK BAZLI KALİTE RAPORLAMASI</p>
          <h1>Departman Raporları</h1>
          <p className="muted">Ortak metrikler, departmana özel metrikler, puan dağılımı ve tekrarlayan şikâyet temaları</p>
        </div>
      </div>

      <form className="card filter-bar" onSubmit={generateReport}>
        <div className="filter-field">
          <label htmlFor="report-department">Birim</label>
          <select
            id="report-department"
            value={departmentId}
            onChange={(event) => setDepartmentId(event.target.value)}
            required
          >
            <option value="">Birim seçin</option>
            {departments.map((department) => (
              <option value={department.id} key={department.id}>
                {department.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="report-date-from">Başlangıç</label>
          <input
            id="report-date-from"
            type="date"
            value={dateFrom}
            onChange={(event) => setDateFrom(event.target.value)}
            required
          />
        </div>
        <div className="filter-field">
          <label htmlFor="report-date-to">Bitiş</label>
          <input
            id="report-date-to"
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
          <Activity size={16} />
          {busy ? "Hazırlanıyor…" : "Raporu göster"}
        </button>
      </form>

      {!report ? (
        <EmptyState title="Rapor bekleniyor" text="Birim ve tarih aralığı seçerek metrik ortalamalarını hesaplayın." />
      ) : report.submissionCount === 0 ? (
        <EmptyState title="Yeterli veri yok" text="Seçilen filtrelerde anket sonucu bulunamadı." />
      ) : (
        <>
          {!report.sufficientData && (
            <InfoBanner
              message={`Bu filtrede yalnızca ${report.submissionCount} gönderim bulunuyor (minimum güven eşiği: ${report.minimumDataThreshold}). Az sayıda veriye dayalı ortalamaları dikkatle yorumlayın.`}
            />
          )}

          <div className="kpi-grid">
            <StatCard label="Toplam Anket" value={report.submissionCount} icon={Activity} tone="primary" />
            <StatCard
              label="Genel Ortalama"
              value={`${Number(report.overallAverage).toFixed(2)} / 5`}
              icon={BarChart3}
              tone="teal"
            />
            <StatCard label="Toplam Cevap" value={report.answerCount} icon={Activity} tone="neutral" />
            <StatCard
              label="Düşük Puan (≤ 3)"
              value={report.lowScoreCount ?? 0}
              icon={AlertCircle}
              tone={(report.lowScoreCount ?? 0) > 0 ? "warning" : "neutral"}
            />
          </div>

          <div className="dashboard-grid">
            <section className="card section-card">
              <div className="section-head">
                <h2>Ortak Metrikler (COMMON)</h2>
              </div>
              {report.commonMetrics.length === 0 ? (
                <p className="muted">Bu aralıkta ortak RATING metriği bulunmuyor.</p>
              ) : (
                report.commonMetrics.map((metric) => <MetricBar key={`common-${metric.metricCode}`} metric={metric} />)
              )}
            </section>

            <section className="card section-card">
              <div className="section-head">
                <h2>Departman Metrikleri (DEPARTMENT_SPECIFIC)</h2>
              </div>
              {report.departmentMetrics.length === 0 ? (
                <p className="muted">Bu aralıkta departmana özel RATING metriği bulunmuyor.</p>
              ) : (
                report.departmentMetrics.map((metric) => (
                  <MetricBar key={`department-${metric.metricCode}`} metric={metric} />
                ))
              )}
            </section>
          </div>

          <div className="dashboard-grid">
            <section className="card section-card">
              <div className="section-head">
                <h2>Puan Dağılımı (1–5)</h2>
              </div>
              {Object.entries(report.scoreDistribution).map(([score, count]) => (
                <div className="distribution-row" key={score}>
                  <span>{score} puan</span>
                  <div className="bar-track">
                    <span style={{ width: `${totalRatedAnswers > 0 ? (Number(count) / totalRatedAnswers) * 100 : 0}%` }} />
                  </div>
                  <strong>{count}</strong>
                </div>
              ))}
            </section>

            <section className="card section-card">
              <div className="section-head">
                <h2>
                  <MessageSquareWarning size={16} /> Tekrarlayan Şikâyet Temaları
                </h2>
              </div>
              {report.complaints.length === 0 ? (
                <p className="muted">Yorumlarda belirgin bir şikâyet teması tespit edilmedi.</p>
              ) : (
                report.complaints.map((complaint) => (
                  <div className="complaint-row" key={complaint.category}>
                    <span>{complaint.category}</span>
                    <span className="count-chip">{complaint.count}</span>
                  </div>
                ))
              )}
            </section>
          </div>

          {report.trend?.length > 0 && (
            <section className="card section-card">
              <div className="section-head">
                <h2>Dönemsel Puan Trendi</h2>
              </div>
              <SimpleTrendChart points={report.trend} />
            </section>
          )}
        </>
      )}
    </div>
  );
}

function MetricBar({ metric }) {
  const percent = Math.max(0, Math.min(100, (Number(metric.averageScore) / 5) * 100));
  return (
    <div className="metric-row">
      <div className="metric-row-heading">
        <div>
          <strong>
            {metric.metricCode}
            {metric.sufficientData === false && <span className="low-sample-tag">Yetersiz veri</span>}
          </strong>
          <span>{metric.questionText}</span>
        </div>
        <ScorePill score={metric.averageScore} suffix=" / 5" />
      </div>
      <div className="bar-track">
        <span style={{ width: `${percent}%` }} />
      </div>
      <small className="muted">{metric.answerCount} yanıt</small>
    </div>
  );
}
