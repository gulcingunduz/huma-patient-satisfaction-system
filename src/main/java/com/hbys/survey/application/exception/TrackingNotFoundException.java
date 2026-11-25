package com.hbys.survey.application.exception;

public class TrackingNotFoundException extends ResourceNotFoundException {

    public TrackingNotFoundException() {
        super("Tracking", "Tracking number was not found.");
    }
}
