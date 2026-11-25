import { expect, test } from "@playwright/test";

test("admin provisions a survey, anonymous patient submits, staff draft is approved and patient rates it", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Personel" }).click();
  await page.getByLabel("Kullanıcı adı").fill("e2e-admin");
  await page.getByLabel("Parola").fill("e2e-local-password-25");
  await page.getByRole("button", { name: "Güvenli giriş" }).click();

  await page.locator("#department-name").fill(`E2E Emergency ${Date.now()}`);
  await page.getByRole("button", { name: "Birim oluştur" }).click();
  await page.locator("#survey-title").fill(`E2E Visit ${Date.now()}`);
  await page.getByRole("button", { name: "Anket oluştur" }).click();

  const departmentField = page.locator(".catalog-edit-row input").nth(0);
  await departmentField.fill(`E2E Emergency Updated ${Date.now()}`);
  await page.locator(".catalog-edit-row").nth(0).getByRole("button", { name: "Kaydet" }).click();
  await page.locator(".catalog-edit-row input").nth(1).fill(`E2E Visit Updated ${Date.now()}`);
  await page.locator(".catalog-edit-row").nth(1).getByRole("button", { name: "Kaydet" }).click();

  await page.getByLabel("Soru metni").fill("How was your visit?");
  await page.getByRole("button", { name: "Soru ekle" }).click();
  await page.getByLabel("Yanıt türü").selectOption("TEXT");
  await page.getByLabel("Soru metni").fill("Anything else to share?");
  await page.getByRole("button", { name: "Soru ekle" }).click();
  await page.locator(".question-edit-row").nth(0).locator("input").fill("How was your visit? Updated");
  await page.locator(".question-edit-row").nth(0).getByRole("button", { name: "Kaydet" }).click();
  const accessResponse = page.waitForResponse((response) => response.url().includes("/api/admin/survey-accesses") && response.request().method() === "POST");
  await page.getByRole("button", { name: "Token üret" }).click();
  const accessResult = await accessResponse;
  if (accessResult.status() !== 201) {
    throw new Error(`Survey access creation failed (${accessResult.status()}): ${await accessResult.text()}`);
  }

  await expect(page.locator(".copy-field code")).toBeVisible({ timeout: 10_000 });
  const accessUrl = await page.locator(".copy-field code").innerText();
  expect(accessUrl).toContain("?access=");
  await page.goto(accessUrl);
  await expect(page.getByRole("heading", { name: "Ziyaretiniz nasıldı?" })).toBeVisible();
  await page.getByRole("radiogroup", { name: "How was your visit? Updated" }).locator("label.rating-option").filter({ hasText: "2" }).click();
  await page.getByRole("button", { name: "Yanıtı gönder" }).click();
  const trackingNumber = await page.locator(".copy-field code").innerText();
  expect(trackingNumber).toMatch(/^TRK-/);

  await page.getByRole("button", { name: "Personel" }).click();
  await page.getByRole("button", { name: "Genel Bakış" }).click();
  await page.getByRole("button", { name: /E2E Emergency Updated/ }).click();
  await page.getByLabel("Yanıt taslağı").fill("We have shared your feedback with the department.");
  await page.getByRole("button", { name: "Taslağı kaydet" }).click();
  await page.getByLabel("İç not").fill("Reviewed by the patient rights office.");
  await page.getByRole("button", { name: "Kararı kaydet" }).click();

  await page.getByRole("button", { name: "Takip" }).click();
  await page.getByLabel("Takip numarası").fill(trackingNumber);
  await page.getByRole("button", { name: "Durumu görüntüle" }).click();
  await expect(page.getByText("We have shared your feedback with the department.")).toBeVisible();
  await page.getByRole("button", { name: "Evet, teşekkürler" }).click();
  await expect(page.getByText("Değerlendirmeniz kaydedildi.")).toBeVisible();
});