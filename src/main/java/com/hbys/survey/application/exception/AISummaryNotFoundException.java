package com.hbys.survey.application.exception;

public class AISummaryNotFoundException extends ResourceNotFoundException {

    public AISummaryNotFoundException() {
        super("AISummary", "AI summary was not found.");
    }
}
