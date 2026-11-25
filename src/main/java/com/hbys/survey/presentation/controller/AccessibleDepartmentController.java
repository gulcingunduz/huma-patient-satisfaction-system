package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.survey.DepartmentResponseDto;
import com.hbys.survey.application.service.CatalogManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','PATIENT_RIGHTS_OFFICER')")
public class AccessibleDepartmentController {

    private final CatalogManagementService catalogManagementService;

    public AccessibleDepartmentController(CatalogManagementService catalogManagementService) {
        this.catalogManagementService = catalogManagementService;
    }

    @GetMapping("/accessible")
    public List<DepartmentResponseDto> getAccessibleDepartments(Authentication authentication) {
        return catalogManagementService.getAccessibleDepartments(authentication.getName());
    }
}