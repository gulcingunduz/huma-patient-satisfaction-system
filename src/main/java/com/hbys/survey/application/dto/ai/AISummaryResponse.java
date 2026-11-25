package com.hbys.survey.application.dto.ai;

import com.hbys.survey.domain.entity.AISummaryStatus;

import java.time.Instant;
import java.time.LocalDate;

public class AISummaryResponse {

    private Long id;
    private Long submissionId;
    private Long departmentId;
    private String departmentName;
    private String createdByName;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private boolean lowScoreOnly;
    private String surveyVersionScope;
    private String summaryText;
    private String modelName;
    private AISummaryStatus status;
    private int retryCount;
    private int submissionCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public AISummaryResponse() {
    }

    public AISummaryResponse(Long id, Long submissionId, String summaryText, String modelName, Instant createdAt) {
        this.id = id;
        this.submissionId = submissionId;
        this.summaryText = summaryText;
        this.modelName = modelName;
        this.createdAt = createdAt;
    }

    public AISummaryResponse(Long id, Long departmentId, LocalDate dateFrom, LocalDate dateTo,
                             String summaryText, String modelName, AISummaryStatus status, Instant createdAt) {
        this.id = id;
        this.departmentId = departmentId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.summaryText = summaryText;
        this.modelName = modelName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public AISummaryResponse(
            Long id,
            Long departmentId,
            String departmentName,
            String createdByName,
            LocalDate dateFrom,
            LocalDate dateTo,
            boolean lowScoreOnly,
            String surveyVersionScope,
            String summaryText,
            String modelName,
            AISummaryStatus status,
            int retryCount,
            int submissionCount,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.createdByName = createdByName;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.lowScoreOnly = lowScoreOnly;
        this.surveyVersionScope = surveyVersionScope;
        this.summaryText = summaryText;
        this.modelName = modelName;
        this.status = status;
        this.retryCount = retryCount;
        this.submissionCount = submissionCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public boolean isLowScoreOnly() {
        return lowScoreOnly;
    }

    public String getSurveyVersionScope() {
        return surveyVersionScope;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public AISummaryStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getSubmissionCount() {
        return submissionCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
