package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class SubmissionSummaryResponse {

    private Long submissionId;
    private Long departmentId;
    private String departmentName;
    private BigDecimal overallScore;
    private Instant createdAt;
    private PatientFeedbackEvaluationStatus evaluationStatus;

    public SubmissionSummaryResponse() {
    }

    public SubmissionSummaryResponse(
            Long submissionId,
            Long departmentId,
            String departmentName,
            BigDecimal overallScore,
            Instant createdAt,
            PatientFeedbackEvaluationStatus evaluationStatus
    ) {
        this.submissionId = submissionId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.overallScore = overallScore;
        this.createdAt = createdAt;
        this.evaluationStatus = evaluationStatus;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PatientFeedbackEvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(PatientFeedbackEvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }
}