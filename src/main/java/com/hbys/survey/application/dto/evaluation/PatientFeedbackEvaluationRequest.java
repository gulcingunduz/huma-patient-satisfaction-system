package com.hbys.survey.application.dto.evaluation;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PatientFeedbackEvaluationRequest {

    @NotNull(message = "Submission id is required")
    private Long submissionId;

    @NotNull(message = "Status is required")
    private PatientFeedbackEvaluationStatus status;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    public PatientFeedbackEvaluationRequest() {
    }

    public PatientFeedbackEvaluationRequest(Long submissionId, PatientFeedbackEvaluationStatus status, String notes) {
        this.submissionId = submissionId;
        this.status = status;
        this.notes = notes;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
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
}
