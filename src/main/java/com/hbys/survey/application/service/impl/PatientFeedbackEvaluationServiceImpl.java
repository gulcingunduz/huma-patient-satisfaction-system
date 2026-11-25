package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationRequest;
import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationResponse;
import com.hbys.survey.application.dto.evaluation.PatientRightsQueueItemResponse;
import com.hbys.survey.application.exception.FeedbackNotFoundException;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.exception.SubmissionNotFoundException;
import com.hbys.survey.application.service.PatientFeedbackEvaluationService;
import com.hbys.survey.domain.entity.FeedbackSatisfaction;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluation;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.StaffFeedback;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.FeedbackSatisfactionRepository;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.StaffFeedbackRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientFeedbackEvaluationServiceImpl implements PatientFeedbackEvaluationService {

    private final PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository;
    private final StaffFeedbackRepository staffFeedbackRepository;
    private final StaffUserRepository staffUserRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final FeedbackSatisfactionRepository feedbackSatisfactionRepository;

    public PatientFeedbackEvaluationServiceImpl(
            PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository,
            StaffFeedbackRepository staffFeedbackRepository,
            StaffUserRepository staffUserRepository,
            SurveySubmissionRepository surveySubmissionRepository,
            FeedbackSatisfactionRepository feedbackSatisfactionRepository
    ) {
        this.patientFeedbackEvaluationRepository = patientFeedbackEvaluationRepository;
        this.staffFeedbackRepository = staffFeedbackRepository;
        this.staffUserRepository = staffUserRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.feedbackSatisfactionRepository = feedbackSatisfactionRepository;
    }

    @Override
    @Transactional
    public PatientFeedbackEvaluationResponse evaluate(String username, PatientFeedbackEvaluationRequest request) {
        StaffUser officer = findActiveOfficer(username);
        SurveySubmission submission = surveySubmissionRepository.findById(request.getSubmissionId())
                .orElseThrow(SubmissionNotFoundException::new);

        staffFeedbackRepository.findBySubmission(submission)
                .orElseThrow(FeedbackNotFoundException::new);

        PatientFeedbackEvaluation evaluation = patientFeedbackEvaluationRepository.findBySubmission(submission)
                .map(existing -> {
                    existing.updateEvaluation(officer, request.getStatus(), request.getNotes());
                    return existing;
                })
                .orElseGet(() -> patientFeedbackEvaluationRepository.save(
                        new PatientFeedbackEvaluation(submission, officer, request.getStatus(), request.getNotes())
                ));

        return toResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientFeedbackEvaluationResponse> getByStatus(PatientFeedbackEvaluationStatus status) {
        return patientFeedbackEvaluationRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(PatientFeedbackEvaluationServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientRightsQueueItemResponse> getEvaluationQueue(PatientFeedbackEvaluationStatus statusFilter) {
        List<StaffFeedback> feedbacks = staffFeedbackRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return feedbacks.stream()
                .map(feedback -> {
                    SurveySubmission submission = feedback.getSubmission();
                    PatientFeedbackEvaluation evaluation = patientFeedbackEvaluationRepository
                            .findBySubmission(submission)
                            .orElse(null);
                    PatientFeedbackEvaluationStatus effectiveStatus = evaluation == null
                            ? PatientFeedbackEvaluationStatus.PENDING
                            : evaluation.getStatus();
                    FeedbackSatisfaction satisfaction = feedbackSatisfactionRepository
                            .findBySubmission(submission)
                            .orElse(null);

                    return new PatientRightsQueueItemResponse(
                            submission.getId(),
                            submission.getDepartment().getId(),
                            submission.getDepartment().getName(),
                            submission.getOverallScore(),
                            submission.getCreatedAt(),
                            feedback.getId(),
                            feedback.getComment(),
                            feedback.getStaffUser().getFullName(),
                            feedback.getCreatedAt(),
                            effectiveStatus,
                            evaluation == null ? null : evaluation.getNotes(),
                            evaluation == null ? null : evaluation.getEvaluatedBy().getFullName(),
                            evaluation == null ? null : evaluation.getCreatedAt(),
                            satisfaction == null ? null : satisfaction.getSatisfied(),
                            satisfaction == null ? null : satisfaction.getCreatedAt()
                    );
                })
                .filter(item -> statusFilter == null || item.evaluationStatus() == statusFilter)
                .toList();
    }

    private StaffUser findActiveOfficer(String username) {
        StaffUser staffUser = staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
        if (staffUser.getRole() != StaffRole.PATIENT_RIGHTS_OFFICER && staffUser.getRole() != StaffRole.ADMIN) {
            throw new ForbiddenOperationException("Only patient rights officers can evaluate feedback.");
        }
        return staffUser;
    }

    private static PatientFeedbackEvaluationResponse toResponse(PatientFeedbackEvaluation evaluation) {
        return new PatientFeedbackEvaluationResponse(
                evaluation.getId(),
                evaluation.getSubmission().getId(),
                evaluation.getEvaluatedBy().getId(),
                evaluation.getStatus(),
                evaluation.getNotes(),
                evaluation.getCreatedAt()
        );
    }
}
