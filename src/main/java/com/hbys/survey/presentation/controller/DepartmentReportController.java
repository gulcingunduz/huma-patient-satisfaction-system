package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.report.DashboardOverviewResponse;
import com.hbys.survey.application.dto.report.DepartmentReportRequest;
import com.hbys.survey.application.dto.report.DepartmentReportResponse;
import com.hbys.survey.application.service.DepartmentReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','PATIENT_RIGHTS_OFFICER')")
public class DepartmentReportController {

    private final DepartmentReportService departmentReportService;

    public DepartmentReportController(DepartmentReportService departmentReportService) {
        this.departmentReportService = departmentReportService;
    }

    @PostMapping("/departments/summary")
    public DepartmentReportResponse generateSummary(
            Authentication authentication,
            @Valid @RequestBody DepartmentReportRequest request
    ) {
        return departmentReportService.generateReport(request, authentication.getName());
    }

    @GetMapping("/dashboard")
    public DashboardOverviewResponse getDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return departmentReportService.getDashboardOverview(departmentId, dateFrom, dateTo, authentication.getName());
    }
}
