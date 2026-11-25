package com.hbys.survey.application.exception;

public class SurveyInactiveException extends RuntimeException {

    public SurveyInactiveException() {
        super("The survey is inactive and cannot be used.");
    }

    public SurveyInactiveException(String message) {
        super(message);
    }
}
