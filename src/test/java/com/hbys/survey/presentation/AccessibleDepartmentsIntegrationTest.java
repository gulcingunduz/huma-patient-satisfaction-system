package com.hbys.survey.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccessibleDepartmentsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long staffDepartmentId;
    private String staffUsername;
    private String officerUsername;

    @BeforeEach
    void createUsersAndDepartments() {
        Department staffDepartment = departmentRepository.save(new Department("Staff department " + UUID.randomUUID()));
        departmentRepository.save(new Department("Other department " + UUID.randomUUID()));
        staffDepartmentId = staffDepartment.getId();
        staffUsername = "scoped-staff-" + UUID.randomUUID();
        officerUsername = "scoped-officer-" + UUID.randomUUID();
        staffUserRepository.save(new StaffUser(
                staffUsername, passwordEncoder.encode("staff-password"), "Scoped Staff", StaffRole.STAFF,
                staffDepartment
        ));
        staffUserRepository.save(new StaffUser(
                officerUsername, passwordEncoder.encode("officer-password"), "Scoped Officer",
                StaffRole.PATIENT_RIGHTS_OFFICER
        ));
    }

    @Test
    void staffSeesOnlyTheirOwnDepartment() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/departments/accessible").session(login(staffUsername, "staff-password")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode departments = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(departments).hasSize(1);
        assertThat(departments.get(0).get("id").asLong()).isEqualTo(staffDepartmentId);
    }

    @Test
    void patientRightsOfficerSeesAllActiveDepartments() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/departments/accessible").session(login(officerUsername, "officer-password")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())).hasSize(2);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfResult.getResponse().getCookie("XSRF-TOKEN"))
                        .header("X-XSRF-TOKEN", csrfBody.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}