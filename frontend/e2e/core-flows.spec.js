import { expect, test } from "@playwright/test";

const errorResponse = (message = "Unauthorized") => ({ status: 401, contentType: "application/json", body: JSON.stringify({ status: 401, message, code: "UNAUTHORIZED" }) });

test("patient submits an anonymous rating and receives a tracking number", async ({ page }) => {
  await page.route("**/api/auth/me", (route) => route.fulfill(errorResponse()));
  await page.route("**/api/public/surveys/access/share-token/questions", (route) => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify([{ id: 9, surveyId: 3, text: "How was your visit?", type: "RATING", displayOrder: 1, required: true, active: true }]),
  }));
  await page.route("**/api/auth/csrf", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ token: "csrf" }) }));
  await page.route("**/api/public/surveys/share-token/submissions", async (route) => {
    const payload = route.request().postDataJSON();
    expect(payload).toEqual({ surveyId: 3, answers: [{ questionId: 9, score: 5, answerValue: "5" }] });
    await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ submissionId: 32, trackingNumber: "TRK-demo-32" }) });
  });

  await page.goto("/?access=share-token");
  await expect(page.getByRole("heading", { name: "Ziyaretiniz nasıldı?" })).toBeVisible();
  await page.locator("label.rating-option").filter({ hasText: "5" }).click();
  await page.getByRole("button", { name: "Yanıtı gönder" }).click();

  await expect(page.getByText("TRK-demo-32")).toBeVisible();
  await expect(page.getByText("YANIT KAYDEDİLDİ")).toBeVisible();
});

test("staff signs in, opens a submission and saves a response draft", async ({ page }) => {
  await page.route("**/api/auth/me", (route) => route.fulfill(errorResponse()));
  await page.route("**/api/auth/csrf", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ token: "csrf" }) }));
  await page.route("**/api/auth/login", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ username: "staff1", fullName: "Test Staff", role: "STAFF" }) }));
  await page.route("**/api/staff/submissions", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([{ submissionId: 7, overallScore: 2, departmentName: "Acil Servis", createdAt: "2026-10-01T10:00:00Z" }]) }));
  await page.route("**/api/staff/submissions/7", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ submissionId: 7, overallScore: 2, departmentName: "Acil Servis", createdAt: "2026-10-01T10:00:00Z", answers: [{ questionId: 1, questionText: "Bekleme süresi", score: 2 }] }) }));
  await page.route("**/api/staff/feedback", async (route) => {
    expect(route.request().postDataJSON()).toEqual({ submissionId: 7, comment: "Geri bildiriminiz inceleniyor." });
    await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ id: 4 }) });
  });

  await page.goto("/");
  await page.getByRole("button", { name: "Personel" }).click();
  await page.getByLabel("Kullanıcı adı").fill("staff1");
  await page.getByLabel("Parola").fill("password");
  await page.getByRole("button", { name: "Güvenli giriş" }).click();
  await page.getByRole("button", { name: /Acil Servis/ }).click();
  await page.getByLabel("Yanıt taslağı").fill("Geri bildiriminiz inceleniyor.");
  await page.getByRole("button", { name: "Taslağı kaydet" }).click();

  await expect(page.getByText("Geri bildirim taslağı oluşturuldu.")).toBeVisible();
  await expect(page.getByText("Bekleme süresi")).toBeVisible();
});

test("patient tracks an approved response and submits satisfaction once", async ({ page }) => {
  await page.route("**/api/auth/me", (route) => route.fulfill(errorResponse()));
  await page.route("**/api/public/surveys/tracking/TRK-demo-8", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ submissionId: 8, trackingNumber: "TRK-demo-8", status: "APPROVED", updatedAt: "2026-10-01T12:00:00Z" }) }));
  await page.route("**/api/public/surveys/tracking/TRK-demo-8/feedback", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ trackingNumber: "TRK-demo-8", comment: "Yaşadığınız sorun ilgili birime iletilmiştir." }) }));
  await page.route("**/api/auth/csrf", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ token: "csrf" }) }));
  await page.route("**/api/public/surveys/tracking/TRK-demo-8/feedback/satisfaction", async (route) => {
    expect(route.request().postDataJSON()).toEqual({ satisfied: "SATISFIED" });
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ satisfied: "SATISFIED" }) });
  });

  await page.goto("/");
  await page.getByRole("button", { name: "Takip" }).click();
  await page.getByLabel("Takip numarası").fill("TRK-demo-8");
  await page.getByRole("button", { name: "Durumu görüntüle" }).click();
  await expect(page.getByText("Yaşadığınız sorun ilgili birime iletilmiştir.")).toBeVisible();
  await page.getByRole("button", { name: "Evet, teşekkürler" }).click();

  await expect(page.getByText("Değerlendirmeniz kaydedildi.")).toBeVisible();
});