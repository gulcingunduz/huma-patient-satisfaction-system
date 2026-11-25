package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyAccess;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.SurveyAccessRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import com.hbys.survey.application.service.TokenHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SurveySubmissionWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SurveyAccessRepository surveyAccessRepository;

    @Autowired
    private SurveySubmissionRepository surveySubmissionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TokenHashService tokenHashService;

    private Long surveyId;
    private Long questionId;
    private String publicToken;

    @BeforeEach
    void createPublicSurvey() {
        Department department = departmentRepository.save(new Department("Survey department " + UUID.randomUUID()));
        Survey survey = surveyRepository.save(new Survey("Anonymous patient survey"));
        Question question = questionRepository.save(new Question(
                survey,
                "Rate your visit",
                QuestionType.RATING,
                1,
                true
        ));
        publicToken = "access-" + UUID.randomUUID();
        surveyAccessRepository.save(new SurveyAccess(survey, department, tokenHashService.sha256(publicToken)));
        surveyId = survey.getId();
        questionId = question.getId();
    }

    @Test
    void anonymousSubmissionPersistsAndRejectsSameCookieWithinTheWindow() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        var csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "surveyId", surveyId,
                "answers", new Object[]{Map.of("questionId", questionId, "score", 4)}
        ));

        MvcResult submissionResult = mockMvc.perform(post(
                        "/api/public/surveys/{token}/submissions", publicToken)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").isNotEmpty())
                .andReturn();
        String trackingNumber = objectMapper.readTree(submissionResult.getResponse().getContentAsString())
                .get("trackingNumber").asText();
        var clientCookie = submissionResult.getResponse().getCookie("HBYS_CLIENT_TOKEN");

        assertThat(surveySubmissionRepository.findByTrackingTokenHash(tokenHashService.sha256(trackingNumber)))
                .isPresent();
        assertThat(answerRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/public/surveys/tracking/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(trackingNumber));

        mockMvc.perform(post("/api/public/surveys/{token}/submissions", publicToken)
                        .cookie(csrfCookie, clientCookie)
                        .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SUBMISSION"));
    }
}