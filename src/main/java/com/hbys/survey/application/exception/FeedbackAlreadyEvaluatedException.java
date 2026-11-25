package com.hbys.survey.application.exception;

public class FeedbackAlreadyEvaluatedException extends RuntimeException {

    public FeedbackAlreadyEvaluatedException() {
        super("This feedback has already been evaluated by the patient.");
    }
}