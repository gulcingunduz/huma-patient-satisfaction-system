package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;

import java.time.Instant;

public class TrackingStatusResponse {

    private Long submissionId;
    private String trackingNumber;
    private PatientFeedbackEvaluationStatus status;
    private Instant updatedAt;
    private String notes;

    public TrackingStatusResponse() {
    }

    public TrackingStatusResponse(Long submissionId, String trackingNumber, PatientFeedbackEvaluationStatus status, Instant updatedAt, String notes) {
        this.submissionId = submissionId;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.updatedAt = updatedAt;
        this.notes = notes;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public PatientFeedbackEvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(PatientFeedbackEvaluationStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
