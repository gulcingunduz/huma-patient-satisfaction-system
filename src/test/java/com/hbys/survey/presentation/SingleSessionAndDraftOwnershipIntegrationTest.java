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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SingleSessionAndDraftOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveySubmissionRepository submissionRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String staff1Username;
    private String staff2Username;
    private String adminUsername;
    private Long submissionId;

    @BeforeEach
    void setup() {
        Department dahiliye = departmentRepository.save(new Department("Dahiliye-" + UUID.randomUUID()));
        Survey survey = surveyRepository.save(new Survey("Hüma Test Survey"));
        SurveySubmission submission = submissionRepository.save(new SurveySubmission(
                survey,
                dahiliye,
                tokenHashService.sha256(UUID.randomUUID().toString()),
                tokenHashService.sha256("TRK-" + UUID.randomUUID()),
                BigDecimal.valueOf(3)
        ));
        submissionId = submission.getId();

        // Two staff in the SAME department with the SAME full name and SAME password, distinguished by unique username & DB id
        staff1Username = "ayse.1." + UUID.randomUUID();
        staff2Username = "ayse.2." + UUID.randomUUID();
        adminUsername = "admin." + UUID.randomUUID();

        staffUserRepository.saveAndFlush(new StaffUser(
                staff1Username, passwordEncoder.encode("OrtakParola123!"), "Dr. Ayşe Yılmaz", StaffRole.STAFF, dahiliye
        ));
        staffUserRepository.saveAndFlush(new StaffUser(
                staff2Username, passwordEncoder.encode("OrtakParola123!"), "Dr. Ayşe Yılmaz", StaffRole.STAFF, dahiliye
        ));
        staffUserRepository.saveAndFlush(new StaffUser(
                adminUsername, passwordEncoder.encode("AdminParola123!"), "Sistem Yöneticisi", StaffRole.ADMIN
        ));
    }

    @Test
    void secondDeviceLoginExpiresFirstSessionImmediately() throws Exception {
        AuthSession device1 = login(staff1Username, "OrtakParola123!");
        mockMvc.perform(get("/api/auth/me").session(device1.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(staff1Username))
                .andExpect(jsonPath("$.userId").isNumber());

        AuthSession device2 = login(staff1Username, "OrtakParola123!");
        mockMvc.perform(get("/api/auth/me").session(device2.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(staff1Username));

        // Device 1 is now expired with structured SESSION_EXPIRED response
        mockMvc.perform(get("/api/auth/me").session(device1.session()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }

    @Test
    void peerStaffInSameDepartmentCannotModifyAnotherStaffDraftWhileAuthorAndAdminCan() throws Exception {
        AuthSession staff1 = login(staff1Username, "OrtakParola123!");
        AuthSession staff2 = login(staff2Username, "OrtakParola123!");
        AuthSession admin = login(adminUsername, "AdminParola123!");

        // Staff 1 creates draft
        postJson("/api/staff/feedback", Map.of(
                "submissionId", submissionId,
                "comment", "İlk doktorun yazdığı taslak."
        ), staff1).andExpect(status().isCreated());

        // Staff 2 (same department, same full name, same password, different userId) CANNOT overwrite Staff 1's draft
        postJson("/api/staff/feedback", Map.of(
                "submissionId", submissionId,
                "comment", "Başka doktorun müdahalesi"
        ), staff2).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Staff 1 CAN update their own draft
        postJson("/api/staff/feedback", Map.of(
                "submissionId", submissionId,
                "comment", "İlk doktorun güncellediği taslak."
        ), staff1).andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("İlk doktorun güncellediği taslak."));

        // Admin CAN update if needed
        postJson("/api/staff/feedback", Map.of(
                "submissionId", submissionId,
                "comment", "Admin düzenlemesi."
        ), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("Admin düzenlemesi."));
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
