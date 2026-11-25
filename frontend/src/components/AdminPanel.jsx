import { useEffect, useMemo, useState } from "react";
import { Building2, ClipboardList, ExternalLink, GitBranch, KeyRound, Lock, Plus, Trash2, Users } from "lucide-react";
import { api } from "../api.js";
import { displayDate, roleLabel } from "../utils/format.js";
import { CopyValue, EmptyState, InfoBanner, StatusBadge, SuccessBanner } from "./Common.jsx";

const ADMIN_SUBTABS = [
  { id: "departments", label: "Departmanlar", icon: Building2 },
  { id: "users", label: "Kullanıcılar & Roller", icon: Users },
  { id: "surveys", label: "Anketler & QR Erişim", icon: KeyRound },
  { id: "questions", label: "Sorular & Metrikler", icon: ClipboardList },
  { id: "versions", label: "Anket Sürümleri", icon: GitBranch },
];

const METRIC_SUGGESTIONS = [
  "COMMUNICATION",
  "INFORMATION",
  "SERVICE_ATTENTION",
  "GENERAL_EXPERIENCE",
  "FIRST_RESPONSE_TIME",
  "EMERGENCY_PROCESS",
  "APPOINTMENT_PROCESS",
  "WAITING_TIME",
  "EXAMINATION_EXPERIENCE",
  "NURSING_CARE",
  "ROOM_COMFORT",
  "CLEANLINESS",
  "SAMPLE_COLLECTION",
  "RESULT_ACCESS",
  "IMAGING_PROCESS",
  "PROCEDURE_INFORMATION",
  "GENERAL_COMMENT",
];

