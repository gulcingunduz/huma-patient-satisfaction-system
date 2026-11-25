package com.hbys.survey.application.ai;

import java.util.regex.Pattern;

/**
 * Masks common Turkish PII patterns before free-text comments leave the application boundary
 * for AI summarization (KVKK data minimization).
 */
public final class PiiMasker {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IBAN_PATTERN =
            Pattern.compile("\\bTR\\d{2}(?:[\\s-]?\\d{4}){5}[\\s-]?\\d{2}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TURKISH_PHONE_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\+?90[\\s.-]?)?(?:0[\\s.-]?)?5\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{2}[\\s.-]?\\d{2}(?!\\d)");
    private static final Pattern LANDLINE_PHONE_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\+?90[\\s.-]?)?0?[2-4]\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{2}[\\s.-]?\\d{2}(?!\\d)");
    private static final Pattern TCKN_PATTERN =
            Pattern.compile("(?<!\\d)[1-9]\\d{10}(?!\\d)");
    private static final Pattern TRACKING_TOKEN_PATTERN =
            Pattern.compile("\\bTRK-[A-Za-z0-9-]{6,}\\b", Pattern.CASE_INSENSITIVE);

    private PiiMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String result = EMAIL_PATTERN.matcher(input).replaceAll("[E-POSTA GİZLENDİ]");
        result = IBAN_PATTERN.matcher(result).replaceAll("[IBAN GİZLENDİ]");
        result = TRACKING_TOKEN_PATTERN.matcher(result).replaceAll("[TAKİP KODU GİZLENDİ]");
        result = TURKISH_PHONE_PATTERN.matcher(result).replaceAll("[TELEFON GİZLENDİ]");
        result = LANDLINE_PHONE_PATTERN.matcher(result).replaceAll("[TELEFON GİZLENDİ]");
        result = TCKN_PATTERN.matcher(result).replaceAll("[TCKN GİZLENDİ]");
        return result.trim();
    }
}
