package com.hbys.survey.infrastructure.ai;

import com.hbys.survey.application.exception.AIServiceException;

/**
 * Treats model output as untrusted external content.
 */
final class AiOutputSanitizer {

    private static final int MAX_LENGTH = 5000;

    private AiOutputSanitizer() {
    }

    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AIServiceException("AI provider returned an empty summary.");
        }
        String cleaned = raw
                .replace('\u0000', ' ')
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (cleaned.isBlank()) {
            throw new AIServiceException("AI provider returned unusable summary content.");
        }
        if (cleaned.length() > MAX_LENGTH) {
            return cleaned.substring(0, MAX_LENGTH);
        }
        return cleaned;
    }
}
