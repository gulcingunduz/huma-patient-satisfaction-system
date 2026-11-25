package com.hbys.survey.application.dto.evaluation;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;

import java.time.Instant;

public class PatientFeedbackEvaluationResponse {

    private Long id;
    private Long submissionId;
    private Long evaluatedByStaffUserId;
    private PatientFeedbackEvaluationStatus status;
    private String notes;
    private Instant createdAt;

    public PatientFeedbackEvaluationResponse() {
    }

    public PatientFeedbackEvaluationResponse(Long id, Long submissionId, Long evaluatedByStaffUserId, PatientFeedbackEvaluationStatus status, String notes, Instant createdAt) {
        this.id = id;
        this.submissionId = submissionId;
        this.evaluatedByStaffUserId = evaluatedByStaffUserId;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getEvaluatedByStaffUserId() {
        return evaluatedByStaffUserId;
    }

    public void setEvaluatedByStaffUserId(Long evaluatedByStaffUserId) {
        this.evaluatedByStaffUserId = evaluatedByStaffUserId;
    }

    public PatientFeedbackEvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(PatientFeedbackEvaluationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
