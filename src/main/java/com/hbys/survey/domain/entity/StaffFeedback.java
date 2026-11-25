package com.hbys.survey.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "staff_feedbacks")
@NoArgsConstructor
public class StaffFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private StaffUser staffUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SurveySubmission submission;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StaffFeedback(StaffUser staffUser, SurveySubmission submission, String comment, BigDecimal score) {
        this.staffUser = staffUser;
        this.submission = submission;
        this.comment = comment;
        this.score = score;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public StaffUser getStaffUser() {
        return staffUser;
    }

    public SurveySubmission getSubmission() {
        return submission;
    }

    public String getComment() {
        return comment;
    }

    public void updateComment(String comment, BigDecimal score) {
        this.comment = comment;
        this.score = score;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}