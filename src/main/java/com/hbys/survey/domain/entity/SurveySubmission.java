package com.hbys.survey.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "survey_submissions")
@NoArgsConstructor
public class SurveySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "survey_version_id")
    private SurveyVersion surveyVersion;

    @Column(nullable = false, length = 128)
    private String clientTokenHash;

    @Column(nullable = false, length = 128)
    private String trackingTokenHash;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SurveySubmission(
            Survey survey,
            Department department,
            String clientTokenHash,
            String trackingTokenHash,
            BigDecimal overallScore
    ) {
        this.survey = survey;
        this.department = department;
        this.clientTokenHash = clientTokenHash;
        this.trackingTokenHash = trackingTokenHash;
        this.overallScore = overallScore;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Survey getSurvey() {
        return survey;
    }

    public Department getDepartment() {
        return department;
    }

    public SurveyVersion getSurveyVersion() {
        return surveyVersion;
    }

    public void setSurveyVersion(SurveyVersion surveyVersion) {
        this.surveyVersion = surveyVersion;
    }

    public String getClientTokenHash() {
        return clientTokenHash;
    }

    public String getTrackingTokenHash() {
        return trackingTokenHash;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
