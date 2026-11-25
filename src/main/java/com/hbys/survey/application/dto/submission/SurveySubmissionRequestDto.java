package com.hbys.survey.application.dto.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SurveySubmissionRequestDto {

    @NotNull(message = "Survey id is required")
    private Long surveyId;

    @NotEmpty(message = "At least one answer is required")
    @Valid
    private List<AnswerSubmitRequestDto> answers;

    public SurveySubmissionRequestDto() {
    }

    public SurveySubmissionRequestDto(Long surveyId, List<AnswerSubmitRequestDto> answers) {
        this.surveyId = surveyId;
        this.answers = answers;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public List<AnswerSubmitRequestDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerSubmitRequestDto> answers) {
        this.answers = answers;
    }
}
