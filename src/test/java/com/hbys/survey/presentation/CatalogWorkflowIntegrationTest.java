package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveyAccessRepository;
import com.hbys.survey.application.service.TokenHashService;
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
class CatalogWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private SurveyAccessRepository surveyAccessRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminUsername;

    @BeforeEach
    void createAdminUser() {
        adminUsername = "admin-" + UUID.randomUUID();
        staffUserRepository.saveAndFlush(new StaffUser(
                adminUsername,
                passwordEncoder.encode("admin-password"),
                "Test Admin",
                StaffRole.ADMIN
        ));
    }

    @Test
    void adminCanCreateCatalogAndPublicUserCanReadQuestionsUsingIssuedToken() throws Exception {
        CsrfSession csrfSession = loginAsAdmin();

        MvcResult departmentResult = postJson(
                "/api/admin/departments",
                Map.of("name", "Department-" + UUID.randomUUID()),
                csrfSession
        ).andExpect(status().isCreated()).andReturn();
        long departmentId = readBody(departmentResult).get("id").asLong();

        MvcResult surveyResult = postJson(
                "/api/admin/surveys",
                Map.of("title", "Patient survey"),
                csrfSession
        ).andExpect(status().isCreated()).andReturn();
        long surveyId = readBody(surveyResult).get("id").asLong();

        postJson(
                "/api/admin/surveys/" + surveyId + "/questions",
                Map.of("text", "How was your visit?", "type", "RATING", "displayOrder", 1, "required", true),
                csrfSession
        ).andExpect(status().isCreated());

        MvcResult accessResult = postJson(
                "/api/admin/survey-accesses",
                Map.of("surveyId", surveyId, "departmentId", departmentId),
                csrfSession
        ).andExpect(status().isCreated()).andReturn();
        String publicToken = readBody(accessResult).get("publicAccessToken").asText();

        assertThat(publicToken).isNotBlank();
        assertThat(surveyAccessRepository.findByPublicTokenHashAndActiveTrue(tokenHashService.sha256(publicToken)))
                .isPresent()
                .get()
                .extracting(access -> access.getPublicTokenHash())
                .isNotEqualTo(publicToken);

        mockMvc.perform(get("/api/public/surveys/access/{token}/questions", publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("How was your visit?"))
                .andExpect(jsonPath("$[0].type").value("RATING"));
    }

    private CsrfSession loginAsAdmin() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfBody = readBody(csrfResult);
        var csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", adminUsername, "password", "admin-password")
                        )))
                .andExpect(status().isOk())
                .andReturn();
        return new CsrfSession(
                (MockHttpSession) loginResult.getRequest().getSession(false),
                csrfCookie,
                csrfBody.get("token").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions postJson(
            String path,
            Object body,
            CsrfSession csrfSession
    ) throws Exception {
        return mockMvc.perform(post(path)
                .session(csrfSession.session())
                .cookie(csrfSession.csrfCookie())
                .header("X-XSRF-TOKEN", csrfSession.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record CsrfSession(MockHttpSession session, jakarta.servlet.http.Cookie csrfCookie, String token) {
    }
}