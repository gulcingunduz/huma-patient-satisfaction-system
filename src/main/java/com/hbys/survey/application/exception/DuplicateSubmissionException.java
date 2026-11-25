package com.hbys.survey.application.exception;

public class DuplicateSubmissionException extends RuntimeException {

    public DuplicateSubmissionException() {
        super("The same survey cannot be submitted again within 24 hours.");
    }
}
