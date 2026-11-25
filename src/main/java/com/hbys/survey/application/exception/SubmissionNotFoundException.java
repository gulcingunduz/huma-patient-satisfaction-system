package com.hbys.survey.application.exception;

public class SubmissionNotFoundException extends ResourceNotFoundException {

    public SubmissionNotFoundException() {
        super("Submission", "Survey submission was not found.");
    }
}