import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { SurveyView } from "./components/SurveyView.jsx";
import { roleLabel, statusLabel, scoreTone } from "./utils/format.js";

describe("format utilities", () => {
  it("formats roles, statuses, and score tones consistently", () => {
    expect(roleLabel("ADMIN")).toBe("Sistem Yöneticisi");
    expect(roleLabel("STAFF")).toBe("Birim Personeli");
    expect(roleLabel("PATIENT_RIGHTS_OFFICER")).toBe("Hasta Hakları Birimi");
    expect(statusLabel("APPROVED")).toBe("Onaylandı");
    expect(statusLabel("REVISION_REQUIRED")).toBe("Revizyon İstendi");
    expect(scoreTone(1.5)).toBe("critical");
    expect(scoreTone(2.8)).toBe("warning");
    expect(scoreTone(4.5)).toBe("good");
  });
});

describe("SurveyView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders department info and groups COMMON and DEPARTMENT_SPECIFIC questions", async () => {
    global.fetch = vi.fn((url) => {
      if (String(url).endsWith("/questions")) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () =>
            Promise.resolve([
              {
                id: 1,
                surveyId: 10,
                text: "Personelin iletişiminden memnun kaldınız mı?",
                type: "RATING",
                scope: "COMMON",
                displayOrder: 10,
                required: true,
                active: true,
              },
              {
                id: 2,
                surveyId: 10,
                text: "İlk müdahale süresi beklentilerinizi karşıladı mı?",
                type: "RATING",
                scope: "DEPARTMENT_SPECIFIC",
                departmentId: 4,
                displayOrder: 20,
                required: true,
                active: true,
              },
            ]),
        });
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () =>
          Promise.resolve({
            surveyId: 10,
            surveyTitle: "Genel Hasta Memnuniyet Anketi",
            departmentId: 4,
            departmentName: "Acil Servis",
            surveyVersionNumber: 1,
            questionCount: 2,
          }),
      });
    });

    render(<SurveyView accessToken="demo-token" setAccessToken={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText("Acil Servis")).toBeInTheDocument();
      expect(screen.getByText("GENEL DEĞERLENDİRME SORULARI")).toBeInTheDocument();
      expect(screen.getByText("BİRİME ÖZEL SORULAR")).toBeInTheDocument();
      expect(screen.getByText(/Personelin iletişiminden memnun kaldınız mı/)).toBeInTheDocument();
      expect(screen.getByText(/İlk müdahale süresi beklentilerinizi karşıladı mı/)).toBeInTheDocument();
    });
  });
});
