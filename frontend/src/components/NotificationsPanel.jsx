import { useEffect, useState } from "react";
import { Bell, CheckCheck, TriangleAlert } from "lucide-react";
import { api } from "../api.js";
import { displayDate } from "../utils/format.js";
import { EmptyState, LoadingState } from "./Common.jsx";

export function NotificationsPanel({ setError, onUnreadCountChange }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [busyAll, setBusyAll] = useState(false);
  const [loading, setLoading] = useState(true);

  async function refresh() {
    setLoading(true);
    try {
      const list = await api(`/api/notifications?unreadOnly=${unreadOnly}`);
      setNotifications(list);
      if (!unreadOnly) {
        onUnreadCountChange?.(list.filter((n) => !n.read).length);
      } else {
        onUnreadCountChange?.(list.length);
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, [unreadOnly]);

  async function markRead(id) {
    setBusyId(id);
    try {
      await api(`/api/notifications/${id}/read`, { method: "PATCH" });
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function markAllRead() {
    setBusyAll(true);
    try {
      await api("/api/notifications/read-all", { method: "PATCH" });
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyAll(false);
    }
  }

  const hasUnread = notifications.some((n) => !n.read);

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">PUAN BAZLI SİSTEM BİLDİRİMLERİ</p>
          <h1>Bildirimler</h1>
          <p className="muted">Düşük puanlı (≤ 3) ve kritik (≤ 2) anket gönderimlerine ait bildirimler</p>
        </div>
        <div className="heading-actions">
          <label className="toggle-control">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(event) => setUnreadOnly(event.target.checked)}
            />
            Yalnızca okunmamış
          </label>
          {hasUnread && (
            <button type="button" className="button button-outline compact" onClick={markAllRead} disabled={busyAll}>
              <CheckCheck size={15} /> Tümünü okundu işaretle
            </button>
          )}
        </div>
      </div>

      <div className="card notification-list">
        {loading ? (
          <LoadingState label="Bildirimler yükleniyor…" />
        ) : notifications.length === 0 ? (
          <EmptyState title="Bildirim yok" text="Yeni bir puan bildirimi oluştuğunda burada listelenecektir." />
        ) : (
          notifications.map((item) => (
            <article className={item.read ? "notification-item read" : "notification-item"} key={item.id}>
              <span className={item.type === "SYSTEM_ALERT" ? "notification-icon alert" : "notification-icon"}>
                {item.type === "SYSTEM_ALERT" ? <TriangleAlert size={18} /> : <Bell size={18} />}
              </span>
              <div className="notification-body">
                <strong>{item.message}</strong>
                <small>
                  {displayDate(item.createdAt)} ·{" "}
                  {item.type === "SYSTEM_ALERT" ? "Kritik Bildirim (≤ 2)" : "Düşük Puan Bildirimi (≤ 3)"}
                </small>
              </div>
              {!item.read && (
                <button
                  type="button"
                  className="button button-outline compact"
                  disabled={busyId === item.id}
                  onClick={() => markRead(item.id)}
                >
                  Okundu
                </button>
              )}
            </article>
          ))
        )}
      </div>
    </div>
  );
}
