package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.staff.StaffUserCreateRequestDto;
import com.hbys.survey.application.dto.staff.StaffUserResponseDto;
import com.hbys.survey.application.dto.staff.StaffUserUpdateRequestDto;
import com.hbys.survey.application.service.StaffUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final StaffUserService staffUserService;

    public AdminUserController(StaffUserService staffUserService) {
        this.staffUserService = staffUserService;
    }

    @GetMapping
    public List<StaffUserResponseDto> listUsers() {
        return staffUserService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffUserResponseDto createUser(@Valid @RequestBody StaffUserCreateRequestDto request) {
        return staffUserService.createUser(request);
    }

    @PutMapping("/{userId}")
    public StaffUserResponseDto updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody StaffUserUpdateRequestDto request,
            Authentication authentication
    ) {
        return staffUserService.updateUser(userId, request, authentication.getName());
    }

    @PatchMapping("/{userId}/active")
    public StaffUserResponseDto setUserActive(
            @PathVariable Long userId,
            @RequestParam boolean active,
            Authentication authentication
    ) {
        return staffUserService.setUserActive(userId, active, authentication.getName());
    }
}
