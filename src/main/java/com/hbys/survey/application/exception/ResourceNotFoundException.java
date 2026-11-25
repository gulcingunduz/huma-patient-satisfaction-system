package com.hbys.survey.application.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resource;

    public ResourceNotFoundException(String resource, String message) {
        super(message);
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
