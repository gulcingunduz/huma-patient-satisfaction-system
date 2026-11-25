package com.hbys.survey.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DepartmentReportResponse(
        Long departmentId,
        String departmentName,
        LocalDate dateFrom,
        LocalDate dateTo,
        long submissionCount,
        long answerCount,
        long lowScoreCount,
        BigDecimal overallAverage,
        boolean sufficientData,
        int minimumDataThreshold,
        Map<Integer, Long> scoreDistribution,
        List<MetricReportResponse> commonMetrics,
        List<MetricReportResponse> departmentMetrics,
        List<ComplaintCategoryCountResponse> complaints,
        List<TrendPointResponse> trend
) {
}
