package com.hbys.survey.application.exception;

public class StaffUserNotFoundException extends ResourceNotFoundException {

    public StaffUserNotFoundException() {
        super("StaffUser", "Active staff user was not found.");
    }
}
