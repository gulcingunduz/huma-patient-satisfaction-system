package com.hbys.survey.presentation.error;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String code;
    private final List<FieldErrorDetail> details;

    public ApiErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, null, null);
    }

    public ApiErrorResponse(
            int status,
            String error,
            String message,
            String path,
            String code,
            List<FieldErrorDetail> details
    ) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.code = code;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
