import { expect, test } from "@playwright/test";

test("admin provisions a survey, anonymous patient submits, staff draft is approved and patient rates it", async ({ page }) => {
  const unique = Date.now();
  const initialDeptName = `E2E Emergency ${unique}`;
  const updatedDeptName = `E2E Emergency Updated ${unique}`;
  const initialSurveyTitle = `E2E Visit ${unique}`;
  const updatedSurveyTitle = `E2E Visit Updated ${unique}`;

  await page.goto("/");
  await page.getByRole("button", { name: "Personel" }).click();
  await page.getByLabel("Kullanıcı adı").fill("e2e-admin");
  await page.getByLabel("Parola").fill("e2e-local-password-25");
  await page.getByRole("button", { name: "Güvenli giriş" }).click();

  await page.getByRole("button", { name: "Yönetim" }).click();

  // 1. Departmanlar subtab: create & update department
  await page.getByPlaceholder("Örn. Acil Servis, Poliklinik, Radyoloji").fill(initialDeptName);
  await page.getByRole("button", { name: "Departman ekle" }).click();
  await expect(page.getByText("Yeni departman oluşturuldu.")).toBeVisible();

  const deptRow = page.locator(".catalog-edit-row").first();
  await deptRow.locator("input").fill(updatedDeptName);
  await deptRow.getByRole("button", { name: "Kaydet" }).click();
  await expect(page.getByText("Departman güncellendi.")).toBeVisible();

  // 2. Anketler & QR Erişim subtab: create & update survey
  await page.getByRole("tab", { name: "Anketler & QR Erişim" }).click();
  await page.locator("#survey-title").fill(initialSurveyTitle);
  await page.getByRole("button", { name: "Anket oluştur" }).click();
  await expect(page.getByText("Yeni anket oluşturuldu.")).toBeVisible();

  const surveyRow = page.locator(".catalog-edit-row").first();
  await surveyRow.locator("input").fill(updatedSurveyTitle);
  await surveyRow.getByRole("button", { name: "Kaydet" }).click();
  await expect(page.getByText("Anket güncellendi.")).toBeVisible();

  // 3. Sorular & Metrikler subtab: add RATING + TEXT questions and update the first question
  await page.getByRole("tab", { name: "Sorular & Metrikler" }).click();
  await page.getByLabel("Soru metni").fill("How was your visit?");
  await page.getByRole("button", { name: "Soru ekle" }).click();
  await expect(page.locator(".question-edit-row")).toHaveCount(1);

  await page.getByLabel("Yanıt türü").selectOption("TEXT");
  await page.getByLabel("Soru metni").fill("Anything else to share?");
  await page.getByRole("button", { name: "Soru ekle" }).click();
  await expect(page.locator(".question-edit-row")).toHaveCount(2);

  const firstQuestionRow = page.locator(".question-edit-row").first();
  await firstQuestionRow.locator('input[aria-label^="Soru "]').fill("How was your visit? Updated");
  await firstQuestionRow.getByRole("button", { name: "Kaydet" }).click();
  await expect(page.getByText("Soru güncellendi.")).toBeVisible();

  // 4. Generate QR / access link for the survey + department
  await page.getByRole("tab", { name: "Anketler & QR Erişim" }).click();
  const accessResponse = page.waitForResponse(
    (response) => response.url().includes("/api/admin/survey-accesses") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Bağlantı & token üret" }).click();
  const accessResult = await accessResponse;
  if (accessResult.status() !== 201) {
    throw new Error(`Survey access creation failed (${accessResult.status()}): ${await accessResult.text()}`);
  }

  await expect(page.locator(".copy-field code")).toBeVisible({ timeout: 10_000 });
  const accessUrl = await page.locator(".copy-field code").innerText();
  expect(accessUrl).toContain("?access=");

  // 5. Open public survey URL as anonymous patient and submit low rating
  await page.goto(accessUrl);
  await expect(page.getByRole("heading", { name: "Ziyaretiniz nasıldı?" })).toBeVisible();
  await page
    .getByRole("radiogroup", { name: "How was your visit? Updated" })
    .locator("label.rating-option")
    .filter({ hasText: "2" })
    .click();
  await page.getByRole("button", { name: "Yanıtı gönder" }).click();

  await expect(page.locator(".copy-field code")).toBeVisible({ timeout: 10_000 });
  const trackingNumber = await page.locator(".copy-field code").innerText();
  expect(trackingNumber).toMatch(/^TRK-/);

  // 6. Staff drafts response & Patient Rights approves in Sonuçlar
  await page.getByRole("button", { name: "Personel" }).click();
  await page.getByRole("button", { name: "Sonuçlar" }).click();
  await page.getByRole("button", { name: new RegExp(updatedDeptName) }).click();
  await page.getByLabel("Yanıt taslağı").fill("We have shared your feedback with the department.");
  await page.getByRole("button", { name: "Taslağı kaydet" }).click();
  await expect(page.getByText("Geri bildirim taslağı oluşturuldu.")).toBeVisible();

  await page.getByLabel("İç değerlendirme notu").fill("Reviewed by the patient rights office.");
  await page.getByRole("button", { name: "Kararı kaydet" }).click();
  await expect(page.getByText("Hasta Hakları kararı kaydedildi.")).toBeVisible();

  // 7. Patient tracks approved feedback and evaluates satisfaction once
  await page.getByRole("button", { name: "Takip" }).click();
  await page.getByLabel("Takip numarası").fill(trackingNumber);
  await page.getByRole("button", { name: "Durumu görüntüle" }).click();
  await expect(page.getByText("We have shared your feedback with the department.")).toBeVisible();
  await page.getByRole("button", { name: "Evet, teşekkürler" }).click();
  await expect(page.getByText("Değerlendirmeniz kaydedildi.")).toBeVisible();
});
