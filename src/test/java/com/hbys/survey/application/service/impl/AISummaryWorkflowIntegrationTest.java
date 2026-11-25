package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.ai.AiSummaryGenerator;
import com.hbys.survey.application.ai.AiSummaryInput;
import com.hbys.survey.application.ai.AiSummaryResult;
import com.hbys.survey.application.dto.ai.AISummaryRequest;
import com.hbys.survey.application.dto.ai.AISummaryResponse;
import com.hbys.survey.application.exception.AIServiceException;
import com.hbys.survey.application.service.AISummaryService;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.AISummary;
import com.hbys.survey.domain.entity.AISummaryStatus;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.AISummaryRepository;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AISummaryWorkflowIntegrationTest {

    @Autowired
    private AISummaryService aiSummaryService;

    @Autowired
    private AISummaryRepository aiSummaryRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SurveySubmissionRepository submissionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @MockBean
    private AiSummaryGenerator aiSummaryGenerator;

    private Department department;
    private String staffUsername;
    private LocalDate businessDate;

    @BeforeEach
    void createAnonymousData() {
        department = departmentRepository.save(new Department("AI summary department " + UUID.randomUUID()));
        Survey survey = surveyRepository.save(new Survey("AI summary survey"));
        Question ratingQuestion = questionRepository.save(new Question(
                survey, "How was the visit?", QuestionType.RATING, 1, true
        ));
        Question textQuestion = questionRepository.save(new Question(
                survey, "Additional comment", QuestionType.TEXT, 2, false
        ));
        SurveySubmission submission = submissionRepository.save(new SurveySubmission(
                survey,
                department,
                tokenHashService.sha256(UUID.randomUUID().toString()),
                tokenHashService.sha256(UUID.randomUUID().toString()),
                BigDecimal.valueOf(2)
        ));
        answerRepository.saveAll(List.of(
                new Answer(submission, ratingQuestion, "2", BigDecimal.valueOf(2)),
                new Answer(submission, textQuestion, "The waiting time was long.", null)
        ));

        staffUsername = "ai-staff-" + UUID.randomUUID();
        staffUserRepository.save(new StaffUser(
                staffUsername,
                "{noop}not-used",
                "AI Summary Staff",
                StaffRole.STAFF,
                department
        ));
        businessDate = LocalDate.now(ZoneId.of("Europe/Istanbul"));
    }

    @Test
    void aggregatesAnonymousAnswersAndPersistsCompletedSummary() {
        when(aiSummaryGenerator.generate(any(AiSummaryInput.class)))
                .thenReturn(new AiSummaryResult("Wait time was a repeated topic.", "test-provider"));

        AISummaryResponse response = aiSummaryService.createSummary(
                new AISummaryRequest(department.getId(), businessDate, businessDate),
                staffUsername
        );

        assertThat(response.getStatus()).isEqualTo(AISummaryStatus.COMPLETED);
        assertThat(response.getSummaryText()).contains("Wait time");
        assertThat(response.getSubmissionCount()).isEqualTo(1);
        assertThat(aiSummaryRepository.findByDepartment_IdAndStatusOrderByCreatedAtDesc(
                department.getId(), AISummaryStatus.COMPLETED
        )).hasSize(1);
        verify(aiSummaryGenerator).generate(any(AiSummaryInput.class));
    }

    @Test
    void providerFailurePersistsFailedStateWithoutLosingSurveyData() {
        when(aiSummaryGenerator.generate(any(AiSummaryInput.class)))
                .thenThrow(new AIServiceException("Provider unavailable."));

        assertThatThrownBy(() -> aiSummaryService.createSummary(
                new AISummaryRequest(department.getId(), businessDate, businessDate),
                staffUsername
        )).isInstanceOf(AIServiceException.class);

        List<AISummary> failures = aiSummaryRepository.findByDepartment_IdAndStatusOrderByCreatedAtDesc(
                department.getId(), AISummaryStatus.FAILED
        );
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getErrorMessage()).contains("Provider unavailable");
        assertThat(answerRepository.count()).isEqualTo(2);
    }
}