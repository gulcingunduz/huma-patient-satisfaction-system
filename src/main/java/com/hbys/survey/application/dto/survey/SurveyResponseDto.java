package com.hbys.survey.application.dto.survey;

import java.time.Instant;

public class SurveyResponseDto {

    private Long id;
    private String title;
    private boolean active;
    private Instant createdAt;

    public SurveyResponseDto() {
    }

    public SurveyResponseDto(Long id, String title, boolean active, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
