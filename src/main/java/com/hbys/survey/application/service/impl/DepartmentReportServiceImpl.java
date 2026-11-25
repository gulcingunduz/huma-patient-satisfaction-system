package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.report.ComplaintCategoryCountResponse;
import com.hbys.survey.application.dto.report.DashboardOverviewResponse;
import com.hbys.survey.application.dto.report.DepartmentReportRequest;
import com.hbys.survey.application.dto.report.DepartmentReportResponse;
import com.hbys.survey.application.dto.report.MetricReportResponse;
import com.hbys.survey.application.dto.report.TrendPointResponse;
import com.hbys.survey.application.exception.DepartmentNotFoundException;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.service.ComplaintCategoryService;
import com.hbys.survey.application.service.DepartmentReportService;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionScope;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DepartmentReportServiceImpl implements DepartmentReportService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");
    private static final BigDecimal LOW_SCORE_THRESHOLD = BigDecimal.valueOf(3);
    private static final BigDecimal CRITICAL_SCORE_THRESHOLD = BigDecimal.valueOf(2);
    static final int MINIMUM_DATA_THRESHOLD = 3;

    private final DepartmentRepository departmentRepository;
    private final StaffUserRepository staffUserRepository;
    private final SurveySubmissionRepository submissionRepository;
    private final AnswerRepository answerRepository;
    private final ComplaintCategoryService complaintCategoryService;
    private final PatientFeedbackEvaluationRepository evaluationRepository;

    public DepartmentReportServiceImpl(
            DepartmentRepository departmentRepository,
            StaffUserRepository staffUserRepository,
            SurveySubmissionRepository submissionRepository,
            AnswerRepository answerRepository,
            ComplaintCategoryService complaintCategoryService,
            PatientFeedbackEvaluationRepository evaluationRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.staffUserRepository = staffUserRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.complaintCategoryService = complaintCategoryService;
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentReportResponse generateReport(DepartmentReportRequest request, String username) {
        validate(request);
        StaffUser staffUser = staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(DepartmentNotFoundException::new);
        authorize(staffUser, department);

        Instant from = startOfBusinessDay(request.getDateFrom());
        Instant to = endOfBusinessDay(request.getDateTo());
        List<SurveySubmission> submissions = submissionRepository
                .findByDepartmentAndCreatedAtBetween(department, from, to)
                .stream()
                .filter(submission -> !request.isLowScoreOnly()
                        || submission.getOverallScore().compareTo(LOW_SCORE_THRESHOLD) <= 0)
                .toList();

        if (submissions.isEmpty()) {
            return new DepartmentReportResponse(
                    department.getId(),
                    department.getName(),
                    request.getDateFrom(),
                    request.getDateTo(),
                    0,
                    0,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    false,
                    MINIMUM_DATA_THRESHOLD,
                    scoreDistribution(List.of()),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<Long> submissionIds = submissions.stream().map(SurveySubmission::getId).toList();
        List<Answer> answers = answerRepository.findBySubmissionIdInWithQuestion(submissionIds);
        BigDecimal overallAverage = calculateOverallAverage(submissions);
        long lowScoreCount = countScoresAtOrBelow(submissions, LOW_SCORE_THRESHOLD);
        Map<Integer, Long> distribution = scoreDistribution(answers);

        List<MetricReportResponse> commonMetrics = calculateMetrics(
                answers, QuestionScope.COMMON, department
        );
        List<MetricReportResponse> departmentMetrics = calculateMetrics(
                answers, QuestionScope.DEPARTMENT_SPECIFIC, department
        );
        List<ComplaintCategoryCountResponse> complaints = calculateComplaints(answers);
        List<TrendPointResponse> trend = calculateTrend(submissions);

        return new DepartmentReportResponse(
                department.getId(),
                department.getName(),
                request.getDateFrom(),
                request.getDateTo(),
                submissions.size(),
                answers.size(),
                lowScoreCount,
                overallAverage,
                submissions.size() >= MINIMUM_DATA_THRESHOLD,
                MINIMUM_DATA_THRESHOLD,
                distribution,
                commonMetrics,
                departmentMetrics,
                complaints,
                trend
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview(
            Long departmentId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String username
    ) {
        StaffUser staffUser = staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);

        LocalDate effectiveTo = dateTo != null ? dateTo : LocalDate.now(BUSINESS_ZONE);
        LocalDate effectiveFrom = dateFrom != null ? dateFrom : effectiveTo.minusDays(29);
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Report end date cannot be before start date.");
        }

        Department scopedDepartment = null;
        if (staffUser.getRole() == StaffRole.STAFF) {
            if (staffUser.getDepartment() == null) {
                throw new ForbiddenOperationException("Your account is not assigned to a department.");
            }
            if (departmentId != null && !staffUser.getDepartment().getId().equals(departmentId)) {
                throw new ForbiddenOperationException("Staff users can only view their own department.");
            }
            scopedDepartment = staffUser.getDepartment();
        } else if (departmentId != null) {
            scopedDepartment = departmentRepository.findById(departmentId)
                    .orElseThrow(DepartmentNotFoundException::new);
        }

        Instant from = startOfBusinessDay(effectiveFrom);
        Instant to = endOfBusinessDay(effectiveTo);

        List<SurveySubmission> submissions = scopedDepartment != null
                ? submissionRepository.findByDepartmentAndCreatedAtBetween(scopedDepartment, from, to)
                : submissionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);

        long lowScoreCount = countScoresAtOrBelow(submissions, LOW_SCORE_THRESHOLD);
        long criticalScoreCount = countScoresAtOrBelow(submissions, CRITICAL_SCORE_THRESHOLD);
        BigDecimal overallAverage = calculateOverallAverage(submissions);

        long pendingFeedbackCount = submissions.stream()
                .filter(submission -> evaluationRepository.findBySubmission(submission)
                        .map(eval -> eval.getStatus() == PatientFeedbackEvaluationStatus.PENDING
                                || eval.getStatus() == PatientFeedbackEvaluationStatus.REVISION_REQUIRED)
                        .orElse(true))
                .count();

        Map<Long, List<SurveySubmission>> byDepartment = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.getDepartment().getId()));

        List<DashboardOverviewResponse.DepartmentSummaryItem> departmentItems = byDepartment.values().stream()
                .map(deptSubs -> {
                    Department dept = deptSubs.get(0).getDepartment();
                    return new DashboardOverviewResponse.DepartmentSummaryItem(
                            dept.getId(),
                            dept.getName(),
                            deptSubs.size(),
                            calculateOverallAverage(deptSubs),
                            countScoresAtOrBelow(deptSubs, LOW_SCORE_THRESHOLD),
                            deptSubs.size() >= MINIMUM_DATA_THRESHOLD
                    );
                })
                .sorted(Comparator.comparing(DashboardOverviewResponse.DepartmentSummaryItem::departmentName))
                .toList();

        List<Answer> answers = submissions.isEmpty()
                ? List.of()
                : answerRepository.findBySubmissionIdInWithQuestion(
                        submissions.stream().map(SurveySubmission::getId).toList()
                );

        return new DashboardOverviewResponse(
                effectiveFrom,
                effectiveTo,
                submissions.size(),
                overallAverage,
                lowScoreCount,
                criticalScoreCount,
                pendingFeedbackCount,
                submissions.size() >= MINIMUM_DATA_THRESHOLD,
                MINIMUM_DATA_THRESHOLD,
                departmentItems,
                calculateTrend(submissions),
                calculateComplaints(answers)
        );
    }

    private void validate(DepartmentReportRequest request) {
        if (request == null || request.getDepartmentId() == null
                || request.getDateFrom() == null || request.getDateTo() == null) {
            throw new IllegalArgumentException("Department and date range are required.");
        }
        if (request.getDateTo().isBefore(request.getDateFrom())) {
            throw new IllegalArgumentException("Report end date cannot be before start date.");
        }
    }

    private void authorize(StaffUser staffUser, Department department) {
        if (staffUser.getRole() == StaffRole.STAFF
                && (staffUser.getDepartment() == null
                || !staffUser.getDepartment().getId().equals(department.getId()))) {
            throw new ForbiddenOperationException("Staff users can only report on their own department.");
        }
    }

    private static Instant startOfBusinessDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private static Instant endOfBusinessDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant().minusNanos(1);
    }

    private static long countScoresAtOrBelow(List<SurveySubmission> submissions, BigDecimal threshold) {
        return submissions.stream()
                .map(SurveySubmission::getOverallScore)
                .filter(Objects::nonNull)
                .filter(score -> score.compareTo(threshold) <= 0)
                .count();
    }

    private static BigDecimal calculateOverallAverage(List<SurveySubmission> submissions) {
        List<BigDecimal> scores = submissions.stream()
                .map(SurveySubmission::getOverallScore)
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private static List<TrendPointResponse> calculateTrend(List<SurveySubmission> submissions) {
        Map<LocalDate, List<SurveySubmission>> byDay = new TreeMap<>();
        for (SurveySubmission submission : submissions) {
            LocalDate day = submission.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();
            byDay.computeIfAbsent(day, ignored -> new ArrayList<>()).add(submission);
        }
        return byDay.entrySet().stream()
                .map(entry -> new TrendPointResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        calculateOverallAverage(entry.getValue())
                ))
                .toList();
    }

    private static Map<Integer, Long> scoreDistribution(List<Answer> answers) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int score = 1; score <= 5; score++) {
            distribution.put(score, 0L);
        }
        for (Answer answer : answers) {
            if (answer.getScore() == null || answer.getQuestion().getType() != QuestionType.RATING) {
                continue;
            }
            int value = answer.getScore().setScale(0, RoundingMode.HALF_UP).intValue();
            if (value >= 1 && value <= 5) {
                distribution.merge(value, 1L, Long::sum);
            }
        }
        return distribution;
    }

    private List<MetricReportResponse> calculateMetrics(
            List<Answer> answers,
            QuestionScope scope,
            Department department
    ) {
        Map<String, List<Answer>> grouped = answers.stream()
                .filter(answer -> answer.getScore() != null)
                .filter(answer -> answer.getQuestion().getType() == QuestionType.RATING)
                .filter(answer -> answer.getQuestion().getScope() == scope)
                .filter(answer -> scope == QuestionScope.COMMON
                        || (answer.getQuestion().getDepartment() != null
                        && answer.getQuestion().getDepartment().getId().equals(department.getId())))
                .collect(Collectors.groupingBy(answer -> {
                    String code = answer.getQuestion().getMetricCode();
                    return code == null || code.isBlank()
                            ? "QUESTION_" + answer.getQuestion().getId()
                            : code;
                }));

        List<MetricReportResponse> metrics = new ArrayList<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<Answer> metricAnswers = entry.getValue();
                    BigDecimal average = metricAnswers.stream()
                            .map(Answer::getScore)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(metricAnswers.size()), 2, RoundingMode.HALF_UP);
                    Question question = metricAnswers.get(0).getQuestion();
                    metrics.add(new MetricReportResponse(
                            entry.getKey(),
                            question.getText(),
                            average,
                            metricAnswers.size(),
                            scoreDistribution(metricAnswers),
                            metricAnswers.size() >= MINIMUM_DATA_THRESHOLD
                    ));
                });
        metrics.sort(Comparator.comparing(MetricReportResponse::metricCode));
        return metrics;
    }

    private List<ComplaintCategoryCountResponse> calculateComplaints(List<Answer> answers) {
        Map<String, Long> categories = new LinkedHashMap<>();
        for (Answer answer : answers) {
            if (answer.getQuestion().getType() != QuestionType.TEXT) {
                continue;
            }
            List<String> matched = complaintCategoryService.categorizeAll(answer.getAnswerValue());
            if (matched == null || matched.isEmpty()) {
                String single = complaintCategoryService.categorize(answer.getAnswerValue());
                if (single != null && !"Diğer".equals(single)) {
                    categories.merge(single, 1L, Long::sum);
                }
            } else {
                for (String category : matched) {
                    if (category != null && !"Diğer".equals(category)) {
                        categories.merge(category, 1L, Long::sum);
                    }
                }
            }
        }

        return categories.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ComplaintCategoryCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
