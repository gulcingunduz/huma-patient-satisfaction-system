import { useEffect, useState } from "react";
import {
  Activity,
  BarChart3,
  Bell,
  ChevronRight,
  ClipboardCheck,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Settings,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { api, getCsrfToken } from "./api.js";
import { roleLabel } from "./utils/format.js";
import { HumaBirdIcon } from "./components/HumaBirdIcon.jsx";
import { ErrorBanner, LoadingState } from "./components/Common.jsx";
import { SurveyView } from "./components/SurveyView.jsx";
import { TrackingView } from "./components/TrackingView.jsx";
import { LoginView } from "./components/LoginView.jsx";
import { DashboardPanel } from "./components/DashboardPanel.jsx";
import { SubmissionsPanel } from "./components/SubmissionsPanel.jsx";
import { PatientRightsPanel } from "./components/PatientRightsPanel.jsx";
import { NotificationsPanel } from "./components/NotificationsPanel.jsx";
import { ReportPanel } from "./components/ReportPanel.jsx";
import { SummaryPanel } from "./components/SummaryPanel.jsx";
import { AdminPanel } from "./components/AdminPanel.jsx";

const NAV_ITEMS = [
  { id: "survey", label: "Anket", icon: ClipboardList },
  { id: "tracking", label: "Takip", icon: Activity },
  { id: "workspace", label: "Personel", icon: LayoutDashboard },
];

function App() {
  const [view, setView] = useState("survey");
  const [session, setSession] = useState(null);
  const [sessionChecked, setSessionChecked] = useState(false);
  const [sessionNotice, setSessionNotice] = useState("");
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [accessToken, setAccessToken] = useState(new URLSearchParams(window.location.search).get("access") ?? "");
  const [prefillTracking, setPrefillTracking] = useState("");

  useEffect(() => {
    api("/api/auth/me")
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setSessionChecked(true));
  }, []);

  useEffect(() => {
    function handleExpired(event) {
      setSession(null);
      setShowLogoutConfirm(false);
      setSessionNotice(
        event.detail || "Hesabınıza başka bir cihazdan giriş yapıldığı için bu oturum sonlandırıldı."
      );
      setView("workspace");
    }
    window.addEventListener("huma:session-expired", handleExpired);
    return () => window.removeEventListener("huma:session-expired", handleExpired);
  }, []);

  useEffect(() => {
    if (!showLogoutConfirm) return;
    function onKeyDown(e) {
      if (e.key === "Escape") setShowLogoutConfirm(false);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [showLogoutConfirm]);

  async function signIn(credentials) {
    setSessionNotice("");
    await getCsrfToken();
    setSession(await api("/api/auth/login", { method: "POST", body: credentials }));
  }

  async function confirmSignOut() {
    setLoggingOut(true);
    try {
      await api("/api/auth/logout", { method: "POST" });
    } finally {
      setLoggingOut(false);
      setShowLogoutConfirm(false);
      setSession(null);
      setSessionNotice("");
      setView("survey");
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a
          className="brand"
          href="#survey"
          onClick={(e) => {
            e.preventDefault();
            setView("survey");
          }}
          aria-label="Hüma ana sayfa"
        >
          <span className="brand-mark">
            <HumaBirdIcon size={24} />
          </span>
          <span>
            <strong>Hüma</strong>
            <small>HASTA MEMNUNİYET SİSTEMİ</small>
          </span>
        </a>

        <nav className="main-nav" aria-label="Ana gezinme">
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              className={view === id ? "nav-link active" : "nav-link"}
              onClick={() => setView(id)}
            >
              <Icon size={16} />
              {label}
            </button>
          ))}
        </nav>

        <div className="topbar-meta">
          <span className="status-dot" />
          <span>{session ? session.fullName : "Anonim erişim"}</span>
          {session && (
            <button
              type="button"
              className="icon-button"
              title="Oturumu kapat"
              onClick={() => setShowLogoutConfirm(true)}
              aria-label="Oturumu kapat"
            >
              <LogOut size={16} />
            </button>
          )}
        </div>
      </header>

      <main className="page-wrap">
        {view === "survey" && (
          <SurveyView
            accessToken={accessToken}
            setAccessToken={setAccessToken}
            onOpenTracking={(code) => {
              setPrefillTracking(code);
              setView("tracking");
            }}
          />
        )}
        {view === "tracking" && <TrackingView initialTrackingNumber={prefillTracking} />}
        {view === "workspace" &&
          (!sessionChecked ? (
            <LoadingState label="Oturum kontrol ediliyor…" />
          ) : session ? (
            <WorkspaceView session={session} />
          ) : (
            <LoginView onLogin={signIn} sessionNotice={sessionNotice} />
          ))}
      </main>

      <footer className="site-footer">
        <span className="footer-brand">
          <HumaBirdIcon size={16} /> HÜMA SAĞLIK DENEYİMİ
        </span>
        <span>Anonim hasta geri bildirimi ile sürekli kalite iyileştirme.</span>
      </footer>

      {showLogoutConfirm && (
        <div
          className="modal-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="logout-modal-title"
          onClick={() => !loggingOut && setShowLogoutConfirm(false)}
        >
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-icon">
              <LogOut size={22} />
            </div>
            <h2 id="logout-modal-title">Çıkış yapmak istiyor musunuz?</h2>
            <p className="muted">
              Onayladığınızda güvenli oturumunuz sonlandırılacak ve anket ana sayfasına yönlendirileceksiniz.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="button button-outline"
                disabled={loggingOut}
                onClick={() => setShowLogoutConfirm(false)}
              >
                Vazgeç
              </button>
              <button
                type="button"
                className="button button-danger"
                disabled={loggingOut}
                onClick={confirmSignOut}
              >
                {loggingOut ? "Çıkış yapılıyor…" : "Evet, çıkış yap"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function WorkspaceView({ session }) {
  const defaultTab =
    session.role === "PATIENT_RIGHTS_OFFICER"
      ? "rights"
      : session.role === "ADMIN"
        ? "dashboard"
        : "submissions";
  const [tab, setTab] = useState(defaultTab);
  const [error, setError] = useState("");
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    api("/api/notifications?unreadOnly=true")
      .then((list) => setUnreadCount(Array.isArray(list) ? list.length : 0))
      .catch(() => setUnreadCount(0));
  }, [session.username]);

  const tabs = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
    { id: "submissions", label: "Sonuçlar", icon: ClipboardList },
    ...(session.role === "PATIENT_RIGHTS_OFFICER" || session.role === "ADMIN"
      ? [{ id: "rights", label: "Hasta Hakları", icon: ClipboardCheck }]
      : []),
    { id: "messages", label: "Bildirimler", icon: Bell, badge: unreadCount },
    { id: "reports", label: "Raporlar", icon: BarChart3 },
    { id: "summaries", label: "AI Özeti", icon: Sparkles },
    ...(session.role === "ADMIN" ? [{ id: "admin", label: "Yönetim", icon: Settings }] : []),
  ];

  return (
    <section className="workspace">
      <aside className="workspace-rail" aria-label="Çalışma alanı menüsü">
        <div className="workspace-user">
          <span className="avatar">{session.fullName.slice(0, 1).toLocaleUpperCase("tr-TR")}</span>
          <div>
            <strong>{session.fullName}</strong>
            <small>{roleLabel(session.role)}</small>
          </div>
        </div>
        <div className="rail-separator" />
        <div className="rail-nav">
          {tabs.map(({ id, label, icon: Icon, badge }) => (
            <button
              key={id}
              type="button"
              className={tab === id ? "rail-link selected" : "rail-link"}
              onClick={() => {
                setTab(id);
                setError("");
              }}
            >
              <Icon size={17} />
              <span>{label}</span>
              {badge > 0 && <span className="nav-badge">{badge}</span>}
              <ChevronRight size={14} className="rail-chevron" />
            </button>
          ))}
        </div>
        <div className="rail-note">
          <ShieldCheck size={15} />
          <span>Rolünüz kapsamındaki anonim kayıtlar gösterilir.</span>
        </div>
      </aside>

      <div className="workspace-content">
        <ErrorBanner message={error} />
        {tab === "dashboard" && <DashboardPanel session={session} setError={setError} onNavigate={setTab} />}
        {tab === "submissions" && <SubmissionsPanel session={session} setError={setError} />}
        {tab === "rights" && <PatientRightsPanel setError={setError} />}
        {tab === "messages" && <NotificationsPanel setError={setError} onUnreadCountChange={setUnreadCount} />}
        {tab === "reports" && <ReportPanel setError={setError} />}
        {tab === "summaries" && <SummaryPanel setError={setError} />}
        {tab === "admin" && <AdminPanel setError={setError} />}
      </div>
    </section>
  );
}

export default App;
