package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.NotificationType;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.NotificationRepository;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SurveyVersioningAndScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveySubmissionRepository submissionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private String adminUsername;
    private StaffUser adminUser;

    @BeforeEach
    void setupAdmin() {
        adminUsername = "ver-admin-" + UUID.randomUUID();
        adminUser = staffUserRepository.saveAndFlush(new StaffUser(
                adminUsername,
                passwordEncoder.encode("admin-pass-123"),
                "Version Admin",
                StaffRole.ADMIN
        ));
    }

    @Test
    void enforcesCommonAndDepartmentScopeVersioningNotificationsAndResubmissionAfter24Hours() throws Exception {
        AuthSession admin = login(adminUsername, "admin-pass-123");

        long acilId = createDepartment(admin, "Acil-" + UUID.randomUUID());
        long labId = createDepartment(admin, "Lab-" + UUID.randomUUID());
        Department acilDept = departmentRepository.findById(acilId).orElseThrow();
        StaffUser acilStaff = staffUserRepository.saveAndFlush(new StaffUser(
                "acil-staff-" + UUID.randomUUID(),
                passwordEncoder.encode("staff-pass-123"),
                "Acil Staff",
                StaffRole.STAFF,
                acilDept
        ));

        long surveyId = createSurvey(admin, "Genel Hasta Memnuniyet Anketi");

        // Create v1 DRAFT
        MvcResult v1Res = postJson("/api/admin/surveys/" + surveyId + "/versions", Map.of(), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long v1Id = readJson(v1Res).get("id").asLong();

        // Add COMMON + Acil-specific + Lab-specific + optional TEXT question to v1
        long commonQId = readJson(postJson("/api/admin/surveys/" + surveyId + "/questions", Map.of(
                "text", "Personelin iletişiminden memnun kaldınız mı?",
                "type", "RATING",
                "scope", "COMMON",
                "metricCode", "COMMUNICATION",
                "displayOrder", 10,
                "required", true
        ), admin).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        long acilQId = readJson(postJson("/api/admin/surveys/" + surveyId + "/questions", Map.of(
                "text", "İlk müdahale süresi beklentilerinizi karşıladı mı?",
                "type", "RATING",
                "scope", "DEPARTMENT_SPECIFIC",
                "departmentId", acilId,
                "metricCode", "FIRST_RESPONSE_TIME",
                "displayOrder", 20,
                "required", true
        ), admin).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        long labQId = readJson(postJson("/api/admin/surveys/" + surveyId + "/questions", Map.of(
                "text", "Numune alma sürecini nasıl değerlendirirsiniz?",
                "type", "RATING",
                "scope", "DEPARTMENT_SPECIFIC",
                "departmentId", labId,
                "metricCode", "SAMPLE_COLLECTION",
                "displayOrder", 20,
                "required", true
        ), admin).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        long textQId = readJson(postJson("/api/admin/surveys/" + surveyId + "/questions", Map.of(
                "text", "Görüş ve öneriniz",
                "type", "TEXT",
                "scope", "COMMON",
                "metricCode", "GENERAL_COMMENT",
                "displayOrder", 90,
                "required", false
        ), admin).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        // Publish v1
        patchNoBody("/api/admin/surveys/" + surveyId + "/versions/" + v1Id + "/publish", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Published question cannot be edited directly
        putJson("/api/admin/questions/" + commonQId, Map.of(
                "text", "Mutated published question",
                "type", "RATING",
                "scope", "COMMON",
                "metricCode", "COMMUNICATION",
                "displayOrder", 10,
                "required", true,
                "active", true
        ), admin).andExpect(status().isBadRequest());

        // Issue public access tokens for Acil & Lab
        String acilToken = readJson(postJson("/api/admin/survey-accesses",
                Map.of("surveyId", surveyId, "departmentId", acilId), admin)
                .andExpect(status().isCreated()).andReturn()).get("publicAccessToken").asText();
        String labToken = readJson(postJson("/api/admin/survey-accesses",
                Map.of("surveyId", surveyId, "departmentId", labId), admin)
                .andExpect(status().isCreated()).andReturn()).get("publicAccessToken").asText();

        // Public info endpoint returns department name without exposing tokens
        mockMvc.perform(get("/api/public/surveys/access/{token}", acilToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(acilId))
                .andExpect(jsonPath("$.surveyVersionNumber").value(1))
                .andExpect(jsonPath("$.questionCount").value(3));

        // Acil receives COMMON + Acil-specific + TEXT (never Lab-specific)
        JsonNode acilQuestions = readJson(mockMvc.perform(get("/api/public/surveys/access/{token}/questions", acilToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(acilQuestions).hasSize(3);
        assertThat(acilQuestions.findValuesAsText("metricCode"))
                .containsExactly("COMMUNICATION", "FIRST_RESPONSE_TIME", "GENERAL_COMMENT");

        // Lab receives COMMON + Lab-specific + TEXT (never Acil-specific)
        JsonNode labQuestions = readJson(mockMvc.perform(get("/api/public/surveys/access/{token}/questions", labToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(labQuestions).hasSize(3);
        assertThat(labQuestions.findValuesAsText("metricCode"))
                .containsExactly("COMMUNICATION", "SAMPLE_COLLECTION", "GENERAL_COMMENT");

        // Cross-department injection (submitting Lab question via Acil token) is rejected
        CsrfBundle csrf = fetchCsrf();
        mockMvc.perform(post("/api/public/surveys/{token}/submissions", acilToken)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "surveyId", surveyId,
                                "answers", List.of(
                                        Map.of("questionId", commonQId, "score", 4),
                                        Map.of("questionId", labQId, "score", 4)
                                )
                        ))))
                .andExpect(status().isBadRequest());

        // Submit low-score (<= 2) anonymous response for Acil under v1
        MvcResult submitRes = mockMvc.perform(post("/api/public/surveys/{token}/submissions", acilToken)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "surveyId", surveyId,
                                "answers", List.of(
                                        Map.of("questionId", commonQId, "score", 2),
                                        Map.of("questionId", acilQId, "score", 1),
                                        Map.of("questionId", textQId, "answerValue", "Bekleme süresi çok uzundu, TC 12345678901.")
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.surveyVersionNumber").value(1))
                .andReturn();
        var clientCookie = submitRes.getResponse().getCookie("HBYS_CLIENT_TOKEN");
        assertThat(clientCookie).isNotNull();

        // Critical notification (<= 2) created for Acil staff AND admin
        assertThat(notificationRepository.findByStaffUserOrderByCreatedAtDesc(acilStaff))
                .anySatisfy(n -> assertThat(n.getType()).isEqualTo(NotificationType.SYSTEM_ALERT));
        long adminNotifCountBeforeAi = notificationRepository.findByStaffUserOrderByCreatedAtDesc(adminUser).size();
        assertThat(adminNotifCountBeforeAi).isGreaterThanOrEqualTo(1);

        // AI summary runs cleanly, includes version scope, masks PII, and creates ZERO notifications
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        MvcResult aiRes = postJson("/api/ai-summaries", Map.of(
                "departmentId", acilId,
                "dateFrom", today.toString(),
                "dateTo", today.toString(),
                "lowScoreOnly", true
        ), admin).andExpect(status().isCreated()).andReturn();
        JsonNode aiJson = readJson(aiRes);
        assertThat(aiJson.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(aiJson.get("surveyVersionScope").asText()).contains("v1");
        assertThat(aiJson.get("summaryText").asText()).doesNotContain("12345678901");
        assertThat(notificationRepository.findByStaffUserOrderByCreatedAtDesc(adminUser))
                .hasSize((int) adminNotifCountBeforeAi);

        // Create v2 DRAFT -> clones v1 questions! Add a new question to v2 and publish v2
        MvcResult v2Res = postJson("/api/admin/surveys/" + surveyId + "/versions", Map.of(), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andReturn();
        long v2Id = readJson(v2Res).get("id").asLong();

        JsonNode v2Questions = readJson(mockMvc.perform(
                        get("/api/admin/surveys/" + surveyId + "/questions")
                                .param("versionId", String.valueOf(v2Id))
                                .session(admin.session()))
                .andExpect(status().isOk()).andReturn());
        assertThat(v2Questions).hasSize(4);

        postJson("/api/admin/surveys/" + surveyId + "/questions", Map.of(
                "text", "Acil servis süreçlerini nasıl değerlendirirsiniz?",
                "type", "RATING",
                "scope", "DEPARTMENT_SPECIFIC",
                "departmentId", acilId,
                "metricCode", "EMERGENCY_PROCESS",
                "displayOrder", 30,
                "required", true
        ), admin).andExpect(status().isCreated());

        patchNoBody("/api/admin/surveys/" + surveyId + "/versions/" + v2Id + "/publish", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Public Acil endpoint now serves v2 (4 questions including EMERGENCY_PROCESS)
        JsonNode acilV2Questions = readJson(mockMvc.perform(get("/api/public/surveys/access/{token}/questions", acilToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(acilV2Questions).hasSize(4);
        assertThat(acilV2Questions.findValuesAsText("metricCode")).contains("EMERGENCY_PROCESS");

        // Historical report still preserves v1 submission metrics accurately
        MvcResult reportRes = postJson("/api/reports/departments/summary", Map.of(
                "departmentId", acilId,
                "dateFrom", today.toString(),
                "dateTo", today.toString(),
                "lowScoreOnly", false
        ), admin).andExpect(status().isOk()).andReturn();
        JsonNode reportJson = readJson(reportRes);
        assertThat(reportJson.get("submissionCount").asInt()).isEqualTo(1);
        assertThat(reportJson.get("sufficientData").asBoolean()).isFalse();
        assertThat(reportJson.get("commonMetrics").get(0).get("metricCode").asText()).isEqualTo("COMMUNICATION");
        assertThat(reportJson.get("departmentMetrics").get(0).get("metricCode").asText()).isEqualTo("FIRST_RESPONSE_TIME");
        assertThat(reportJson.get("complaints").get(0).get("category").asText()).isEqualTo("Bekleme süresi");

        // Resubmission with same cookie is allowed once previous submission is older than 24 hours
        Survey survey = surveyRepository.findById(surveyId).orElseThrow();
        SurveySubmission previous = submissionRepository.findBySurveyAndActiveTrueOrderByCreatedAtDesc(survey).get(0);
        entityManager.createQuery("update SurveySubmission s set s.createdAt = :older where s.id = :id")
                .setParameter("older", Instant.now().minus(25, ChronoUnit.HOURS))
                .setParameter("id", previous.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        List<Map<String, Object>> v2Answers = new java.util.ArrayList<>();
        for (JsonNode q : acilV2Questions) {
            if ("RATING".equals(q.get("type").asText())) {
                v2Answers.add(Map.of("questionId", q.get("id").asLong(), "score", 5));
            }
        }
        mockMvc.perform(post("/api/public/surveys/{token}/submissions", acilToken)
                        .cookie(csrf.cookie(), clientCookie)
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "surveyId", surveyId,
                                "answers", v2Answers
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.surveyVersionNumber").value(2));
    }

    private long createDepartment(AuthSession admin, String name) throws Exception {
        return readJson(postJson("/api/admin/departments", Map.of("name", name), admin)
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private long createSurvey(AuthSession admin, String title) throws Exception {
        return readJson(postJson("/api/admin/surveys", Map.of("title", title), admin)
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private CsrfBundle fetchCsrf() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        return new CsrfBundle(
                csrfResult.getResponse().getCookie("XSRF-TOKEN"),
                readJson(csrfResult).get("token").asText()
        );
    }

    private AuthSession login(String username, String password) throws Exception {
        CsrfBundle csrf = fetchCsrf();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return new AuthSession((MockHttpSession) loginResult.getRequest().getSession(false), csrf.cookie(), csrf.token());
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body, AuthSession auth) throws Exception {
        return mockMvc.perform(post(path)
                .session(auth.session())
                .cookie(auth.csrfCookie())
                .header("X-XSRF-TOKEN", auth.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String path, Object body, AuthSession auth) throws Exception {
        return mockMvc.perform(put(path)
                .session(auth.session())
                .cookie(auth.csrfCookie())
                .header("X-XSRF-TOKEN", auth.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private org.springframework.test.web.servlet.ResultActions patchNoBody(String path, AuthSession auth) throws Exception {
        return mockMvc.perform(patch(path)
                .session(auth.session())
                .cookie(auth.csrfCookie())
                .header("X-XSRF-TOKEN", auth.csrfToken()));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record CsrfBundle(jakarta.servlet.http.Cookie cookie, String token) {
    }

    private record AuthSession(MockHttpSession session, jakarta.servlet.http.Cookie csrfCookie, String csrfToken) {
    }
}
