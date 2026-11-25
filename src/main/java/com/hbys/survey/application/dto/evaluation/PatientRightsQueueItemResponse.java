package com.hbys.survey.application.dto.evaluation;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.SatisfactionLevel;

import java.math.BigDecimal;
import java.time.Instant;

public record PatientRightsQueueItemResponse(
        Long submissionId,
        Long departmentId,
        String departmentName,
        BigDecimal overallScore,
        Instant submissionCreatedAt,
        Long feedbackId,
        String feedbackComment,
        String feedbackAuthor,
        Instant feedbackCreatedAt,
        PatientFeedbackEvaluationStatus evaluationStatus,
        String evaluationNotes,
        String evaluatedByName,
        Instant evaluationCreatedAt,
        SatisfactionLevel patientSatisfaction,
        Instant patientSatisfactionCreatedAt
) {
}
