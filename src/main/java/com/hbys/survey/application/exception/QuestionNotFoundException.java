package com.hbys.survey.application.exception;

public class QuestionNotFoundException extends ResourceNotFoundException {

    public QuestionNotFoundException() {
        super("Question", "Question was not found.");
    }
}
