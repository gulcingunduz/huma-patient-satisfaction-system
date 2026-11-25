package com.hbys.survey.application.exception;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException() {
        super("Notification", "Notification was not found for the current user.");
    }
}