export function AdminPanel({ setError }) {
  const [subtab, setSubtab] = useState("departments");
  const [departments, setDepartments] = useState([]);
  const [surveys, setSurveys] = useState([]);
  const [users, setUsers] = useState([]);
  const [versions, setVersions] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [accesses, setAccesses] = useState([]);

  const [surveyId, setSurveyId] = useState("");
  const [selectedVersionId, setSelectedVersionId] = useState("");
  const [scopeFilter, setScopeFilter] = useState("ALL");
  const [accessDepartmentId, setAccessDepartmentId] = useState("");

  const [departmentName, setDepartmentName] = useState("");
  const [surveyTitle, setSurveyTitle] = useState("");
  const [publicToken, setPublicToken] = useState("");

  // New user form
  const [newUsername, setNewUsername] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newFullName, setNewFullName] = useState("");
  const [newRole, setNewRole] = useState("STAFF");
  const [newUserDeptId, setNewUserDeptId] = useState("");

  // New question form
  const [questionText, setQuestionText] = useState("");
  const [questionType, setQuestionType] = useState("RATING");
  const [questionScope, setQuestionScope] = useState("COMMON");
  const [questionDepartmentId, setQuestionDepartmentId] = useState("");
  const [metricCode, setMetricCode] = useState("");
  const [questionRequired, setQuestionRequired] = useState(true);
  const [questionOrder, setQuestionOrder] = useState(10);

  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  async function refreshBase() {
    try {
      const [deptList, surveyList, userList] = await Promise.all([
        api("/api/admin/departments?includeInactive=true"),
        api("/api/admin/surveys?includeInactive=true"),
        api("/api/admin/users").catch(() => []),
      ]);
      setDepartments(deptList);
      setSurveys(surveyList);
      setUsers(userList);

      const activeDepts = deptList.filter((d) => d.active);
      if (!accessDepartmentId && activeDepts[0]) setAccessDepartmentId(String(activeDepts[0].id));
      if (!questionDepartmentId && activeDepts[0]) setQuestionDepartmentId(String(activeDepts[0].id));
      if (!newUserDeptId && activeDepts[0]) setNewUserDeptId(String(activeDepts[0].id));
      if (!surveyId && surveyList[0]) setSurveyId(String(surveyList[0].id));
    } catch (err) {
      setError(err.message);
    }
  }

  async function loadSurveyDetails(targetSurveyId, preferredVersionId = null) {
    if (!targetSurveyId) {
      setVersions([]);
      setQuestions([]);
      return;
    }
    try {
      const versionList = await api(`/api/admin/surveys/${targetSurveyId}/versions`);
      setVersions(versionList);
      const nextVersion =
        preferredVersionId ??
        (selectedVersionId && versionList.some((v) => String(v.id) === String(selectedVersionId))
          ? selectedVersionId
          : versionList.find((v) => v.status === "DRAFT")?.id ??
            versionList.find((v) => v.status === "PUBLISHED")?.id ??
            "");
      setSelectedVersionId(nextVersion ? String(nextVersion) : "");
      const qUrl = nextVersion
        ? `/api/admin/surveys/${targetSurveyId}/questions?versionId=${nextVersion}&includeInactive=true`
        : `/api/admin/surveys/${targetSurveyId}/questions?includeInactive=true`;
      const qList = await api(qUrl);
      setQuestions(qList);
      setQuestionOrder((qList.length + 1) * 10);
    } catch (err) {
      setError(err.message);
    }
  }

  async function loadAccesses(targetDeptId) {
    if (!targetDeptId) {
      setAccesses([]);
      return;
    }
    try {
      setAccesses(await api(`/api/admin/survey-accesses?departmentId=${targetDeptId}`));
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    refreshBase();
  }, []);

  useEffect(() => {
    loadSurveyDetails(surveyId);
  }, [surveyId]);

  useEffect(() => {
    if (!surveyId) return;
    const qUrl = selectedVersionId
      ? `/api/admin/surveys/${surveyId}/questions?versionId=${selectedVersionId}&includeInactive=true`
      : `/api/admin/surveys/${surveyId}/questions?includeInactive=true`;
    api(qUrl)
      .then((qList) => {
        setQuestions(qList);
        setQuestionOrder((qList.length + 1) * 10);
      })
      .catch((err) => setError(err.message));
  }, [selectedVersionId]);

  useEffect(() => {
    loadAccesses(accessDepartmentId);
  }, [accessDepartmentId]);

  async function runAction(action) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
      await refreshBase();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const activeDepartments = useMemo(() => departments.filter((d) => d.active), [departments]);
  const selectedVersion = versions.find((v) => String(v.id) === String(selectedVersionId)) ?? null;
  const versionLocked = Boolean(selectedVersion && selectedVersion.status !== "DRAFT");

  const visibleQuestions = useMemo(() => {
    if (scopeFilter === "ALL") return questions;
    return questions.filter((q) => (q.scope ?? "COMMON") === scopeFilter);
  }, [questions, scopeFilter]);

  return (
    <div className="panel-stack">
      <div className="workspace-heading">
        <div>
          <p className="eyebrow">KURUMSAL SİSTEM YÖNETİMİ</p>
          <h1>Yönetim Paneli</h1>
          <p className="muted">Departmanlar, kullanıcı rolleri, ortak/özel sorular, sürümler ve QR erişim bağlantıları</p>
        </div>
      </div>

      <div className="subtab-row" role="tablist" aria-label="Yönetim bölümleri">
        {ADMIN_SUBTABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={subtab === id}
            className={subtab === id ? "subtab active" : "subtab"}
            onClick={() => {
              setSubtab(id);
              setNotice("");
              setError("");
            }}
          >
            <Icon size={15} /> {label}
          </button>
        ))}
      </div>

      <SuccessBanner message={notice} />

      {subtab === "departments" && (
        <div className="panel-stack">
          <form
            className="card admin-card"
            onSubmit={(e) => {
              e.preventDefault();
              runAction(async () => {
                await api("/api/admin/departments", { method: "POST", body: { name: departmentName } });
                setDepartmentName("");
                setNotice("Yeni departman oluşturuldu.");
              });
            }}
          >
            <h2>Yeni Departman Ekle</h2>
            <div className="inline-form-row">
              <input
                aria-label="Birim adı"
                value={departmentName}
                onChange={(e) => setDepartmentName(e.target.value)}
                maxLength={100}
                required
                placeholder="Örn. Acil Servis, Poliklinik, Radyoloji"
              />
              <button className="button button-primary" disabled={busy}>
                <Plus size={16} /> Departman ekle
              </button>
            </div>
          </form>

          <section className="card admin-card">
            <div className="section-head">
              <h2>
                Mevcut Departmanlar ({departments.length})
              </h2>
              <span className="muted">
                {activeDepartments.length} aktif · {departments.length - activeDepartments.length} pasif
              </span>
            </div>
            <InfoBanner message="Varsayılan hastane birimleri kullanıma hazır gelir. İstediğiniz birimin adını düzenleyebilir, pasifleştirebilir, yeni birim ekleyebilir veya henüz kullanılmamış birimleri silebilirsiniz." />
            {departments.length === 0 ? (
              <EmptyState title="Departman yok" text="İlk hastane birimini yukarıdan ekleyin." />
            ) : (
              departments.map((dept) => (
                <CatalogEntityRow
                  key={dept.id}
                  item={dept}
                  onSave={(name) =>
                    runAction(async () => {
                      await api(`/api/admin/departments/${dept.id}`, { method: "PUT", body: { name } });
                      setNotice("Departman güncellendi.");
                    })
                  }
                  onToggle={() =>
                    runAction(async () => {
                      await api(`/api/admin/departments/${dept.id}/active?active=${!dept.active}`, {
                        method: "PATCH",
                      });
                      setNotice("Departman durumu güncellendi.");
                    })
                  }
                  onDelete={() =>
                    runAction(async () => {
                      await api(`/api/admin/departments/${dept.id}`, { method: "DELETE" });
                      setNotice(`"${dept.name}" departmanı silindi.`);
                    })
                  }
                />
              ))
            )}
          </section>
        </div>
      )}

      {subtab === "users" && (
        <div className="panel-stack">
          <form
            className="card admin-card"
            onSubmit={(e) => {
              e.preventDefault();
              runAction(async () => {
                await api("/api/admin/users", {
                  method: "POST",
                  body: {
                    username: newUsername.trim(),
                    password: newPassword,
                    fullName: newFullName.trim(),
                    role: newRole,
                    departmentId: newRole === "STAFF" ? Number(newUserDeptId) : null,
                  },
                });
                setNewUsername("");
                setNewPassword("");
                setNewFullName("");
                setNotice("Yeni kullanıcı oluşturuldu.");
              });
            }}
          >
            <h2>Yeni Kullanıcı Oluştur</h2>
            <div className="form-grid-3">
              <div>
                <label htmlFor="u-username">Kullanıcı adı</label>
                <input
                  id="u-username"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  minLength={3}
                  maxLength={100}
                  required
                  placeholder="ornek.personel"
                />
              </div>
              <div>
                <label htmlFor="u-fullname">Ad Soyad</label>
                <input
                  id="u-fullname"
                  value={newFullName}
                  onChange={(e) => setNewFullName(e.target.value)}
                  maxLength={120}
                  required
                  placeholder="Dr. Ayşe Yılmaz"
                />
              </div>
              <div>
                <label htmlFor="u-password">Parola (min 8 karakter)</label>
                <input
                  id="u-password"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  minLength={8}
                  maxLength={120}
                  required
                  placeholder="Güçlü parola"
                />
              </div>
              <div>
                <label htmlFor="u-role">Rol</label>
                <select id="u-role" value={newRole} onChange={(e) => setNewRole(e.target.value)}>
                  <option value="STAFF">Birim Personeli (STAFF)</option>
                  <option value="PATIENT_RIGHTS_OFFICER">Hasta Hakları (PATIENT_RIGHTS_OFFICER)</option>
                  <option value="ADMIN">Sistem Yöneticisi (ADMIN)</option>
                </select>
              </div>
              {newRole === "STAFF" && (
                <div>
                  <label htmlFor="u-dept">Bağlı Birim</label>
                  <select
                    id="u-dept"
                    value={newUserDeptId}
                    onChange={(e) => setNewUserDeptId(e.target.value)}
                    required
                  >
                    <option value="">Birim seçin</option>
                    {activeDepartments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            <button
              className="button button-primary"
              disabled={busy || (newRole === "STAFF" && !newUserDeptId)}
            >
              <Plus size={16} /> Kullanıcı oluştur
            </button>
          </form>

          <section className="card admin-card">
            <h2>Kayıtlı Kullanıcılar ({users.length})</h2>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Kullanıcı</th>
                    <th>Ad Soyad</th>
                    <th>Rol</th>
                    <th>Departman</th>
                    <th>Durum</th>
                    <th>İşlem</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <code>{u.username}</code>
                      </td>
                      <td>{u.fullName}</td>
                      <td>{roleLabel(u.role)}</td>
                      <td>{u.departmentName ?? "Tüm birimler"}</td>
                      <td>
                        <span className={u.active ? "entity-status active" : "entity-status"}>
                          {u.active ? "Aktif" : "Pasif"}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="button button-outline compact"
                          disabled={busy}
                          onClick={() =>
                            runAction(async () => {
                              await api(`/api/admin/users/${u.id}/active?active=${!u.active}`, {
                                method: "PATCH",
                              });
                              setNotice("Kullanıcı durumu güncellendi.");
                            })
                          }
                        >
                          {u.active ? "Pasifleştir" : "Etkinleştir"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}

      {subtab === "surveys" && (
        <div className="panel-stack">
          <InfoBanner message="Hüma Genel Hasta Memnuniyet Anketi varsayılan sorularıyla yayında gelir. İsterseniz doğrudan aşağıdan birim erişim bağlantısı üretip kullanın, isterseniz 'Anket Sürümleri' sekmesinden taslak oluşturup soruları özelleştirin ya da sıfırdan yeni anket oluşturun." />
          <div className="dashboard-grid">
            <form
              className="card admin-card"
              onSubmit={(e) => {
                e.preventDefault();
                runAction(async () => {
                  await api("/api/admin/surveys", { method: "POST", body: { title: surveyTitle } });
                  setSurveyTitle("");
                  setNotice("Yeni anket oluşturuldu.");
                });
              }}
            >
              <h2>Yeni Anket Oluştur</h2>
              <label htmlFor="survey-title">Anket başlığı</label>
              <input
                id="survey-title"
                value={surveyTitle}
                onChange={(e) => setSurveyTitle(e.target.value)}
                maxLength={150}
                required
                placeholder="Örn. Genel Hasta Memnuniyet Anketi"
              />
              <button className="button button-primary" disabled={busy}>
                <Plus size={16} /> Anket oluştur
              </button>
            </form>

            <form
              className="card admin-card"
              onSubmit={(e) => {
                e.preventDefault();
                runAction(async () => {
                  const result = await api("/api/admin/survey-accesses", {
                    method: "POST",
                    body: { surveyId: Number(surveyId), departmentId: Number(accessDepartmentId) },
                  });
                  setPublicToken(result.publicAccessToken);
                  await loadAccesses(accessDepartmentId);
                  setNotice("Erişim bağlantısı üretildi. Güvenlik gereği token yalnızca bir kez gösterilir.");
                });
              }}
            >
              <h2>Birim QR / Erişim Bağlantısı Üret</h2>
              <label htmlFor="access-survey">Anket</label>
              <select
                id="access-survey"
                value={surveyId}
                onChange={(e) => setSurveyId(e.target.value)}
                required
              >
                {surveys.map((s) => (
                  <option value={s.id} key={s.id}>
                    {s.title}
                  </option>
                ))}
              </select>
              <label htmlFor="access-department">Birim</label>
              <select
                id="access-department"
                value={accessDepartmentId}
                onChange={(e) => setAccessDepartmentId(e.target.value)}
                required
              >
                {activeDepartments.map((d) => (
                  <option value={d.id} key={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
              <button className="button button-primary" disabled={busy || !surveyId || !accessDepartmentId}>
                <KeyRound size={16} /> Bağlantı & token üret
              </button>
              {publicToken && (
                <div className="token-reveal">
                  <small className="muted">QR kod olarak paylaşılacak anket bağlantısı:</small>
                  <CopyValue value={`${window.location.origin}/?access=${encodeURIComponent(publicToken)}`} />
                  <a
                    className="button button-outline compact test-link-btn"
                    href={`${window.location.origin}/?access=${encodeURIComponent(publicToken)}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <ExternalLink size={14} /> Anketi yeni sekmede test et
                  </a>
                </div>
              )}
            </form>
          </div>

          <section className="card admin-card">
            <h2>Mevcut Anketler</h2>
            {surveys.map((s) => (
              <CatalogEntityRow
                key={s.id}
                item={s}
                onSave={(title) =>
                  runAction(async () => {
                    await api(`/api/admin/surveys/${s.id}`, { method: "PUT", body: { title } });
                    setNotice("Anket güncellendi.");
                  })
                }
                onToggle={() =>
                  runAction(async () => {
                    await api(`/api/admin/surveys/${s.id}/active?active=${!s.active}`, { method: "PATCH" });
                    setNotice("Anket durumu güncellendi.");
                  })
                }
              />
            ))}
          </section>

          <section className="card admin-card">
            <h2>Seçili Birimin Erişim Kayıtları</h2>
            {accesses.length === 0 ? (
              <p className="muted">Bu birim için henüz erişim bağlantısı oluşturulmamış.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Anket</th>
                      <th>Birim</th>
                      <th>Oluşturulma</th>
                      <th>Durum</th>
                      <th>İşlem</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accesses.map((a) => (
                      <tr key={a.id}>
                        <td>#{a.id}</td>
                        <td>{a.surveyTitle}</td>
                        <td>{a.departmentName}</td>
                        <td>{displayDate(a.createdAt)}</td>
                        <td>
                          <span className={a.active ? "entity-status active" : "entity-status"}>
                            {a.active ? "Aktif" : "Pasif"}
                          </span>
                        </td>
                        <td>
                          {a.active && (
                            <button
                              type="button"
                              className="button button-outline compact"
                              disabled={busy}
                              onClick={() =>
                                runAction(async () => {
                                  await api(`/api/admin/survey-accesses/${a.id}/deactivate`, { method: "PATCH" });
                                  await loadAccesses(accessDepartmentId);
                                  setNotice("Erişim bağlantısı pasifleştirildi.");
                                })
                              }
                            >
                              İptal et
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}

      {subtab === "questions" && (
        <div className="panel-stack">
          <div className="card filter-bar compact-filters">
            <div className="filter-field">
              <label htmlFor="q-survey-select">Anket</label>
              <select id="q-survey-select" value={surveyId} onChange={(e) => setSurveyId(e.target.value)}>
                {surveys.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.title}
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="q-version-select">Sürüm</label>
              <select
                id="q-version-select"
                value={selectedVersionId}
                onChange={(e) => setSelectedVersionId(e.target.value)}
              >
                <option value="">Tüm sürümler / temel</option>
                {versions.map((v) => (
                  <option key={v.id} value={v.id}>
                    Versiyon v{v.versionNumber} ({v.status})
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="q-scope-filter">Kapsam</label>
              <select id="q-scope-filter" value={scopeFilter} onChange={(e) => setScopeFilter(e.target.value)}>
                <option value="ALL">Tümü</option>
                <option value="COMMON">Ortak (COMMON)</option>
                <option value="DEPARTMENT_SPECIFIC">Departmana Özel</option>
              </select>
            </div>
          </div>

          {versionLocked && (
            <InfoBanner
              message="Seçili sürüm yayımlanmış veya arşivlenmiş olduğu için doğrudan değiştirilemez. Değişiklik yapmak için 'Anket Sürümleri' sekmesinden yeni bir taslak sürüm oluşturun."
            />
          )}

          {!versionLocked && (
            <form
              className="card admin-card"
              onSubmit={(e) => {
                e.preventDefault();
                runAction(async () => {
                  const payload = {
                    text: questionText,
                    type: questionType,
                    scope: questionScope,
                    departmentId: questionScope === "DEPARTMENT_SPECIFIC" ? Number(questionDepartmentId) : null,
                    metricCode: metricCode.trim() || null,
                    displayOrder: Number(questionOrder) || questions.length + 1,
                    required: questionRequired,
                    surveyVersionId: selectedVersionId ? Number(selectedVersionId) : null,
                  };
                  await api(`/api/admin/surveys/${surveyId}/questions`, { method: "POST", body: payload });
                  setQuestionText("");
                  setMetricCode("");
                  await loadSurveyDetails(surveyId, selectedVersionId || null);
                  setNotice("Soru eklendi.");
                });
              }}
            >
              <h2>Yeni Soru Ekle</h2>
              <div className="form-grid-3">
                <div>
                  <label htmlFor="question-scope">Soru kapsamı</label>
                  <select
                    id="question-scope"
                    value={questionScope}
                    onChange={(e) => setQuestionScope(e.target.value)}
                  >
                    <option value="COMMON">COMMON — Tüm birimler</option>
                    <option value="DEPARTMENT_SPECIFIC">DEPARTMENT_SPECIFIC — Birime özel</option>
                  </select>
                </div>

                {questionScope === "DEPARTMENT_SPECIFIC" && (
                  <div>
                    <label htmlFor="question-department">Departman</label>
                    <select
                      id="question-department"
                      value={questionDepartmentId}
                      onChange={(e) => setQuestionDepartmentId(e.target.value)}
                      required
                    >
                      {activeDepartments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div>
                  <label htmlFor="question-type">Yanıt türü</label>
                  <select
                    id="question-type"
                    value={questionType}
                    onChange={(e) => {
                      setQuestionType(e.target.value);
                      setQuestionRequired(e.target.value === "RATING");
                    }}
                  >
                    <option value="RATING">Puanlama (1–5)</option>
                    <option value="TEXT">Yorum metni (max 500)</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="metric-code">Metrik kodu</label>
                  <input
                    id="metric-code"
                    list="metric-suggestions"
                    value={metricCode}
                    onChange={(e) => setMetricCode(e.target.value)}
                    maxLength={100}
                    placeholder="Örn. WAITING_TIME"
                  />
                  <datalist id="metric-suggestions">
                    {METRIC_SUGGESTIONS.map((m) => (
                      <option key={m} value={m} />
                    ))}
                  </datalist>
                </div>

                <div>
                  <label htmlFor="question-order">Sıra no</label>
                  <input
                    id="question-order"
                    type="number"
                    min={1}
                    value={questionOrder}
                    onChange={(e) => setQuestionOrder(Number(e.target.value))}
                  />
                </div>

                <label className="toggle-control checkbox-align">
                  <input
                    type="checkbox"
                    checked={questionRequired}
                    onChange={(e) => setQuestionRequired(e.target.checked)}
                  />
                  Zorunlu soru
                </label>
              </div>

              <label htmlFor="question-text">Soru metni</label>
              <input
                id="question-text"
                value={questionText}
                onChange={(e) => setQuestionText(e.target.value)}
                maxLength={500}
                required
                placeholder="Hasta dostu, açık ve anlaşılır soru metnini yazın"
              />
              <button
                className="button button-primary"
                disabled={busy || !surveyId || (questionScope === "DEPARTMENT_SPECIFIC" && !questionDepartmentId)}
              >
                <Plus size={16} /> Soru ekle
              </button>
            </form>
          )}

          <section className="card admin-card">
            <div className="section-head">
              <h2>Sorular ({visibleQuestions.length})</h2>
              {versionLocked && (
                <span className="pill-warning">
                  <Lock size={13} /> Salt okunur sürüm
                </span>
              )}
            </div>
            {visibleQuestions.length === 0 ? (
              <EmptyState title="Soru bulunamadı" text="Seçilen sürüm veya kapsamda henüz soru yok." />
            ) : (
              visibleQuestions.map((question) => (
                <QuestionManagementRow
                  key={question.id}
                  question={question}
                  locked={versionLocked}
                  onSave={async (update) => {
                    await api(`/api/admin/questions/${question.id}`, { method: "PUT", body: update });
                    await loadSurveyDetails(surveyId, selectedVersionId || null);
                    setNotice("Soru güncellendi.");
                  }}
                  onDeactivate={async () => {
                    await api(`/api/admin/questions/${question.id}/deactivate`, { method: "PATCH" });
                    await loadSurveyDetails(surveyId, selectedVersionId || null);
                    setNotice("Soru pasifleştirildi.");
                  }}
                />
              ))
            )}
          </section>
        </div>
      )}

      {subtab === "versions" && (
        <section className="card admin-card">
          <div className="section-head">
            <div>
              <h2>Anket Sürümleri</h2>
              <p className="muted">
                Yeni taslak sürüm oluşturduğunuzda mevcut yayındaki aktif sorular otomatik kopyalanır; eski anket
                sonuçları kendi sürümünde korunur.
              </p>
            </div>
            <button
              type="button"
              className="button button-primary"
              disabled={!surveyId || busy}
              onClick={() =>
                runAction(async () => {
                  const created = await api(`/api/admin/surveys/${surveyId}/versions`, { method: "POST" });
                  await loadSurveyDetails(surveyId, created.id);
                  setNotice(`Taslak sürüm v${created.versionNumber} oluşturuldu.`);
                })
              }
            >
              <Plus size={16} /> Taslak sürüm oluştur
            </button>
          </div>

          <div className="filter-field max-320">
            <label htmlFor="v-survey-select">Anket seçin</label>
            <select id="v-survey-select" value={surveyId} onChange={(e) => setSurveyId(e.target.value)}>
              {surveys.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.title}
                </option>
              ))}
            </select>
          </div>

          {versions.length === 0 ? (
            <EmptyState title="Sürüm yok" text="Seçili anket için ilk taslak sürümü oluşturun." />
          ) : (
            <div className="version-list">
              {versions.map((version) => (
                <article className="version-row" key={version.id}>
                  <div>
                    <strong>Versiyon v{version.versionNumber}</strong>
                    <small className="muted">
                      Oluşturulma: {displayDate(version.createdAt)}
                      {version.publishedAt ? ` · Yayım: ${displayDate(version.publishedAt)}` : ""}
                    </small>
                  </div>
                  <StatusBadge status={version.status} />
                  <div className="version-actions">
                    <button
                      type="button"
                      className="button button-outline compact"
                      onClick={() => {
                        setSelectedVersionId(String(version.id));
                        setSubtab("questions");
                      }}
                    >
                      Soruları gör
                    </button>
                    {version.status === "DRAFT" && (
                      <button
                        type="button"
                        className="button button-primary compact"
                        disabled={busy}
                        onClick={() =>
                          runAction(async () => {
                            await api(`/api/admin/surveys/${surveyId}/versions/${version.id}/publish`, {
                              method: "PATCH",
                            });
                            await loadSurveyDetails(surveyId, version.id);
                            setNotice(`Versiyon v${version.versionNumber} yayımlandı.`);
                          })
                        }
                      >
                        Yayımla
                      </button>
                    )}
                    {version.status !== "ARCHIVED" && (
                      <button
                        type="button"
                        className="button button-outline compact"
                        disabled={busy}
                        onClick={() =>
                          runAction(async () => {
                            await api(`/api/admin/surveys/${surveyId}/versions/${version.id}/archive`, {
                              method: "PATCH",
                            });
                            await loadSurveyDetails(surveyId);
                            setNotice(`Versiyon v${version.versionNumber} arşivlendi.`);
                          })
                        }
                      >
                        Arşivle
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function CatalogEntityRow({ item, onSave, onToggle, onDelete }) {
  const [value, setValue] = useState(item.name ?? item.title);
  const fieldLabel = item.name ? "Birim adı" : "Anket başlığı";

  return (
    <form
      className={onDelete ? "catalog-edit-row with-delete" : "catalog-edit-row"}
      onSubmit={(event) => {
        event.preventDefault();
        onSave(value);
      }}
    >
      <input
        aria-label={fieldLabel}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        maxLength={item.name ? 100 : 150}
        required
      />
      <span className={item.active ? "entity-status active" : "entity-status"}>
        {item.active ? "Aktif" : "Pasif"}
      </span>
      <button className="button button-outline compact">Kaydet</button>
      <button type="button" className="button button-outline compact" onClick={onToggle}>
        {item.active ? "Pasifleştir" : "Etkinleştir"}
      </button>
      {onDelete && (
        <button
          type="button"
          className="button button-danger-outline compact"
          title="Departmanı sil"
          onClick={onDelete}
        >
          <Trash2 size={14} /> Sil
        </button>
      )}
    </form>
  );
}

function QuestionManagementRow({ question, locked, onSave, onDeactivate }) {
  const [text, setText] = useState(question.text);
  const [type, setType] = useState(question.type);
  const [displayOrder, setDisplayOrder] = useState(question.displayOrder);
  const [metricCode, setMetricCode] = useState(question.metricCode ?? "");
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    try {
      await onSave({
        text,
        type,
        displayOrder: Number(displayOrder) || question.displayOrder,
        required: question.required,
        active: question.active,
        scope: question.scope ?? "COMMON",
        departmentId: question.departmentId ?? null,
        metricCode: metricCode.trim() || question.metricCode,
      });
    } finally {
      setBusy(false);
    }
  }

  async function deactivate() {
    setBusy(true);
    try {
      await onDeactivate();
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className={question.active ? "question-edit-row" : "question-edit-row inactive"}>
      <div className="question-edit-main">
        <div className="question-meta-chips">
          <span className="scope-chip">
            {question.scope === "DEPARTMENT_SPECIFIC"
              ? `Özel: ${question.departmentName ?? `Birim #${question.departmentId}`}`
              : "Ortak (COMMON)"}
          </span>
          <input
            aria-label={`Metrik ${question.id}`}
            className="metric-inline-input"
            value={metricCode}
            disabled={locked}
            onChange={(e) => setMetricCode(e.target.value)}
            placeholder="METRIC_CODE"
          />
          <input
            aria-label={`Sıra ${question.id}`}
            type="number"
            className="order-inline-input"
            value={displayOrder}
            disabled={locked}
            onChange={(e) => setDisplayOrder(e.target.value)}
          />
        </div>
        <input
          aria-label={`Soru ${question.id}`}
          value={text}
          disabled={locked}
          onChange={(event) => setText(event.target.value)}
          maxLength={500}
        />
      </div>
      <select
        aria-label={`Soru tipi ${question.id}`}
        value={type}
        disabled={locked}
        onChange={(event) => setType(event.target.value)}
      >
        <option value="RATING">RATING</option>
        <option value="TEXT">TEXT</option>
      </select>
      {!locked && (
        <>
          <button type="button" className="button button-outline compact" onClick={save} disabled={busy}>
            Kaydet
          </button>
          {question.active && (
            <button type="button" className="button button-outline compact" onClick={deactivate} disabled={busy}>
              Pasifleştir
            </button>
          )}
        </>
      )}
    </div>
  );
}
