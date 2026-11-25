package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserAndRbacIntegrationTest {

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
    private TokenHashService tokenHashService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminUsername;
    private Long adminUserId;
    private Department deptA;
    private Department deptB;

    @BeforeEach
    void setup() {
        deptA = departmentRepository.save(new Department("Dept-A-" + UUID.randomUUID()));
        deptB = departmentRepository.save(new Department("Dept-B-" + UUID.randomUUID()));
        adminUsername = "rbac-admin-" + UUID.randomUUID();
        StaffUser savedAdmin = staffUserRepository.saveAndFlush(new StaffUser(
                adminUsername,
                passwordEncoder.encode("admin-pass-123"),
                "RBAC Admin",
                StaffRole.ADMIN
        ));
        adminUserId = savedAdmin.getId();
    }

    @Test
    void adminManagesUsersAndEnforcesRoleBoundariesAcrossStaffAndPatientRightsOfficer() throws Exception {
        AuthSession admin = login(adminUsername, "admin-pass-123");

        // Admin cannot deactivate own account
        mockMvc.perform(patch("/api/admin/users/{id}/active?active=false", adminUserId)
                        .session(admin.session())
                        .cookie(admin.csrfCookie())
                        .header("X-XSRF-TOKEN", admin.csrfToken()))
                .andExpect(status().isBadRequest());

        // STAFF requires departmentId
        postJson("/api/admin/users", Map.of(
                "username", "no-dept-" + UUID.randomUUID(),
                "password", "SecurePass123",
                "fullName", "No Dept Staff",
                "role", "STAFF"
        ), admin).andExpect(status().isBadRequest());

        // Create STAFF for deptA and PATIENT_RIGHTS_OFFICER
        String staffUser = "staff-a-" + UUID.randomUUID();
        MvcResult createdStaff = postJson("/api/admin/users", Map.of(
                "username", staffUser,
                "password", "SecurePass123",
                "fullName", "Staff A",
                "role", "STAFF",
                "departmentId", deptA.getId()
        ), admin).andExpect(status().isCreated()).andReturn();
        assertThat(createdStaff.getResponse().getContentAsString()).doesNotContain("passwordHash");
        assertThat(createdStaff.getResponse().getContentAsString()).doesNotContain("SecurePass123");

        String officerUser = "pro-" + UUID.randomUUID();
        postJson("/api/admin/users", Map.of(
                "username", officerUser,
                "password", "OfficerPass123",
                "fullName", "Rights Officer",
                "role", "PATIENT_RIGHTS_OFFICER"
        ), admin).andExpect(status().isCreated());

        // Seed one submission in deptA and one in deptB
        Survey survey = surveyRepository.save(new Survey("RBAC Survey"));
        SurveySubmission subA = submissionRepository.save(new SurveySubmission(
                survey, deptA, tokenHashService.sha256(UUID.randomUUID().toString()),
                tokenHashService.sha256("TRK-A-" + UUID.randomUUID()), BigDecimal.valueOf(2)
        ));
        SurveySubmission subB = submissionRepository.save(new SurveySubmission(
                survey, deptB, tokenHashService.sha256(UUID.randomUUID().toString()),
                tokenHashService.sha256("TRK-B-" + UUID.randomUUID()), BigDecimal.valueOf(4)
        ));

        // STAFF logs in -> can access own department dashboard & submission, forbidden from admin users & deptB
        AuthSession staff = login(staffUser, "SecurePass123");
        mockMvc.perform(get("/api/admin/users").session(staff.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/submissions/{id}", subB.getId()).session(staff.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/dashboard").param("departmentId", String.valueOf(deptB.getId()))
                        .session(staff.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/dashboard").session(staff.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(1));

        // Staff drafts feedback for subA
        postJson("/api/staff/feedback", Map.of(
                "submissionId", subA.getId(),
                "comment", "İlgili birim incelemesi tamamlandı."
        ), staff).andExpect(status().isCreated());

        // PATIENT_RIGHTS_OFFICER logs in -> forbidden from user/question admin, CAN see evaluation queue & draft
        AuthSession officer = login(officerUser, "OfficerPass123");
        mockMvc.perform(get("/api/admin/users").session(officer.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patient-rights/evaluations/queue").session(officer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].submissionId").value(subA.getId()))
                .andExpect(jsonPath("$[0].feedbackComment").value("İlgili birim incelemesi tamamlandı."))
                .andExpect(jsonPath("$[0].evaluationStatus").value("PENDING"));

        mockMvc.perform(get("/api/reports/dashboard").session(officer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(2));
    }

    private AuthSession login(String username, String password) throws Exception {
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
        return new AuthSession(
                (MockHttpSession) loginResult.getRequest().getSession(false),
                csrfCookie,
                csrfBody.get("token").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body, AuthSession auth) throws Exception {
        return mockMvc.perform(post(path)
                .session(auth.session())
                .cookie(auth.csrfCookie())
                .header("X-XSRF-TOKEN", auth.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private record AuthSession(MockHttpSession session, jakarta.servlet.http.Cookie csrfCookie, String csrfToken) {
    }
}
