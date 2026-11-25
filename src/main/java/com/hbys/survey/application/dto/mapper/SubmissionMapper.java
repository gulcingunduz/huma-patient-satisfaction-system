package com.hbys.survey.application.dto.mapper;

import com.hbys.survey.application.dto.submission.SurveySubmissionRequestDto;
import com.hbys.survey.application.dto.submission.SurveySubmissionResponse;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.domain.entity.SurveyVersion;

public final class SubmissionMapper {

    private SubmissionMapper() {
    }

    public static SurveySubmission toEntity(
            SurveySubmissionRequestDto dto,
            Survey survey,
            Department department,
            String clientTokenHash,
            String trackingTokenHash,
            java.math.BigDecimal overallScore,
            SurveyVersion surveyVersion
    ) {
        if (dto == null) {
            return null;
        }

        SurveySubmission submission = new SurveySubmission(
                survey,
                department,
                clientTokenHash,
                trackingTokenHash,
                overallScore
        );
        submission.setSurveyVersion(surveyVersion);
        return submission;
    }

    public static SurveySubmissionResponse toResponse(
            SurveySubmission submission,
            String trackingNumber
    ) {
        if (submission == null) {
            return null;
        }

        SurveySubmissionResponse response = new SurveySubmissionResponse(
                submission.getId(),
                trackingNumber,
                submission.getCreatedAt()
        );
        if (submission.getSurveyVersion() != null) {
            response.setSurveyVersionId(submission.getSurveyVersion().getId());
            response.setSurveyVersionNumber(submission.getSurveyVersion().getVersionNumber());
        }
        return response;
    }
}
