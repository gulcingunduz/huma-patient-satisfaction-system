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

@Entity
@Table(name = "questions")
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "survey_version_id")
    private SurveyVersion surveyVersion;

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionScope scope = QuestionScope.COMMON;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = true, length = 100)
    private String metricCode;

    public Question(
            Survey survey,
            String text,
            QuestionType type,
            Integer displayOrder,
            boolean required
    ) {
        this.survey = survey;
        this.text = text;
        this.type = type;
        this.displayOrder = displayOrder;
        this.required = required;
    }

    public Long getId() {
        return id;
    }

    public Survey getSurvey() {
        return survey;
    }

    public SurveyVersion getSurveyVersion() {
        return surveyVersion;
    }

    public void setSurveyVersion(SurveyVersion surveyVersion) {
        this.surveyVersion = surveyVersion;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isActive() {
        return active;
    }

    public QuestionScope getScope() {
        return scope;
    }

    public Department getDepartment() {
        return department;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void updateScope(QuestionScope scope, Department department, String metricCode) {
        this.scope = scope;
        this.department = department;
        this.metricCode = metricCode;
    }

    public void updateDetails(
            String text,
            QuestionType type,
            Integer displayOrder,
            boolean required,
            boolean active
    ) {
        this.text = text;
        this.type = type;
        this.displayOrder = displayOrder;
        this.required = required;
        this.active = active;
    }
}
