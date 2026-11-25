package com.hbys.survey.application.dto.survey;

public record PublicSurveyInfoResponse(
        Long surveyId,
        String surveyTitle,
        Long departmentId,
        String departmentName,
        Integer surveyVersionNumber,
        int questionCount
) {
}
