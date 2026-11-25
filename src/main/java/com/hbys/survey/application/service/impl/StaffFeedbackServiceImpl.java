package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.staff.StaffFeedbackRequest;
import com.hbys.survey.application.dto.staff.StaffFeedbackResponse;
import com.hbys.survey.application.exception.FeedbackNotFoundException;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.exception.SubmissionNotFoundException;
import com.hbys.survey.application.service.StaffFeedbackService;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.StaffFeedback;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.StaffFeedbackRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StaffFeedbackServiceImpl implements StaffFeedbackService {

    private final StaffFeedbackRepository staffFeedbackRepository;
    private final StaffUserRepository staffUserRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository;

    public StaffFeedbackServiceImpl(
            StaffFeedbackRepository staffFeedbackRepository,
            StaffUserRepository staffUserRepository,
            SurveySubmissionRepository surveySubmissionRepository,
            PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository
    ) {
        this.staffFeedbackRepository = staffFeedbackRepository;
        this.staffUserRepository = staffUserRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.patientFeedbackEvaluationRepository = patientFeedbackEvaluationRepository;
    }

    @Override
    @Transactional
    public StaffFeedbackResponse createFeedback(String username, StaffFeedbackRequest request) {
        StaffUser staffUser = findActiveStaffUser(username);
        SurveySubmission submission = surveySubmissionRepository.findById(request.getSubmissionId())
                .orElseThrow(SubmissionNotFoundException::new);

        assertWriteDepartmentAccess(staffUser, submission);

        Optional<StaffFeedback> existingOpt = staffFeedbackRepository.findBySubmission(submission);
        if (existingOpt.isPresent()) {
            StaffFeedback existing = existingOpt.get();
            assertOwnerOrAdmin(staffUser, existing);
            assertNotYetApproved(existing);
            existing.updateComment(request.getComment(), request.getScore());
            return toResponse(existing);
        }

        StaffFeedback feedback = new StaffFeedback(staffUser, submission, request.getComment(), request.getScore());
        StaffFeedback saved = staffFeedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StaffFeedbackResponse updateFeedback(String username, Long feedbackId, StaffFeedbackRequest request) {
        StaffUser staffUser = findActiveStaffUser(username);
        StaffFeedback feedback = staffFeedbackRepository.findById(feedbackId)
                .orElseThrow(FeedbackNotFoundException::new);

        assertOwnerOrAdmin(staffUser, feedback);
        assertNotYetApproved(feedback);

        feedback.updateComment(request.getComment(), request.getScore());
        return toResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffFeedbackResponse getFeedbackForSubmission(String username, Long submissionId) {
        StaffUser staffUser = findActiveStaffUser(username);
        SurveySubmission submission = surveySubmissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        assertReadDepartmentAccess(staffUser, submission);

        StaffFeedback feedback = staffFeedbackRepository.findBySubmission(submission)
                .orElseThrow(FeedbackNotFoundException::new);
        return toResponse(feedback);
    }

    private void assertWriteDepartmentAccess(StaffUser staffUser, SurveySubmission submission) {
        if (staffUser.getRole() == StaffRole.ADMIN) {
            return;
        }
        if (staffUser.getRole() != StaffRole.STAFF
                || staffUser.getDepartment() == null
                || !staffUser.getDepartment().getId().equals(submission.getDepartment().getId())) {
            throw new ForbiddenOperationException("You do not have access to this department's submissions.");
        }
    }

    private void assertReadDepartmentAccess(StaffUser staffUser, SurveySubmission submission) {
        if (staffUser.getRole() == StaffRole.ADMIN || staffUser.getRole() == StaffRole.PATIENT_RIGHTS_OFFICER) {
            return;
        }
        if (staffUser.getDepartment() == null
                || !staffUser.getDepartment().getId().equals(submission.getDepartment().getId())) {
            throw new ForbiddenOperationException("You do not have access to this department's submissions.");
        }
    }

    private void assertOwnerOrAdmin(StaffUser staffUser, StaffFeedback feedback) {
        if (staffUser.getRole() == StaffRole.ADMIN) {
            return;
        }
        if (!feedback.getStaffUser().getId().equals(staffUser.getId())) {
            throw new ForbiddenOperationException("Başka bir personelin oluşturduğu yanıt taslağına müdahale edemezsiniz.");
        }
    }

    private void assertNotYetApproved(StaffFeedback feedback) {
        patientFeedbackEvaluationRepository.findBySubmission(feedback.getSubmission())
                .ifPresent(evaluation -> {
                    if (evaluation.getStatus() == PatientFeedbackEvaluationStatus.APPROVED) {
                        throw new ForbiddenOperationException("Feedback is already approved and cannot be edited.");
                    }
                });
    }

    private StaffUser findActiveStaffUser(String username) {
        return staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
    }

    private static StaffFeedbackResponse toResponse(StaffFeedback feedback) {
        return new StaffFeedbackResponse(
                feedback.getId(),
                feedback.getStaffUser().getId(),
                feedback.getSubmission().getId(),
                feedback.getComment(),
                feedback.getScore(),
                feedback.getCreatedAt()
        );
    }
}
