package com.hbys.survey.application.exception;

public class SurveyNotFoundException extends ResourceNotFoundException {

    public SurveyNotFoundException() {
        super("Survey", "Survey was not found.");
    }

    public SurveyNotFoundException(String message) {
        super("Survey", message);
    }
}
