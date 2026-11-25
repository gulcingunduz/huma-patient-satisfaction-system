package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SatisfactionLevel;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.FeedbackSatisfactionRepository;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.StaffFeedbackRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
class FeedbackWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveySubmissionRepository surveySubmissionRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private StaffFeedbackRepository staffFeedbackRepository;

    @Autowired
    private PatientFeedbackEvaluationRepository evaluationRepository;

    @Autowired
    private FeedbackSatisfactionRepository satisfactionRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long submissionId;
    private String trackingNumber;
    private String staffUsername;
    private String officerUsername;

    @BeforeEach
    void createFeedbackWorkflowData() {
        Department department = departmentRepository.save(new Department("Feedback department " + UUID.randomUUID()));
        Survey survey = surveyRepository.save(new Survey("Feedback workflow survey"));
        trackingNumber = "TRK-" + UUID.randomUUID();
        SurveySubmission submission = surveySubmissionRepository.save(new SurveySubmission(
                survey,
                department,
                tokenHashService.sha256(UUID.randomUUID().toString()),
                tokenHashService.sha256(trackingNumber),
                BigDecimal.valueOf(2)
        ));
        submissionId = submission.getId();

        staffUsername = "staff-" + UUID.randomUUID();
        staffUserRepository.save(new StaffUser(
                staffUsername,
                passwordEncoder.encode("staff-password"),
                "Feedback Staff",
                StaffRole.STAFF,
                department
        ));
        officerUsername = "officer-" + UUID.randomUUID();
        staffUserRepository.save(new StaffUser(
                officerUsername,
                passwordEncoder.encode("officer-password"),
                "Patient Rights Officer",
                StaffRole.PATIENT_RIGHTS_OFFICER
        ));
    }

    @Test
    void staffDraftCanBeApprovedAndPatientCanEvaluateOnlyOnce() throws Exception {
        AuthContext staffContext = login(staffUsername, "staff-password");
        MvcResult feedbackResult = postJson(
                "/api/staff/feedback",
                Map.of("submissionId", submissionId, "comment", "Your feedback is being reviewed.", "score", 4),
                staffContext
        ).andExpect(status().isCreated()).andReturn();
        long feedbackId = objectMapper.readTree(feedbackResult.getResponse().getContentAsString())
                .get("id").asLong();

        AuthContext officerContext = login(officerUsername, "officer-password");
        postJson(
                "/api/patient-rights/evaluations",
                Map.of("submissionId", submissionId, "status", PatientFeedbackEvaluationStatus.APPROVED.name(),
                        "notes", "Approved for publication."),
                officerContext
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/public/surveys/tracking/{trackingNumber}/feedback", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Your feedback is being reviewed."));

        postJson(
                "/api/public/surveys/tracking/" + trackingNumber + "/feedback/satisfaction",
                Map.of("satisfied", SatisfactionLevel.SATISFIED.name()),
                officerContext
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.satisfied").value("SATISFIED"));

        postJson(
                "/api/public/surveys/tracking/" + trackingNumber + "/feedback/satisfaction",
                Map.of("satisfied", SatisfactionLevel.NOT_SATISFIED.name()),
                officerContext
        ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEEDBACK_ALREADY_EVALUATED"));

        assertThat(staffFeedbackRepository.findById(feedbackId)).isPresent();
        assertThat(evaluationRepository.findBySubmission(
                surveySubmissionRepository.findById(submissionId).orElseThrow()
        )).get().extracting(evaluation -> evaluation.getStatus())
                .isEqualTo(PatientFeedbackEvaluationStatus.APPROVED);
        assertThat(satisfactionRepository.count()).isEqualTo(1);
    }

    @Test
    void unauthenticatedStaffFeedbackRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/staff/submissions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private AuthContext login(String username, String password) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        var csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return new AuthContext(
                (MockHttpSession) loginResult.getRequest().getSession(false),
                csrfCookie,
                csrfBody.get("token").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body, AuthContext context)
            throws Exception {
        return mockMvc.perform(post(path)
                .session(context.session())
                .cookie(context.csrfCookie())
                .header("X-XSRF-TOKEN", context.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private record AuthContext(
            MockHttpSession session,
            jakarta.servlet.http.Cookie csrfCookie,
            String csrfToken
    ) {
    }
}