package com.hbys.survey.application.dto.staff;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class StaffFeedbackRequest {

    @NotNull(message = "Submission id is required")
    private Long submissionId;

    @NotBlank(message = "Comment cannot be blank")
    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    private String comment;

    @DecimalMin(value = "1.0", message = "Score must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "Score must be between 1 and 5")
    private BigDecimal score;

    public StaffFeedbackRequest() {
    }

    public StaffFeedbackRequest(Long submissionId, String comment, BigDecimal score) {
        this.submissionId = submissionId;
        this.comment = comment;
        this.score = score;
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
}