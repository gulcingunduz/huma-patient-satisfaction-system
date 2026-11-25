package com.hbys.survey.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardOverviewResponse(
        LocalDate dateFrom,
        LocalDate dateTo,
        long totalSubmissions,
        BigDecimal overallAverage,
        long lowScoreCount,
        long criticalScoreCount,
        long pendingFeedbackCount,
        boolean sufficientData,
        int minimumDataThreshold,
        List<DepartmentSummaryItem> departments,
        List<TrendPointResponse> trend,
        List<ComplaintCategoryCountResponse> topComplaints
) {

    public record DepartmentSummaryItem(
            Long departmentId,
            String departmentName,
            long submissionCount,
            BigDecimal averageScore,
            long lowScoreCount,
            boolean sufficientData
    ) {
    }
}
