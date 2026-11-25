package com.hbys.survey.application.exception;

public class FeedbackNotFoundException extends ResourceNotFoundException {

    public FeedbackNotFoundException() {
        super("Feedback", "Feedback was not found.");
    }
}
