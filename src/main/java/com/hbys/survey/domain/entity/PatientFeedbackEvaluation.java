package com.hbys.survey.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "patient_feedback_evaluations")
@NoArgsConstructor
public class PatientFeedbackEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SurveySubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private StaffUser evaluatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PatientFeedbackEvaluationStatus status;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public PatientFeedbackEvaluation(
            SurveySubmission submission,
            StaffUser evaluatedBy,
            PatientFeedbackEvaluationStatus status,
            String notes
    ) {
        this.submission = submission;
        this.evaluatedBy = evaluatedBy;
        this.status = status;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public SurveySubmission getSubmission() {
        return submission;
    }

    public StaffUser getEvaluatedBy() {
        return evaluatedBy;
    }

    public PatientFeedbackEvaluationStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateEvaluation(StaffUser evaluatedBy, PatientFeedbackEvaluationStatus status, String notes) {
        this.evaluatedBy = evaluatedBy;
        this.status = status;
        this.notes = notes;
    }
}