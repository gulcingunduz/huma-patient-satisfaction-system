package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.survey.DepartmentCreateRequestDto;
import com.hbys.survey.application.dto.survey.DepartmentResponseDto;
import com.hbys.survey.application.dto.survey.SurveyAccessCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyAccessResponseDto;
import com.hbys.survey.application.dto.survey.SurveyCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyResponseDto;
import com.hbys.survey.application.service.CatalogManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogManagementService catalogManagementService;

    public AdminCatalogController(CatalogManagementService catalogManagementService) {
        this.catalogManagementService = catalogManagementService;
    }

    @GetMapping("/departments")
    public List<DepartmentResponseDto> getDepartments(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return catalogManagementService.getDepartments(includeInactive);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponseDto createDepartment(@Valid @RequestBody DepartmentCreateRequestDto request) {
        return catalogManagementService.createDepartment(request);
    }

    @PutMapping("/departments/{departmentId}")
    public DepartmentResponseDto updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentCreateRequestDto request
    ) {
        return catalogManagementService.updateDepartment(departmentId, request);
    }

    @PatchMapping("/departments/{departmentId}/active")
    public DepartmentResponseDto setDepartmentActive(
            @PathVariable Long departmentId,
            @RequestParam boolean active
    ) {
        return catalogManagementService.setDepartmentActive(departmentId, active);
    }

    @DeleteMapping("/departments/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long departmentId) {
        catalogManagementService.deleteDepartment(departmentId);
    }

    @GetMapping("/surveys")
    public List<SurveyResponseDto> getSurveys(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return catalogManagementService.getSurveys(includeInactive);
    }

    @PostMapping("/surveys")
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyResponseDto createSurvey(@Valid @RequestBody SurveyCreateRequestDto request) {
        return catalogManagementService.createSurvey(request);
    }

    @PutMapping("/surveys/{surveyId}")
    public SurveyResponseDto updateSurvey(
            @PathVariable Long surveyId,
            @Valid @RequestBody SurveyCreateRequestDto request
    ) {
        return catalogManagementService.updateSurvey(surveyId, request);
    }

    @PatchMapping("/surveys/{surveyId}/active")
    public SurveyResponseDto setSurveyActive(@PathVariable Long surveyId, @RequestParam boolean active) {
        return catalogManagementService.setSurveyActive(surveyId, active);
    }

    @GetMapping("/survey-accesses")
    public List<SurveyAccessResponseDto> getSurveyAccesses(@RequestParam Long departmentId) {
        return catalogManagementService.getSurveyAccesses(departmentId);
    }

    @PostMapping("/survey-accesses")
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyAccessResponseDto createSurveyAccess(@Valid @RequestBody SurveyAccessCreateRequestDto request) {
        return catalogManagementService.createSurveyAccess(request);
    }

    @PatchMapping("/survey-accesses/{accessId}/deactivate")
    public SurveyAccessResponseDto deactivateSurveyAccess(@PathVariable Long accessId) {
        return catalogManagementService.deactivateSurveyAccess(accessId);
    }
}