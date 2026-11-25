package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.report.DashboardOverviewResponse;
import com.hbys.survey.application.dto.report.DepartmentReportRequest;
import com.hbys.survey.application.dto.report.DepartmentReportResponse;

import java.time.LocalDate;

public interface DepartmentReportService {

    DepartmentReportResponse generateReport(DepartmentReportRequest request, String username);

    DashboardOverviewResponse getDashboardOverview(Long departmentId, LocalDate dateFrom, LocalDate dateTo, String username);
}
