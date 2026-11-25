package com.hbys.survey.application.dto.report;

import java.math.BigDecimal;
import java.util.Map;

public record MetricReportResponse(
        String metricCode,
        String questionText,
        BigDecimal averageScore,
        long answerCount,
        Map<Integer, Long> scoreDistribution,
        boolean sufficientData
) {

    public MetricReportResponse(
            String metricCode,
            String questionText,
            BigDecimal averageScore,
            long answerCount,
            Map<Integer, Long> scoreDistribution
    ) {
        this(metricCode, questionText, averageScore, answerCount, scoreDistribution, answerCount >= 3);
    }
}
