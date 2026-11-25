package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.ai.AiSummaryGenerator;
import com.hbys.survey.application.ai.AiSummaryInput;
import com.hbys.survey.application.ai.AiSummaryResult;
import com.hbys.survey.application.ai.PiiMasker;
import com.hbys.survey.application.dto.ai.AISummaryRequest;
import com.hbys.survey.application.dto.ai.AISummaryResponse;
import com.hbys.survey.application.exception.AISummaryNotFoundException;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.DepartmentNotFoundException;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.service.AISummaryService;
import com.hbys.survey.application.service.ComplaintCategoryService;
import com.hbys.survey.domain.entity.AISummary;
import com.hbys.survey.domain.entity.AISummaryStatus;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.AISummaryRepository;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AISummaryServiceImpl implements AISummaryService {

    private static final BigDecimal LOW_SCORE_THRESHOLD = BigDecimal.valueOf(3);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");

    private final AISummaryRepository aiSummaryRepository;
    private final AnswerRepository answerRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffUserRepository staffUserRepository;
    private final ComplaintCategoryService complaintCategoryService;
    private final AiSummaryGenerator aiSummaryGenerator;
    private final TransactionTemplate transactionTemplate;

    public AISummaryServiceImpl(
            AISummaryRepository aiSummaryRepository,
            AnswerRepository answerRepository,
            SurveySubmissionRepository surveySubmissionRepository,
            DepartmentRepository departmentRepository,
            StaffUserRepository staffUserRepository,
            ComplaintCategoryService complaintCategoryService,
            AiSummaryGenerator aiSummaryGenerator,
            PlatformTransactionManager transactionManager
    ) {
        this.aiSummaryRepository = aiSummaryRepository;
        this.answerRepository = answerRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.departmentRepository = departmentRepository;
        this.staffUserRepository = staffUserRepository;
        this.complaintCategoryService = complaintCategoryService;
        this.aiSummaryGenerator = aiSummaryGenerator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public AISummaryResponse createSummary(AISummaryRequest request, String username) {
        validateDateRange(request);

        PreparedSummary prepared = transactionTemplate.execute(status -> prepareSummary(request, username));
        if (prepared == null) {
            throw new BadRequestException("Unable to prepare AI summary.");
        }
        if (!prepared.hasAnalyzableData()) {
            throw new BadRequestException("No comments or analyzable data found for the selected filters.");
        }

        Long summaryId = transactionTemplate.execute(status -> {
            AISummary pending = new AISummary(
                    null,
                    prepared.department(),
                    prepared.creator(),
                    request.getDateFrom(),
                    request.getDateTo(),
                    request.isLowScoreOnly(),
                    prepared.input().submissionCount(),
                    "Summary generation pending.",
                    "pending",
                    AISummaryStatus.PENDING
            );
            pending.setSurveyVersionScope(prepared.input().surveyVersionScope());
            return aiSummaryRepository.save(pending).getId();
        });

        return generateAndPersist(summaryId, prepared.input());
    }

    @Override
    public AISummaryResponse retrySummary(Long summaryId, String username) {
        PreparedRetry prepared = transactionTemplate.execute(status -> {
            StaffUser actor = findActiveStaffUser(username);
            AISummary summary = aiSummaryRepository.findById(summaryId)
                    .orElseThrow(AISummaryNotFoundException::new);
            authorizeDepartment(actor, summary.getDepartment());

            if (!summary.canRetry()) {
                throw new BadRequestException(
                        "Summary cannot be retried. Maximum retry count is " + AISummary.getMaxRetryCount() + "."
                );
            }

            summary.incrementRetryCount();
            summary.markPendingRetry();

            AISummaryRequest filter = new AISummaryRequest(
                    summary.getDepartment().getId(),
                    summary.getDateFrom(),
                    summary.getDateTo(),
                    summary.isLowScoreOnly()
            );
            FilteredData data = loadFilteredData(summary.getDepartment(), filter);
            AiSummaryInput input = buildAiInput(summary.getDepartment(), filter, data);
            summary.setSurveyVersionScope(input.surveyVersionScope());
            if (!hasAnalyzableData(input)) {
                summary.fail("No comments or analyzable data found for the selected filters.");
                throw new BadRequestException("No comments or analyzable data found for the selected filters.");
            }
            return new PreparedRetry(summary.getId(), input);
        });

        if (prepared == null) {
            throw new BadRequestException("Unable to prepare AI summary retry.");
        }
        return generateAndPersist(prepared.summaryId(), prepared.input());
    }

    @Override
    public AISummaryResponse getSummary(Long summaryId, String username) {
        return transactionTemplate.execute(status -> {
            StaffUser actor = findActiveStaffUser(username);
            AISummary summary = aiSummaryRepository.findById(summaryId)
                    .orElseThrow(AISummaryNotFoundException::new);
            authorizeDepartment(actor, summary.getDepartment());
            return toResponse(summary);
        });
    }

    @Override
    public List<AISummaryResponse> listSummaries(Long departmentId, String username) {
        return transactionTemplate.execute(status -> {
            StaffUser actor = findActiveStaffUser(username);
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(DepartmentNotFoundException::new);
            authorizeDepartment(actor, department);
            return aiSummaryRepository.findByDepartment_IdOrderByCreatedAtDesc(departmentId).stream()
                    .map(AISummaryServiceImpl::toResponse)
                    .toList();
        });
    }

    /**
     * AI call stays outside the database transaction so external latency cannot hold DB locks.
     */
    private AISummaryResponse generateAndPersist(Long summaryId, AiSummaryInput input) {
        try {
            AiSummaryResult result = aiSummaryGenerator.generate(input);
            return transactionTemplate.execute(status -> {
                AISummary summary = aiSummaryRepository.findById(summaryId)
                        .orElseThrow(AISummaryNotFoundException::new);
                summary.complete(result.summaryText(), result.modelName());
                return toResponse(summary);
            });
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                AISummary summary = aiSummaryRepository.findById(summaryId).orElse(null);
                if (summary != null) {
                    String message = exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "Summary generation failed."
                            : exception.getMessage();
                    summary.fail(truncate(message, 1000));
                }
            });
            throw exception;
        }
    }

    private PreparedSummary prepareSummary(AISummaryRequest request, String username) {
        StaffUser creator = findActiveStaffUser(username);
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(DepartmentNotFoundException::new);
        authorizeDepartment(creator, department);

        FilteredData data = loadFilteredData(department, request);
        AiSummaryInput input = buildAiInput(department, request, data);
        return new PreparedSummary(department, creator, input);
    }

    private FilteredData loadFilteredData(Department department, AISummaryRequest request) {
        Instant from = startOfBusinessDay(request.getDateFrom());
        Instant to = endOfBusinessDay(request.getDateTo());

        List<SurveySubmission> submissions = surveySubmissionRepository
                .findByDepartmentAndCreatedAtBetween(department, from, to)
                .stream()
                .filter(submission -> !request.isLowScoreOnly()
                        || submission.getOverallScore().compareTo(LOW_SCORE_THRESHOLD) <= 0)
                .toList();

        if (submissions.isEmpty()) {
            return new FilteredData(List.of(), List.of());
        }

        List<Long> submissionIds = submissions.stream().map(SurveySubmission::getId).toList();
        List<Answer> answers = answerRepository.findBySubmissionIdInWithQuestion(submissionIds);
        return new FilteredData(submissions, answers);
    }

    static Instant startOfBusinessDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    static Instant endOfBusinessDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant().minusNanos(1);
    }

    private AiSummaryInput buildAiInput(Department department, AISummaryRequest request, FilteredData data) {
        int submissionCount = data.submissions().size();
        BigDecimal overallAverage = calculateOverallAverage(data.submissions());
        List<AiSummaryInput.QuestionAverage> questionAverages = calculateQuestionAverages(data.answers());
        Map<Integer, Long> scoreDistribution = calculateScoreDistribution(data.answers());
        List<String> comments = extractMaskedComments(data.answers());
        List<AiSummaryInput.MetricAverage> metricAverages = calculateMetricAverages(data.answers());
        List<AiSummaryInput.ComplaintTheme> complaintThemes = calculateComplaintThemes(data.answers());
        String versionScope = resolveSurveyVersionScope(data.submissions());

        return new AiSummaryInput(
                department.getName(),
                request.getDateFrom(),
                request.getDateTo(),
                versionScope,
                submissionCount,
                overallAverage,
                questionAverages,
                scoreDistribution,
                comments.size(),
                comments,
                metricAverages,
                complaintThemes
        );
    }

    private String resolveSurveyVersionScope(List<SurveySubmission> submissions) {
        if (submissions.isEmpty()) {
            return null;
        }
        String scope = submissions.stream()
                .map(submission -> {
                    String title = submission.getSurvey() != null ? submission.getSurvey().getTitle() : "Anket";
                    if (submission.getSurveyVersion() != null) {
                        return title + " (v" + submission.getSurveyVersion().getVersionNumber() + ")";
                    }
                    return title + " (temel)";
                })
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return truncate(scope, 255);
    }

    private List<AiSummaryInput.MetricAverage> calculateMetricAverages(List<Answer> answers) {
        Map<String, List<Answer>> grouped = answers.stream()
                .filter(answer -> answer.getScore() != null)
                .filter(answer -> answer.getQuestion().getType() == QuestionType.RATING)
                .collect(Collectors.groupingBy(answer -> {
                    String code = answer.getQuestion().getMetricCode();
                    return code == null || code.isBlank()
                            ? "QUESTION_" + answer.getQuestion().getId()
                            : code;
                }));

        List<AiSummaryInput.MetricAverage> metrics = new ArrayList<>();
        grouped.forEach((code, metricAnswers) -> {
            Answer sample = metricAnswers.get(0);
            BigDecimal total = metricAnswers.stream()
                    .map(Answer::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = total.divide(
                    BigDecimal.valueOf(metricAnswers.size()),
                    2,
                    RoundingMode.HALF_UP
            );
            metrics.add(new AiSummaryInput.MetricAverage(
                    code,
                    sample.getQuestion().getScope().name(),
                    sample.getQuestion().getText(),
                    average,
                    metricAnswers.size()
            ));
        });
        metrics.sort(Comparator.comparing(AiSummaryInput.MetricAverage::metricCode));
        return metrics;
    }

    private List<AiSummaryInput.ComplaintTheme> calculateComplaintThemes(List<Answer> answers) {
        Map<String, Long> themes = answers.stream()
                .filter(answer -> answer.getQuestion().getType() == QuestionType.TEXT)
                .map(Answer::getAnswerValue)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> complaintCategoryService.categorizeAll(value).stream())
                .filter(theme -> theme != null && !"Diğer".equals(theme))
                .collect(Collectors.groupingBy(theme -> theme, Collectors.counting()));

        return themes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new AiSummaryInput.ComplaintTheme(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal calculateOverallAverage(List<SurveySubmission> submissions) {
        List<BigDecimal> scores = submissions.stream()
                .map(SurveySubmission::getOverallScore)
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private List<AiSummaryInput.QuestionAverage> calculateQuestionAverages(List<Answer> answers) {
        Map<Long, List<Answer>> byQuestion = answers.stream()
                .filter(answer -> answer.getScore() != null)
                .filter(answer -> answer.getQuestion().getType() == QuestionType.RATING)
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        List<AiSummaryInput.QuestionAverage> averages = new ArrayList<>();
        for (List<Answer> questionAnswers : byQuestion.values()) {
            Answer sample = questionAnswers.get(0);
            BigDecimal total = questionAnswers.stream()
                    .map(Answer::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = total.divide(
                    BigDecimal.valueOf(questionAnswers.size()),
                    2,
                    RoundingMode.HALF_UP
            );
            averages.add(new AiSummaryInput.QuestionAverage(
                    sample.getQuestion().getText(),
                    average,
                    questionAnswers.size()
            ));
        }
        averages.sort(Comparator.comparing(AiSummaryInput.QuestionAverage::questionText));
        return averages;
    }

    private Map<Integer, Long> calculateScoreDistribution(List<Answer> answers) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int score = 1; score <= 5; score++) {
            distribution.put(score, 0L);
        }
        for (Answer answer : answers) {
            if (answer.getScore() == null || answer.getQuestion().getType() != QuestionType.RATING) {
                continue;
            }
            int scoreValue = answer.getScore().setScale(0, RoundingMode.HALF_UP).intValue();
            if (scoreValue >= 1 && scoreValue <= 5) {
                distribution.merge(scoreValue, 1L, Long::sum);
            }
        }
        return distribution;
    }

    private List<String> extractMaskedComments(List<Answer> answers) {
        return answers.stream()
                .filter(answer -> answer.getQuestion().getType() == QuestionType.TEXT)
                .map(Answer::getAnswerValue)
                .filter(value -> value != null && !value.isBlank() && !"-".equals(value.trim()))
                .map(PiiMasker::mask)
                .filter(masked -> !masked.isBlank())
                .toList();
    }

    private void validateDateRange(AISummaryRequest request) {
        if (request == null || request.getDepartmentId() == null
                || request.getDateFrom() == null || request.getDateTo() == null) {
            throw new BadRequestException("AI summary filters are required.");
        }
        if (request.getDateTo().isBefore(request.getDateFrom())) {
            throw new BadRequestException("Summary end date cannot be before start date.");
        }
    }

    private StaffUser findActiveStaffUser(String username) {
        return staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
    }

    private void authorizeDepartment(StaffUser actor, Department department) {
        if (actor.getRole() == StaffRole.STAFF
                && (actor.getDepartment() == null
                || !actor.getDepartment().getId().equals(department.getId()))) {
            throw new ForbiddenOperationException("Staff users can only summarize their own department.");
        }
    }

    private static boolean hasAnalyzableData(AiSummaryInput input) {
        return input.submissionCount() > 0
                || input.commentCount() > 0
                || !input.questionAverages().isEmpty();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static AISummaryResponse toResponse(AISummary summary) {
        return new AISummaryResponse(
                summary.getId(),
                summary.getDepartment().getId(),
                summary.getDepartment().getName(),
                summary.getCreatedBy() != null ? summary.getCreatedBy().getFullName() : null,
                summary.getDateFrom(),
                summary.getDateTo(),
                summary.isLowScoreOnly(),
                summary.getSurveyVersionScope(),
                summary.getSummaryText(),
                summary.getModelName(),
                summary.getStatus(),
                summary.getRetryCount(),
                summary.getSubmissionCount(),
                summary.getErrorMessage(),
                summary.getCreatedAt(),
                summary.getUpdatedAt()
        );
    }

    private record FilteredData(List<SurveySubmission> submissions, List<Answer> answers) {
    }

    private record PreparedSummary(Department department, StaffUser creator, AiSummaryInput input) {
        boolean hasAnalyzableData() {
            return AISummaryServiceImpl.hasAnalyzableData(input);
        }
    }

    private record PreparedRetry(Long summaryId, AiSummaryInput input) {
    }
}
