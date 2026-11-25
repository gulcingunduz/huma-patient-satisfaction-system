package com.hbys.survey.application.dto.auth;

public record AuthSessionResponse(Long userId, String username, String fullName, String role) {

    public AuthSessionResponse(String username, String fullName, String role) {
        this(null, username, fullName, role);
    }
}
