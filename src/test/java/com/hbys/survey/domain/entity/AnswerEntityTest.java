package com.hbys.survey.domain.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AnswerEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistAnswer() {
        Department department = new Department("Cardiology");
        Survey survey = new Survey("Patient Satisfaction");
        Question question = new Question(survey, "How satisfied are you?", QuestionType.RATING, 1, true);
        SurveySubmission submission = new SurveySubmission(
                survey,
                department,
                "client-token-hash",
                "tracking-token-hash",
                new BigDecimal("4.80")
        );

        Survey savedSurvey = entityManager.persistAndFlush(survey);
        entityManager.persistAndFlush(department);
        Question savedQuestion = entityManager.persistAndFlush(question);
        SurveySubmission savedSubmission = entityManager.persistAndFlush(submission);

        Answer answer = new Answer(savedSubmission, savedQuestion, "5", new BigDecimal("5.00"));
        Answer saved = entityManager.persistAndFlush(answer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubmission()).isEqualTo(savedSubmission);
        assertThat(saved.getQuestion()).isEqualTo(savedQuestion);
        assertThat(saved.getAnswerValue()).isEqualTo("5");
        assertThat(saved.getScore()).isEqualByComparingTo(new BigDecimal("5.00"));
    }
}
