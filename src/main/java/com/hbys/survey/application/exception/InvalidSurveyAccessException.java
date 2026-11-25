package com.hbys.survey.application.exception;

public class InvalidSurveyAccessException extends RuntimeException {

    public InvalidSurveyAccessException() {
        super("The survey access token is invalid or inactive.");
    }
}
