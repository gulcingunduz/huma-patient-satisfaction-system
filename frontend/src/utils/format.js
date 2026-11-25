export const ISTANBUL_TZ = "Europe/Istanbul";

export function displayDate(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("tr-TR", {
    timeZone: ISTANBUL_TZ,
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function displayShortDate(isoDate) {
  if (!isoDate) return "-";
  const parts = String(isoDate).split("-");
  if (parts.length === 3) return `${parts[2]}.${parts[1]}.${parts[0]}`;
  return new Intl.DateTimeFormat("tr-TR", {
    timeZone: ISTANBUL_TZ,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(isoDate));
}

export function todayIsoDate() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: ISTANBUL_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

export function daysAgoIsoDate(days) {
  const date = new Date(Date.now() - days * 86_400_000);
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: ISTANBUL_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function roleLabel(role) {
  return (
    {
      ADMIN: "Sistem Yöneticisi",
      STAFF: "Birim Personeli",
      PATIENT_RIGHTS_OFFICER: "Hasta Hakları Birimi",
    }[role] ?? role
  );
}

export function statusLabel(status) {
  return (
    {
      APPROVED: "Onaylandı",
      REJECTED: "Reddedildi",
      REVISION_REQUIRED: "Revizyon İstendi",
      PENDING: "İncelemede",
      COMPLETED: "Tamamlandı",
      FAILED: "Başarısız",
      DRAFT: "Taslak",
      PUBLISHED: "Yayında",
      ARCHIVED: "Arşivlendi",
    }[status] ?? status ?? "Beklemede"
  );
}

export function satisfactionLabel(level) {
  return (
    {
      SATISFIED: "Memnun Kaldım",
      NOT_SATISFIED: "Memnun Kalmadım",
    }[level] ?? "Henüz değerlendirilmedi"
  );
}

export function scoreTone(score) {
  const numeric = Number(score);
  if (Number.isNaN(numeric) || numeric === 0) return "neutral";
  if (numeric <= 2) return "critical";
  if (numeric <= 3) return "warning";
  return "good";
}
