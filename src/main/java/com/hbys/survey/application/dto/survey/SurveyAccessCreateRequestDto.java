package com.hbys.survey.application.dto.survey;

import jakarta.validation.constraints.NotNull;

public class SurveyAccessCreateRequestDto {

    @NotNull
    private Long surveyId;

    @NotNull
    private Long departmentId;

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}