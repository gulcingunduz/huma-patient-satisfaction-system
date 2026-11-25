package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.ai.PiiMasker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskerAndComplaintCategoryTest {

    private final ComplaintCategoryServiceImpl complaintCategoryService = new ComplaintCategoryServiceImpl();

    @Test
    void masksTurkishPiiPatternsInFreeTextComments() {
        String raw = "TC 12345678901, telefonum 0532 123 45 67 veya +905551112233, "
                + "mail: hasta@example.com, takip: TRK-abcdef-12345, "
                + "iban TR33 0006 1005 1978 6457 8413 26. Bekleme süresi uzundu.";

        String masked = PiiMasker.mask(raw);

        assertThat(masked)
                .doesNotContain("12345678901")
                .doesNotContain("0532 123 45 67")
                .doesNotContain("5551112233")
                .doesNotContain("hasta@example.com")
                .doesNotContain("TRK-abcdef-12345")
                .doesNotContain("TR33 0006")
                .contains("[TCKN GİZLENDİ]")
                .contains("[TELEFON GİZLENDİ]")
                .contains("[E-POSTA GİZLENDİ]")
                .contains("[TAKİP KODU GİZLENDİ]")
                .contains("[IBAN GİZLENDİ]")
                .contains("Bekleme süresi uzundu.");
    }

    @Test
    void separatesAppointmentAndWaitingComplaintsAndDetectsMultipleCategories() {
        assertThat(complaintCategoryService.categorize("Randevu almak çok zor oldu."))
                .isEqualTo("Randevu süreci");

        List<String> multiple = complaintCategoryService.categorizeAll(
                "Bekleme süresi çok uzundu, temizlik yetersizdi ve bilgilendirme yapılmadı."
        );
        assertThat(multiple).containsExactlyInAnyOrder(
                "Bekleme süresi",
                "Temizlik",
                "Bilgilendirme eksikliği"
        );
    }
}
