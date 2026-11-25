let csrfTokenPromise;

export async function getCsrfToken() {
  csrfTokenPromise ??= fetch("/api/auth/csrf", { credentials: "include" })
    .then(async (response) => {
      if (!response.ok) throw new Error("Güvenlik token’ı alınamadı.");
      return (await response.json()).token;
    })
    .catch((error) => {
      csrfTokenPromise = undefined;
      throw error;
    });
  return csrfTokenPromise;
}

export async function api(path, options = {}) {
  const method = options.method ?? "GET";
  const headers = new Headers(options.headers ?? {});
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (!["GET", "HEAD", "OPTIONS"].includes(method.toUpperCase())) {
    headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: "include",
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (response.status === 204) return null;
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && payload?.code === "SESSION_EXPIRED" && typeof window !== "undefined") {
      csrfTokenPromise = undefined;
      window.dispatchEvent(new CustomEvent("huma:session-expired", { detail: payload.message }));
    }
    const err = new Error(payload?.message ?? `Ağ isteği başarısız oldu (${response.status}).`);
    err.status = response.status;
    err.code = payload?.code;
    throw err;
  }
  return payload;
}
