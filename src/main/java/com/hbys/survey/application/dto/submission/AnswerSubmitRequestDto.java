package com.hbys.survey.application.dto.submission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AnswerSubmitRequestDto {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @Size(max = 500, message = "Answer text must be at most 500 characters")
    private String answerValue;

    @DecimalMin(value = "1.0", message = "Rating must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "Rating must be between 1 and 5")
    private BigDecimal score;

    public AnswerSubmitRequestDto() {
    }

    public AnswerSubmitRequestDto(Long questionId, String answerValue, BigDecimal score) {
        this.questionId = questionId;
        this.answerValue = answerValue;
        this.score = score;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public void setAnswerValue(String answerValue) {
        this.answerValue = answerValue;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
