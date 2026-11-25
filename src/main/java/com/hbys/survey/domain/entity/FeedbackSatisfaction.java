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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "feedback_satisfactions")
@NoArgsConstructor
public class FeedbackSatisfaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private SurveySubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SatisfactionLevel satisfied;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public FeedbackSatisfaction(SurveySubmission submission, SatisfactionLevel satisfied) {
        this.submission = submission;
        this.satisfied = satisfied;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public SurveySubmission getSubmission() {
        return submission;
    }

    public SatisfactionLevel getSatisfied() {
        return satisfied;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}