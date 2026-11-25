package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.SatisfactionLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class SubmissionDetailResponse {

    private Long submissionId;
    private Long departmentId;
    private String departmentName;
    private Integer surveyVersionNumber;
    private BigDecimal overallScore;
    private Instant createdAt;
    private PatientFeedbackEvaluationStatus evaluationStatus;
    private String evaluationNotes;
    private String evaluatedByName;
    private Instant evaluationCreatedAt;
    private Long staffFeedbackId;
    private Long staffFeedbackAuthorId;
    private String staffFeedbackComment;
    private String staffFeedbackAuthor;
    private Instant staffFeedbackCreatedAt;
    private SatisfactionLevel patientSatisfaction;
    private Instant patientSatisfactionCreatedAt;
    private List<AnswerDetailResponse> answers;

    public SubmissionDetailResponse() {
    }

    public SubmissionDetailResponse(
            Long submissionId,
            Long departmentId,
            String departmentName,
            BigDecimal overallScore,
            Instant createdAt,
            PatientFeedbackEvaluationStatus evaluationStatus,
            List<AnswerDetailResponse> answers
    ) {
        this.submissionId = submissionId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.overallScore = overallScore;
        this.createdAt = createdAt;
        this.evaluationStatus = evaluationStatus;
        this.answers = answers;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getSurveyVersionNumber() {
        return surveyVersionNumber;
    }

    public void setSurveyVersionNumber(Integer surveyVersionNumber) {
        this.surveyVersionNumber = surveyVersionNumber;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PatientFeedbackEvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(PatientFeedbackEvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getEvaluationNotes() {
        return evaluationNotes;
    }

    public void setEvaluationNotes(String evaluationNotes) {
        this.evaluationNotes = evaluationNotes;
    }

    public String getEvaluatedByName() {
        return evaluatedByName;
    }

    public void setEvaluatedByName(String evaluatedByName) {
        this.evaluatedByName = evaluatedByName;
    }

    public Instant getEvaluationCreatedAt() {
        return evaluationCreatedAt;
    }

    public void setEvaluationCreatedAt(Instant evaluationCreatedAt) {
        this.evaluationCreatedAt = evaluationCreatedAt;
    }

    public Long getStaffFeedbackId() {
        return staffFeedbackId;
    }

    public void setStaffFeedbackId(Long staffFeedbackId) {
        this.staffFeedbackId = staffFeedbackId;
    }

    public Long getStaffFeedbackAuthorId() {
        return staffFeedbackAuthorId;
    }

    public void setStaffFeedbackAuthorId(Long staffFeedbackAuthorId) {
        this.staffFeedbackAuthorId = staffFeedbackAuthorId;
    }

    public String getStaffFeedbackComment() {
        return staffFeedbackComment;
    }

    public void setStaffFeedbackComment(String staffFeedbackComment) {
        this.staffFeedbackComment = staffFeedbackComment;
    }

    public String getStaffFeedbackAuthor() {
        return staffFeedbackAuthor;
    }

    public void setStaffFeedbackAuthor(String staffFeedbackAuthor) {
        this.staffFeedbackAuthor = staffFeedbackAuthor;
    }

    public Instant getStaffFeedbackCreatedAt() {
        return staffFeedbackCreatedAt;
    }

    public void setStaffFeedbackCreatedAt(Instant staffFeedbackCreatedAt) {
        this.staffFeedbackCreatedAt = staffFeedbackCreatedAt;
    }

    public SatisfactionLevel getPatientSatisfaction() {
        return patientSatisfaction;
    }

    public void setPatientSatisfaction(SatisfactionLevel patientSatisfaction) {
        this.patientSatisfaction = patientSatisfaction;
    }

    public Instant getPatientSatisfactionCreatedAt() {
        return patientSatisfactionCreatedAt;
    }

    public void setPatientSatisfactionCreatedAt(Instant patientSatisfactionCreatedAt) {
        this.patientSatisfactionCreatedAt = patientSatisfactionCreatedAt;
    }

    public List<AnswerDetailResponse> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerDetailResponse> answers) {
        this.answers = answers;
    }
}
