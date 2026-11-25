package com.hbys.survey.application.dto.survey;

import com.hbys.survey.domain.entity.SurveyVersionStatus;

import java.time.Instant;

public record SurveyVersionResponseDto(
        Long id,
        Long surveyId,
        Integer versionNumber,
        SurveyVersionStatus status,
        Instant createdAt,
        Instant publishedAt
) {
}
