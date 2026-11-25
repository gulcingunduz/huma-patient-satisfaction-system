package com.hbys.survey.application.dto.submission;

import java.time.Instant;

public class PublishedFeedbackResponse {

    private String trackingNumber;
    private String comment;
    private Instant publishedAt;

    public PublishedFeedbackResponse() {
    }

    public PublishedFeedbackResponse(String trackingNumber, String comment, Instant publishedAt) {
        this.trackingNumber = trackingNumber;
        this.comment = comment;
        this.publishedAt = publishedAt;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}