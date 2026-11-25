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
class SurveySubmissionEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistSurveySubmission() {
        Department department = new Department("Cardiology");
        Survey survey = new Survey("Patient Satisfaction");

        entityManager.persistAndFlush(department);
        entityManager.persistAndFlush(survey);

        SurveySubmission submission = new SurveySubmission(
                survey,
                department,
                "client-token-hash",
                "tracking-token-hash",
                new BigDecimal("4.80")
        );

        SurveySubmission saved = entityManager.persistAndFlush(submission);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSurvey()).isEqualTo(survey);
        assertThat(saved.getDepartment()).isEqualTo(department);
        assertThat(saved.getOverallScore()).isEqualByComparingTo(new BigDecimal("4.80"));
        assertThat(saved.getClientTokenHash()).isEqualTo("client-token-hash");
        assertThat(saved.getTrackingTokenHash()).isEqualTo("tracking-token-hash");
    }
}
