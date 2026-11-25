package com.hbys.survey.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointResponse(
        LocalDate date,
        long submissionCount,
        BigDecimal averageScore
) {
}
