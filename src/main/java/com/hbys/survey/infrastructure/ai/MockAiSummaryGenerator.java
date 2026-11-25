package com.hbys.survey.infrastructure.ai;

import com.hbys.survey.application.ai.AiSummaryGenerator;
import com.hbys.survey.application.ai.AiSummaryInput;
import com.hbys.survey.application.ai.AiSummaryResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "hbys.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiSummaryGenerator implements AiSummaryGenerator {

    private static final BigDecimal HIGH_METRIC_THRESHOLD = BigDecimal.valueOf(4.0);
    private static final BigDecimal LOW_METRIC_THRESHOLD = BigDecimal.valueOf(3.0);

    @Override
    public AiSummaryResult generate(AiSummaryInput input) {
        BigDecimal average = input.overallAverageRating() == null
                ? BigDecimal.ZERO
                : input.overallAverageRating();

        StringBuilder summary = new StringBuilder();
        String deptPrefix = input.departmentName() != null && !input.departmentName().isBlank()
                ? input.departmentName() + " biriminde "
                : "Seçilen kapsamda ";
        summary.append(deptPrefix)
                .append(input.submissionCount())
                .append(" anonim anket gönderimi incelendi ve genel ortalama ")
                .append(average.toPlainString())
                .append("/5 olarak hesaplandı.");

        List<AiSummaryInput.ComplaintTheme> themes = input.complaintThemes() == null
                ? List.of()
                : input.complaintThemes();
        if (!themes.isEmpty()) {
            String topThemes = themes.stream()
                    .limit(3)
                    .map(theme -> theme.category() + " (" + theme.count() + ")")
                    .collect(Collectors.joining(", "));
            summary.append(" Yorumlarda en sık tekrarlanan şikâyet temaları: ")
                    .append(topThemes)
                    .append(".");
        }

        List<AiSummaryInput.MetricAverage> metrics = input.metricAverages() == null
                ? List.of()
                : input.metricAverages();

        List<AiSummaryInput.MetricAverage> weakMetrics = metrics.stream()
                .filter(m -> m.averageScore() != null && m.averageScore().compareTo(LOW_METRIC_THRESHOLD) <= 0)
                .sorted(Comparator.comparing(AiSummaryInput.MetricAverage::averageScore))
                .limit(3)
                .toList();
        if (!weakMetrics.isEmpty()) {
            String areas = weakMetrics.stream()
                    .map(m -> m.metricCode() + " (" + m.averageScore().toPlainString() + "/5)")
                    .collect(Collectors.joining(", "));
            summary.append(" İyileştirilmesi gereken öncelikli alanlar: ")
                    .append(areas)
                    .append(".");
        }

        List<AiSummaryInput.MetricAverage> strongMetrics = metrics.stream()
                .filter(m -> m.averageScore() != null && m.averageScore().compareTo(HIGH_METRIC_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(AiSummaryInput.MetricAverage::averageScore).reversed())
                .limit(3)
                .toList();
        if (!strongMetrics.isEmpty()) {
            String strengths = strongMetrics.stream()
                    .map(m -> m.metricCode() + " (" + m.averageScore().toPlainString() + "/5)")
                    .collect(Collectors.joining(", "));
            summary.append(" Hastaların görece yüksek puanla değerlendirdiği hizmet alanları: ")
                    .append(strengths)
                    .append(".");
        }

        if (input.commentCount() > 0) {
            summary.append(" Özet ")
                    .append(input.commentCount())
                    .append(" adet anonimleştirilmiş yazılı yorum ve toplu metrik ortalamalarına dayanmaktadır.");
        } else {
            summary.append(" Seçilen dönemde yazılı hasta yorumu bulunmadığından özet puan ortalamaları üzerinden oluşturuldu.");
        }

        return new AiSummaryResult(summary.toString(), "mock-ai");
    }
}
