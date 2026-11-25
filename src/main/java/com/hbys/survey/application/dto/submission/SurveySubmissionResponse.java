package com.hbys.survey.application.dto.submission;

import java.time.Instant;

public class SurveySubmissionResponse {

    private Long submissionId;
    private String trackingNumber;
    private Instant submittedAt;

    private Long surveyVersionId;

    private Integer surveyVersionNumber;

    public SurveySubmissionResponse() {
    }

    public SurveySubmissionResponse(Long submissionId, String trackingNumber, Instant submittedAt) {
        this.submissionId = submissionId;
        this.trackingNumber = trackingNumber;
        this.submittedAt = submittedAt;
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Long getSurveyVersionId() {
        return surveyVersionId;
    }

    public void setSurveyVersionId(Long surveyVersionId) {
        this.surveyVersionId = surveyVersionId;
    }

    public Integer getSurveyVersionNumber() {
        return surveyVersionNumber;
    }

    public void setSurveyVersionNumber(Integer surveyVersionNumber) {
        this.surveyVersionNumber = surveyVersionNumber;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
