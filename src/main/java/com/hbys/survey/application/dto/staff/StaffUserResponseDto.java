package com.hbys.survey.application.dto.staff;

import com.hbys.survey.domain.entity.StaffRole;

import java.time.Instant;

public class StaffUserResponseDto {

    private Long id;
    private String username;
    private String fullName;
    private StaffRole role;
    private Long departmentId;
    private String departmentName;
    private boolean active;
    private Instant createdAt;

    public StaffUserResponseDto() {
    }

    public StaffUserResponseDto(
            Long id,
            String username,
            String fullName,
            StaffRole role,
            Long departmentId,
            String departmentName,
            boolean active,
            Instant createdAt
    ) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
