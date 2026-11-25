package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.SatisfactionLevel;

import java.time.Instant;

public class FeedbackSatisfactionResponse {

    private String trackingNumber;
    private SatisfactionLevel satisfied;
    private Instant createdAt;

    public FeedbackSatisfactionResponse() {
    }

    public FeedbackSatisfactionResponse(String trackingNumber, SatisfactionLevel satisfied, Instant createdAt) {
        this.trackingNumber = trackingNumber;
        this.satisfied = satisfied;
        this.createdAt = createdAt;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public SatisfactionLevel getSatisfied() {
        return satisfied;
    }

    public void setSatisfied(SatisfactionLevel satisfied) {
        this.satisfied = satisfied;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}