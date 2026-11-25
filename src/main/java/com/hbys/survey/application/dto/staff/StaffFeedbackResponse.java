package com.hbys.survey.application.dto.staff;

import java.math.BigDecimal;
import java.time.Instant;

public class StaffFeedbackResponse {

    private Long id;
    private Long staffUserId;
    private Long submissionId;
    private String comment;
    private BigDecimal score;
    private Instant createdAt;

    public StaffFeedbackResponse() {
    }

    public StaffFeedbackResponse(Long id, Long staffUserId, Long submissionId, String comment, BigDecimal score, Instant createdAt) {
        this.id = id;
        this.staffUserId = staffUserId;
        this.submissionId = submissionId;
        this.comment = comment;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStaffUserId() {
        return staffUserId;
    }

    public void setStaffUserId(Long staffUserId) {
        this.staffUserId = staffUserId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}