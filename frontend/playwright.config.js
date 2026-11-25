import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:5173",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: "npm run dev -- --host 127.0.0.1",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: !process.env.CI,
      timeout: 30_000,
    },
    {
      command: "cd .. && mvn -q -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true -DskipTests spring-boot:run",
      url: "http://127.0.0.1:8080/api/auth/csrf",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
      env: {
        HBYS_ADMIN_USERNAME: "e2e-admin",
        HBYS_ADMIN_PASSWORD: "e2e-local-password-25",
      },
    },
  ],
});