package com.hbys.survey.application.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Anonymous payload sent to the AI generator.
 * Must not contain tracking tokens, cookies, usernames, or staff identity.
 */
public record AiSummaryInput(
        String departmentName,
        LocalDate dateFrom,
        LocalDate dateTo,
        String surveyVersionScope,
        int submissionCount,
        BigDecimal overallAverageRating,
        List<QuestionAverage> questionAverages,
        Map<Integer, Long> scoreDistribution,
        int commentCount,
        List<String> comments,
        List<MetricAverage> metricAverages,
        List<ComplaintTheme> complaintThemes
) {

    public AiSummaryInput(
            int submissionCount,
            BigDecimal overallAverageRating,
            List<QuestionAverage> questionAverages,
            Map<Integer, Long> scoreDistribution,
            int commentCount,
            List<String> comments,
            List<MetricAverage> metricAverages,
            List<ComplaintTheme> complaintThemes
    ) {
        this(null, null, null, null, submissionCount, overallAverageRating, questionAverages,
                scoreDistribution, commentCount, comments, metricAverages, complaintThemes);
    }

    public record QuestionAverage(String questionText, BigDecimal averageScore, long answerCount) {
    }

    public record MetricAverage(
            String metricCode,
            String scope,
            String questionText,
            BigDecimal averageScore,
            long answerCount
    ) {
    }

    public record ComplaintTheme(String category, long count) {
    }
}
