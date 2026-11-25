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

import java.time.Instant;

@Entity
@Table(name = "survey_accesses")
@NoArgsConstructor
public class SurveyAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, unique = true, length = 128, updatable = false)
    private String publicTokenHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SurveyAccess(Survey survey, Department department, String publicTokenHash) {
        this.survey = survey;
        this.department = department;
        this.publicTokenHash = publicTokenHash;
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

    public String getPublicTokenHash() {
        return publicTokenHash;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
