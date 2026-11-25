import { useState } from "react";
import { Lock } from "lucide-react";
import { HumaBirdIcon } from "./HumaBirdIcon.jsx";
import { ErrorBanner, InfoBanner } from "./Common.jsx";

export function LoginView({ onLogin, sessionNotice = "" }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await onLogin({ username: username.trim(), password });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card login-layout">
      <div className="login-aside">
        <span className="login-badge">
          <HumaBirdIcon size={18} /> HÜMA YETKİLİ ERİŞİM
        </span>
        <h1>Hasta deneyimini veriyle iyileştirin.</h1>
        <p>
          Birim memnuniyet metriklerini, geri bildirim onay süreçlerini, kritik bildirimleri ve anonim AI özetlerini
          rolünüze özel güvenli oturum üzerinden yönetin.
        </p>
        <ul className="login-features">
          <li>Varsayılan hastane birimleri ve hazır anket şablonu</li>
          <li>Rol bazlı birim izolasyonu ve CSRF korumalı oturum</li>
          <li>Metrik ve sürüm bazlı raporlama & Hasta Hakları onayı</li>
        </ul>
      </div>

      <form className="login-form" onSubmit={submit}>
        <p className="eyebrow">HÜMA PERSONEL VE YÖNETİM PORTALI</p>
        <h2>Oturum açın</h2>
        <InfoBanner message={sessionNotice} />

        <label htmlFor="username">Kullanıcı adı</label>
        <input
          id="username"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="kurumsal kullanıcı adınız"
          required
        />

        <label htmlFor="password">Parola</label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
          required
        />

        <ErrorBanner message={error} />

        <button className="button button-primary full-width" disabled={busy}>
          <Lock size={16} />
          {busy ? "Giriş yapılıyor…" : "Güvenli giriş"}
        </button>

        <span className="muted login-footnote">
          Kurumsal hesabınızla giriş yapın. Yetkisiz erişim denemeleri kısıtlanır.
        </span>
      </form>
    </section>
  );
}
