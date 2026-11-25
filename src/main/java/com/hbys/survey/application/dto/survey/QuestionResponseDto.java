package com.hbys.survey.application.dto.survey;

import com.hbys.survey.domain.entity.QuestionType;

public class QuestionResponseDto {

    private Long id;
    private Long surveyId;
    private String text;
    private QuestionType type;
    private Integer displayOrder;
    private boolean required;
    private boolean active;

    private com.hbys.survey.domain.entity.QuestionScope scope;

    private Long departmentId;

    private String departmentName;

    private String metricCode;

    private Long surveyVersionId;

    private Integer surveyVersionNumber;

    public QuestionResponseDto() {
    }

    public QuestionResponseDto(Long id, Long surveyId, String text, QuestionType type, Integer displayOrder, boolean required, boolean active) {
        this.id = id;
        this.surveyId = surveyId;
        this.text = text;
        this.type = type;
        this.displayOrder = displayOrder;
        this.required = required;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public com.hbys.survey.domain.entity.QuestionScope getScope() {
        return scope;
    }

    public void setScope(com.hbys.survey.domain.entity.QuestionScope scope) {
        this.scope = scope;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
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
}
