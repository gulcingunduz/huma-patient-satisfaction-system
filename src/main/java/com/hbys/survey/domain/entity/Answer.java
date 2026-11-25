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
@Table(name = "answers")
@NoArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SurveySubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_value", nullable = false, length = 2000)
    private String answerValue;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Answer(SurveySubmission submission, Question question, String answerValue, BigDecimal score) {
        this.submission = submission;
        this.question = question;
        this.answerValue = answerValue;
        this.score = score;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public SurveySubmission getSubmission() {
        return submission;
    }

    public Question getQuestion() {
        return question;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
