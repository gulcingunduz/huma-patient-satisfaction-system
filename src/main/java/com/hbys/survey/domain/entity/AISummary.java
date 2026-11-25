package com.hbys.survey.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ai_summaries")
@NoArgsConstructor
public class AISummary {

    private static final int MAX_RETRY_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: department/date-range summaries are not tied to a single submission.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "submission_id")
    private SurveySubmission submission;

    @Column(nullable = false, length = 5000)
    private String summaryText;

    @Column(nullable = false, length = 100)
    private String modelName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_staff_user_id", nullable = false)
    private StaffUser createdBy;

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Column(nullable = false)
    private boolean lowScoreOnly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AISummaryStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int submissionCount;

    @Column(length = 1000)
    private String errorMessage;

    @Column(name = "survey_version_scope", length = 255)
    private String surveyVersionScope;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public AISummary(SurveySubmission submission, String summaryText, String modelName) {
        this.submission = submission;
        this.summaryText = summaryText;
        this.modelName = modelName;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.retryCount = 0;
        this.submissionCount = 0;
        this.lowScoreOnly = false;
        this.status = AISummaryStatus.PENDING;
    }

    public AISummary(
            SurveySubmission submission,
            Department department,
            StaffUser createdBy,
            LocalDate dateFrom,
            LocalDate dateTo,
            boolean lowScoreOnly,
            int submissionCount,
            String summaryText,
            String modelName,
            AISummaryStatus status
    ) {
        this.submission = submission;
        this.department = department;
        this.createdBy = createdBy;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.lowScoreOnly = lowScoreOnly;
        this.submissionCount = submissionCount;
        this.summaryText = summaryText;
        this.modelName = modelName;
        this.status = status;
        this.retryCount = 0;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(String summaryText, String modelName) {
        this.summaryText = summaryText;
        this.modelName = modelName;
        this.status = AISummaryStatus.COMPLETED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.summaryText = "Summary generation failed.";
        this.status = AISummaryStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void markPendingRetry() {
        this.status = AISummaryStatus.PENDING;
        this.errorMessage = null;
        this.summaryText = "Summary generation pending.";
        this.modelName = "pending";
        this.updatedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public boolean canRetry() {
        return this.status == AISummaryStatus.FAILED && this.retryCount < MAX_RETRY_COUNT;
    }

    public static int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    public Long getId() {
        return id;
    }

    public SurveySubmission getSubmission() {
        return submission;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getModelName() {
        return modelName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Department getDepartment() {
        return department;
    }

    public StaffUser getCreatedBy() {
        return createdBy;
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

    public String getSurveyVersionScope() {
        return surveyVersionScope;
    }

    public void setSurveyVersionScope(String surveyVersionScope) {
        this.surveyVersionScope = surveyVersionScope;
    }
}
