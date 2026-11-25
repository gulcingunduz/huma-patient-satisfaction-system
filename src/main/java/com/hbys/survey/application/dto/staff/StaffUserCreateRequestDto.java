package com.hbys.survey.application.dto.staff;

import com.hbys.survey.domain.entity.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StaffUserCreateRequestDto {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    private String password;

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private StaffRole role;

    private Long departmentId;

    public StaffUserCreateRequestDto() {
    }

    public StaffUserCreateRequestDto(String username, String password, String fullName, StaffRole role, Long departmentId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
