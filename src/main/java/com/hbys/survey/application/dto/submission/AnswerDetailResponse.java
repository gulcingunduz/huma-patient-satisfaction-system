package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.QuestionType;

import java.math.BigDecimal;

public class AnswerDetailResponse {

    private Long questionId;
    private String questionText;
    private QuestionType questionType;
    private String answerValue;
    private BigDecimal score;

    public AnswerDetailResponse() {
    }

    public AnswerDetailResponse(
            Long questionId,
            String questionText,
            QuestionType questionType,
            String answerValue,
            BigDecimal score
    ) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.answerValue = answerValue;
        this.score = score;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
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