package com.hbys.survey.application.dto.submission;

import com.hbys.survey.domain.entity.SatisfactionLevel;
import jakarta.validation.constraints.NotNull;

public class FeedbackSatisfactionRequest {

    @NotNull(message = "Satisfaction level is required")
    private SatisfactionLevel satisfied;

    public FeedbackSatisfactionRequest() {
    }

    public FeedbackSatisfactionRequest(SatisfactionLevel satisfied) {
        this.satisfied = satisfied;
    }

    public SatisfactionLevel getSatisfied() {
        return satisfied;
    }

    public void setSatisfied(SatisfactionLevel satisfied) {
        this.satisfied = satisfied;
    }
}