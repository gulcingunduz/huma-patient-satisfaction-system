import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "./api.js";

describe("API client", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("loads a CSRF token and sends it with credentialed mutations", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-value" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 8 }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await api("/api/admin/departments", {
      method: "POST",
      body: { name: "Emergency" },
    });

    expect(result).toEqual({ id: 8 });
    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/auth/csrf", { credentials: "include" });
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "POST",
      credentials: "include",
      body: JSON.stringify({ name: "Emergency" }),
    });
    expect(fetchMock.mock.calls[1][1].headers.get("X-XSRF-TOKEN")).toBe("csrf-value");
  });

  it("surfaces the backend error message", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ message: "Access denied." }), { status: 403 }),
    ));

    await expect(api("/api/admin/departments")).rejects.toThrow("Access denied.");
  });
});