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
import jakarta.persistence.UniqueConstraint;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "survey_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_survey_version_number", columnNames = {"survey_id", "version_number"})
)
@NoArgsConstructor
public class SurveyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyVersionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant publishedAt;

    public SurveyVersion(Survey survey, Integer versionNumber) {
        this.survey = survey;
        this.versionNumber = versionNumber;
        this.status = SurveyVersionStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Survey getSurvey() {
        return survey;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public SurveyVersionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void publish() {
        if (this.status == SurveyVersionStatus.PUBLISHED) {
            return;
        }
        this.status = SurveyVersionStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void archive() {
        this.status = SurveyVersionStatus.ARCHIVED;
    }

    public boolean isPublished() {
        return this.status == SurveyVersionStatus.PUBLISHED;
    }
}
