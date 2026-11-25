package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityErrorResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String staffUsername;

    @BeforeEach
    void createStaffUser() {
        staffUsername = "staff-" + UUID.randomUUID();
        staffUserRepository.saveAndFlush(new StaffUser(
                staffUsername,
                passwordEncoder.encode("test-password"),
                "Test Staff",
                StaffRole.STAFF
        ));
    }

    @Test
    void unauthenticatedRequestReturnsStructuredJson() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/notifications"));
    }

    @Test
    void authenticatedStaffWithoutAdminRoleGetsStructuredForbiddenResponse() throws Exception {
        mockMvc.perform(get("/api/admin/surveys/1/questions")
                .session(loginAsStaff()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/admin/surveys/1/questions"));
    }

        @Test
        void loginCreatesSessionThatCanAccessCurrentUserEndpoint() throws Exception {
        MockHttpSession session = loginAsStaff();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(staffUsername))
            .andExpect(jsonPath("$.role").value("STAFF"));
        }

        private MockHttpSession loginAsStaff() throws Exception {
        var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        var csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        var loginResult = mockMvc.perform(post("/api/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("username", staffUsername, "password", "test-password")
                )))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
        }
}