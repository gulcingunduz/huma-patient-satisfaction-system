package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.submission.AnswerDetailResponse;
import com.hbys.survey.application.dto.submission.SubmissionDetailResponse;
import com.hbys.survey.application.dto.submission.SubmissionSummaryResponse;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.exception.SubmissionNotFoundException;
import com.hbys.survey.application.service.SubmissionResultsService;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.FeedbackSatisfactionRepository;
import com.hbys.survey.infrastructure.repository.PatientFeedbackEvaluationRepository;
import com.hbys.survey.infrastructure.repository.StaffFeedbackRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionResultsServiceImpl implements SubmissionResultsService {

    private final SurveySubmissionRepository surveySubmissionRepository;
    private final AnswerRepository answerRepository;
    private final StaffUserRepository staffUserRepository;
    private final PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository;
    private final StaffFeedbackRepository staffFeedbackRepository;
    private final FeedbackSatisfactionRepository feedbackSatisfactionRepository;

    public SubmissionResultsServiceImpl(
            SurveySubmissionRepository surveySubmissionRepository,
            AnswerRepository answerRepository,
            StaffUserRepository staffUserRepository,
            PatientFeedbackEvaluationRepository patientFeedbackEvaluationRepository,
            StaffFeedbackRepository staffFeedbackRepository,
            FeedbackSatisfactionRepository feedbackSatisfactionRepository
    ) {
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.answerRepository = answerRepository;
        this.staffUserRepository = staffUserRepository;
        this.patientFeedbackEvaluationRepository = patientFeedbackEvaluationRepository;
        this.staffFeedbackRepository = staffFeedbackRepository;
        this.feedbackSatisfactionRepository = feedbackSatisfactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionSummaryResponse> getSubmissions(String username) {
        StaffUser staffUser = findActiveStaffUser(username);

        List<SurveySubmission> submissions = canSeeAllDepartments(staffUser)
                ? surveySubmissionRepository.findAllByOrderByCreatedAtDesc()
                : surveySubmissionRepository.findByDepartmentAndActiveTrueOrderByCreatedAtDesc(requireDepartment(staffUser));

        return submissions.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionDetail(String username, Long submissionId) {
        StaffUser staffUser = findActiveStaffUser(username);
        SurveySubmission submission = surveySubmissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        assertDepartmentAccess(staffUser, submission);

        List<Answer> answers = answerRepository.findBySubmissionWithQuestion(submission);
        List<AnswerDetailResponse> answerResponses = answers.stream()
                .map(answer -> new AnswerDetailResponse(
                        answer.getQuestion().getId(),
                        answer.getQuestion().getText(),
                        answer.getQuestion().getType(),
                        answer.getAnswerValue(),
                        answer.getScore()
                ))
                .toList();

        SubmissionDetailResponse detail = new SubmissionDetailResponse(
                submission.getId(),
                submission.getDepartment().getId(),
                submission.getDepartment().getName(),
                submission.getOverallScore(),
                submission.getCreatedAt(),
                resolveEvaluationStatus(submission),
                answerResponses
        );

        if (submission.getSurveyVersion() != null) {
            detail.setSurveyVersionNumber(submission.getSurveyVersion().getVersionNumber());
        }

        staffFeedbackRepository.findBySubmission(submission).ifPresent(feedback -> {
            detail.setStaffFeedbackId(feedback.getId());
            detail.setStaffFeedbackAuthorId(feedback.getStaffUser().getId());
            detail.setStaffFeedbackComment(feedback.getComment());
            detail.setStaffFeedbackAuthor(feedback.getStaffUser().getFullName());
            detail.setStaffFeedbackCreatedAt(feedback.getCreatedAt());
        });

        patientFeedbackEvaluationRepository.findBySubmission(submission).ifPresent(evaluation -> {
            detail.setEvaluationStatus(evaluation.getStatus());
            detail.setEvaluationNotes(evaluation.getNotes());
            detail.setEvaluatedByName(evaluation.getEvaluatedBy().getFullName());
            detail.setEvaluationCreatedAt(evaluation.getCreatedAt());
        });

        feedbackSatisfactionRepository.findBySubmission(submission).ifPresent(satisfaction -> {
            detail.setPatientSatisfaction(satisfaction.getSatisfied());
            detail.setPatientSatisfactionCreatedAt(satisfaction.getCreatedAt());
        });

        return detail;
    }

    private SubmissionSummaryResponse toSummary(SurveySubmission submission) {
        return new SubmissionSummaryResponse(
                submission.getId(),
                submission.getDepartment().getId(),
                submission.getDepartment().getName(),
                submission.getOverallScore(),
                submission.getCreatedAt(),
                resolveEvaluationStatus(submission)
        );
    }

    private PatientFeedbackEvaluationStatus resolveEvaluationStatus(SurveySubmission submission) {
        return patientFeedbackEvaluationRepository.findBySubmission(submission)
                .map(evaluation -> evaluation.getStatus())
                .orElse(null);
    }

    private void assertDepartmentAccess(StaffUser staffUser, SurveySubmission submission) {
        if (canSeeAllDepartments(staffUser)) {
            return;
        }
        Department department = requireDepartment(staffUser);
        if (!department.getId().equals(submission.getDepartment().getId())) {
            throw new ForbiddenOperationException("You do not have access to this department's submissions.");
        }
    }

    private boolean canSeeAllDepartments(StaffUser staffUser) {
        return staffUser.getRole() == StaffRole.ADMIN || staffUser.getRole() == StaffRole.PATIENT_RIGHTS_OFFICER;
    }

    private Department requireDepartment(StaffUser staffUser) {
        if (staffUser.getDepartment() == null) {
            throw new ForbiddenOperationException("Your account is not assigned to a department.");
        }
        return staffUser.getDepartment();
    }

    private StaffUser findActiveStaffUser(String username) {
        return staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
    }
}
