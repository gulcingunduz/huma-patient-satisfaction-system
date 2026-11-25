package com.hbys.survey.application.dto.survey;

import java.time.Instant;

public record SurveyAccessResponseDto(
        Long id,
        Long surveyId,
        String surveyTitle,
        Long departmentId,
        String departmentName,
        boolean active,
        Instant createdAt,
        String publicAccessToken
) {
}