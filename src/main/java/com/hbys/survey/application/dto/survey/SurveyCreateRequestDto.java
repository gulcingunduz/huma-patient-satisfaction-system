package com.hbys.survey.application.dto.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SurveyCreateRequestDto {

    @NotBlank(message = "Survey title cannot be blank")
    @Size(max = 150, message = "Survey title must be at most 150 characters")
    private String title;

    public SurveyCreateRequestDto() {
    }

    public SurveyCreateRequestDto(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
