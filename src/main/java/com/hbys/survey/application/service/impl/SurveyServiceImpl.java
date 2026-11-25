package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.mapper.SubmissionMapper;
import com.hbys.survey.application.dto.submission.AnswerSubmitRequestDto;
import com.hbys.survey.application.dto.submission.FeedbackSatisfactionRequest;
import com.hbys.survey.application.dto.submission.FeedbackSatisfactionResponse;
import com.hbys.survey.application.dto.submission.PublishedFeedbackResponse;
import com.hbys.survey.application.dto.submission.SurveySubmissionRequestDto;
import com.hbys.survey.application.dto.submission.SurveySubmissionResponse;
import com.hbys.survey.application.dto.submission.TrackingStatusResponse;
import com.hbys.survey.application.exception.DuplicateSubmissionException;
import com.hbys.survey.application.exception.FeedbackAlreadyEvaluatedException;
import com.hbys.survey.application.exception.FeedbackNotFoundException;
import com.hbys.survey.application.exception.InvalidSubmissionException;
import com.hbys.survey.application.exception.InvalidSurveyAccessException;
import com.hbys.survey.application.exception.TrackingNotFoundException;
import com.hbys.survey.application.service.SurveyService;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.FeedbackSatisfaction;
import com.hbys.survey.domain.entity.Notification;
import com.hbys.survey.domain.entity.NotificationType;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluation;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionScope;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.StaffFeedback;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyAccess;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.domain.entity.SurveyVersion;
import com.hbys.survey.domain.entity.SurveyVersionStatus;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.FeedbackSatisfactionRepository;
import com.hbys.survey.infrastructure.repository.NotificationRepository;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.StaffFeedbackRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveyAccessRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import com.hbys.survey.infrastructure.repository.SurveyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final int MAX_TEXT_ANSWER_LENGTH = 500;

    private final SurveyAccessRepository surveyAccessRepository;
    private final QuestionRepository questionRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final AnswerRepository answerRepository;
    private final TokenHashService tokenHashService;
    private final NotificationRepository notificationRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final StaffUserRepository staffUserRepository;
    private final PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository;
    private final StaffFeedbackRepository staffFeedbackRepository;
    private final FeedbackSatisfactionRepository feedbackSatisfactionRepository;

    public SurveyServiceImpl(
            SurveyAccessRepository surveyAccessRepository,
            QuestionRepository questionRepository,
            SurveySubmissionRepository surveySubmissionRepository,
            AnswerRepository answerRepository,
            TokenHashService tokenHashService,
            NotificationRepository notificationRepository,
            SurveyVersionRepository surveyVersionRepository,
            StaffUserRepository staffUserRepository,
            PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository,
            StaffFeedbackRepository staffFeedbackRepository,
            FeedbackSatisfactionRepository feedbackSatisfactionRepository
    ) {
        this.surveyAccessRepository = surveyAccessRepository;
        this.questionRepository = questionRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.answerRepository = answerRepository;
        this.tokenHashService = tokenHashService;
        this.notificationRepository = notificationRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.staffUserRepository = staffUserRepository;
        this.patientFeedbackEvaluationRepository = patientFeedbackEvaluationRepository;
        this.staffFeedbackRepository = staffFeedbackRepository;
        this.feedbackSatisfactionRepository = feedbackSatisfactionRepository;
    }

    @Override
    @Transactional
    public SurveySubmissionResponse submitSurvey(
            SurveySubmissionRequestDto request,
            String publicAccessToken,
            String clientCookieValue
    ) {
        if (request == null) {
            throw new InvalidSubmissionException("Submission request cannot be null.");
        }

        String publicTokenHash = tokenHashService.sha256(publicAccessToken);
        SurveyAccess surveyAccess = surveyAccessRepository
                .findByPublicTokenHashAndActiveTrue(publicTokenHash)
                .filter(access -> matchesRequestedContext(access, request))
                .orElseThrow(InvalidSurveyAccessException::new);

        Survey survey = surveyAccess.getSurvey();
        Department department = surveyAccess.getDepartment();

        Optional<SurveyVersion> publishedVersion = surveyVersionRepository
                .findBySurveyAndStatus(survey, SurveyVersionStatus.PUBLISHED);
        List<Question> questions = publishedVersion
                .map(version -> questionRepository.findPublishedQuestionsBySurveyVersionAndDepartment(
                        survey.getId(),
                        version.getId(),
                        department.getId()
                ))
                .orElseGet(() -> questionRepository.findPublishedQuestionsBySurveyAndDepartment(
                        survey.getId(),
                        department.getId()
                ));

        Map<Long, Question> questionsById = mapQuestions(questions);
        validateAnswers(request.getAnswers(), questionsById, department);

        String clientTokenHash = tokenHashService.sha256(clientCookieValue);
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        if (surveySubmissionRepository.findRecentDuplicateSubmission(
                survey.getId(),
                clientTokenHash,
                since
        ).isPresent()) {
            throw new DuplicateSubmissionException();
        }

        String trackingNumber = createTrackingNumber();
        String trackingTokenHash = tokenHashService.sha256(trackingNumber);
        BigDecimal overallScore = calculateOverallScore(request.getAnswers(), questionsById);

        SurveySubmission submission = SubmissionMapper.toEntity(
                request,
                survey,
                department,
                clientTokenHash,
                trackingTokenHash,
                overallScore,
                publishedVersion.orElse(null)
        );
        SurveySubmission savedSubmission = surveySubmissionRepository.save(submission);

        List<Answer> answers = request.getAnswers().stream()
                .map(answer -> toAnswer(answer, savedSubmission, questionsById.get(answer.getQuestionId())))
                .toList();
        answerRepository.saveAll(answers);
        createScoreNotifications(department, overallScore);

        return SubmissionMapper.toResponse(savedSubmission, trackingNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingStatusResponse getTrackingStatus(String trackingNumber) {
        SurveySubmission submission = findSubmissionByTrackingNumber(trackingNumber);

        return patientFeedbackEvaluationRepository.findBySubmission(submission)
                .map(evaluation -> new TrackingStatusResponse(
                        submission.getId(),
                        trackingNumber,
                        evaluation.getStatus(),
                        evaluation.getCreatedAt(),
                        evaluation.getNotes()
                ))
                .orElseGet(() -> new TrackingStatusResponse(
                        submission.getId(),
                        trackingNumber,
                        PatientFeedbackEvaluationStatus.PENDING,
                        submission.getCreatedAt(),
                        null
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PublishedFeedbackResponse getPublishedFeedback(String trackingNumber) {
        SurveySubmission submission = findSubmissionByTrackingNumber(trackingNumber);
        PatientFeedbackEvaluation evaluation = findApprovedEvaluation(submission);

        StaffFeedback feedback = staffFeedbackRepository.findBySubmission(submission)
                .orElseThrow(FeedbackNotFoundException::new);

        return new PublishedFeedbackResponse(trackingNumber, feedback.getComment(), evaluation.getCreatedAt());
    }

    @Override
    @Transactional
    public FeedbackSatisfactionResponse submitFeedbackSatisfaction(
            String trackingNumber,
            FeedbackSatisfactionRequest request
    ) {
        SurveySubmission submission = findSubmissionByTrackingNumber(trackingNumber);
        findApprovedEvaluation(submission);

        if (feedbackSatisfactionRepository.existsBySubmission(submission)) {
            throw new FeedbackAlreadyEvaluatedException();
        }

        FeedbackSatisfaction saved = feedbackSatisfactionRepository.save(
                new FeedbackSatisfaction(submission, request.getSatisfied())
        );

        return new FeedbackSatisfactionResponse(trackingNumber, saved.getSatisfied(), saved.getCreatedAt());
    }

    private PatientFeedbackEvaluation findApprovedEvaluation(SurveySubmission submission) {
        return patientFeedbackEvaluationRepository.findBySubmission(submission)
                .filter(item -> item.getStatus() == PatientFeedbackEvaluationStatus.APPROVED)
                .orElseThrow(FeedbackNotFoundException::new);
    }

    private SurveySubmission findSubmissionByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new TrackingNotFoundException();
        }
        String trackingTokenHash = tokenHashService.sha256(trackingNumber);
        return surveySubmissionRepository.findByTrackingTokenHash(trackingTokenHash)
                .orElseThrow(TrackingNotFoundException::new);
    }

    private boolean matchesRequestedContext(SurveyAccess access, SurveySubmissionRequestDto request) {
        return access.getSurvey().isActive()
                && access.getDepartment().isActive()
                && access.getSurvey().getId().equals(request.getSurveyId());
    }

    private void createScoreNotifications(Department department, BigDecimal overallScore) {
        if (overallScore.compareTo(BigDecimal.valueOf(3)) > 0) {
            return;
        }

        NotificationType type = overallScore.compareTo(BigDecimal.valueOf(2)) <= 0
                ? NotificationType.SYSTEM_ALERT
                : NotificationType.FEEDBACK_RECEIVED;
        String formattedScore = overallScore.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String message = type == NotificationType.SYSTEM_ALERT
                ? department.getName() + " biriminde kritik düşük puanlı (" + formattedScore + "/5) anket gönderimi alındı."
                : department.getName() + " biriminde düşük puanlı (" + formattedScore + "/5) anket gönderimi alındı.";

        List<StaffUser> recipients = new ArrayList<>(staffUserRepository.findByDepartmentAndActiveTrue(department));
        recipients.addAll(staffUserRepository.findByRoleAndActiveTrueOrderByFullNameAsc(StaffRole.ADMIN));
        notificationRepository.saveAll(
                recipients.stream()
                        .distinct()
                        .map(staffUser -> new Notification(staffUser, type, message))
                        .toList()
        );
    }

    private Map<Long, Question> mapQuestions(List<Question> questions) {
        Map<Long, Question> questionsById = new HashMap<>();
        for (Question question : questions) {
            questionsById.put(question.getId(), question);
        }
        return questionsById;
    }

    private void validateAnswers(
            List<AnswerSubmitRequestDto> answerRequests,
            Map<Long, Question> questionsById,
            Department department
    ) {
        Set<Long> submittedQuestionIds = new HashSet<>();
        for (AnswerSubmitRequestDto answerRequest : answerRequests) {
            if (!submittedQuestionIds.add(answerRequest.getQuestionId())) {
                throw new InvalidSubmissionException("A question cannot be answered more than once.");
            }

            Question question = questionsById.get(answerRequest.getQuestionId());
            if (question == null) {
                throw new InvalidSubmissionException("Answer contains an inactive or unknown question.");
            }

            if (question.getScope() == null
                    || (question.getScope() == QuestionScope.COMMON && question.getDepartment() != null)
                    || (question.getScope() == QuestionScope.DEPARTMENT_SPECIFIC
                    && (question.getDepartment() == null
                    || !question.getDepartment().getId().equals(department.getId())))) {
                throw new InvalidSubmissionException("Question scope does not match the selected department.");
            }

            if (question.getType() == QuestionType.TEXT && answerRequest.getAnswerValue() != null
                    && answerRequest.getAnswerValue().length() > MAX_TEXT_ANSWER_LENGTH) {
                throw new InvalidSubmissionException("Text answer must be at most 500 characters.");
            }

            if (question.isRequired() && question.getType() == QuestionType.TEXT
                    && isBlank(answerRequest.getAnswerValue())) {
                throw new InvalidSubmissionException("A required text question cannot be blank.");
            }

            if (question.isRequired() && question.getType() == QuestionType.RATING
                    && answerRequest.getScore() == null) {
                throw new InvalidSubmissionException("A required rating question must have a score.");
            }

            if (answerRequest.getScore() != null
                    && (answerRequest.getScore().compareTo(BigDecimal.ONE) < 0
                    || answerRequest.getScore().compareTo(BigDecimal.valueOf(5)) > 0)) {
                throw new InvalidSubmissionException("Rating must be between 1 and 5.");
            }
        }

        boolean missingRequiredQuestion = questionsById.values().stream()
                .anyMatch(question -> question.isRequired() && !submittedQuestionIds.contains(question.getId()));
        if (missingRequiredQuestion) {
            throw new InvalidSubmissionException("All required questions must be answered.");
        }
    }

    private BigDecimal calculateOverallScore(
            List<AnswerSubmitRequestDto> answerRequests,
            Map<Long, Question> questionsById
    ) {
        List<BigDecimal> ratingScores = answerRequests.stream()
                .filter(answer -> questionsById.get(answer.getQuestionId()).getType() == QuestionType.RATING)
                .map(AnswerSubmitRequestDto::getScore)
                .filter(score -> score != null)
                .toList();

        if (ratingScores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = ratingScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ratingScores.size()), 2, RoundingMode.HALF_UP);
    }

    private Answer toAnswer(
            AnswerSubmitRequestDto answerRequest,
            SurveySubmission submission,
            Question question
    ) {
        String answerValue = answerRequest.getAnswerValue();
        if (question.getType() == QuestionType.RATING && isBlank(answerValue)) {
            answerValue = answerRequest.getScore().toPlainString();
        }

        if (isBlank(answerValue)) {
            answerValue = "-";
        }

        return new Answer(submission, question, answerValue.trim(), answerRequest.getScore());
    }

    private String createTrackingNumber() {
        return "TRK-" + UUID.randomUUID();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
