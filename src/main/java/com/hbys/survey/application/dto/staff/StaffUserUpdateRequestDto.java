package com.hbys.survey.application.dto.staff;

import com.hbys.survey.domain.entity.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StaffUserUpdateRequestDto {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private StaffRole role;

    private Long departmentId;

    private boolean active = true;

    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    private String newPassword;

    public StaffUserUpdateRequestDto() {
    }

    public StaffUserUpdateRequestDto(String fullName, StaffRole role, Long departmentId, boolean active) {
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
        this.active = active;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
