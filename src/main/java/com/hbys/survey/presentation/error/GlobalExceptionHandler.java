package com.hbys.survey.presentation.error;

import com.hbys.survey.application.exception.AIServiceException;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.DuplicateSubmissionException;
import com.hbys.survey.application.exception.FeedbackAlreadyEvaluatedException;
import com.hbys.survey.application.exception.ForbiddenOperationException;
import com.hbys.survey.application.exception.InvalidSubmissionException;
import com.hbys.survey.application.exception.InvalidSurveyAccessException;
import com.hbys.survey.application.exception.ResourceNotFoundException;
import com.hbys.survey.application.exception.SurveyInactiveException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                exception.getResource().toUpperCase() + "_NOT_FOUND",
                null
        );
    }

    @ExceptionHandler({DuplicateSubmissionException.class, FeedbackAlreadyEvaluatedException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        String code = exception instanceof FeedbackAlreadyEvaluatedException
                ? "FEEDBACK_ALREADY_EVALUATED"
                : "DUPLICATE_SUBMISSION";
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, code, null);
    }

    @ExceptionHandler({InvalidSurveyAccessException.class, ForbiddenOperationException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request, "FORBIDDEN", null);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required.", request, "UNAUTHORIZED", null);
    }

    @ExceptionHandler(SurveyInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleSurveyInactive(
            SurveyInactiveException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.GONE, exception.getMessage(), request, "SURVEY_INACTIVE", null);
    }

    @ExceptionHandler({
            InvalidSubmissionException.class,
            BadRequestException.class,
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, resolveMessage(exception), request, "BAD_REQUEST", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldErrorDetail(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
                ))
                .toList();
        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed.",
                request,
                "VALIDATION_ERROR",
                details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldErrorDetail> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed.",
                request,
                "VALIDATION_ERROR",
                details
        );
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleAiService(
            AIServiceException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage() == null ? "AI service failed." : exception.getMessage(),
                request,
                "AI_SERVICE_FAILURE",
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + exception.getMethod() + "' is not supported for this endpoint.",
                request,
                "METHOD_NOT_ALLOWED",
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unexpected request failure: {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                "INTERNAL_ERROR",
                null
        );
    }

    private String resolveMessage(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed.";
        }
        if (exception instanceof MissingServletRequestParameterException missing) {
            return "Required request parameter '" + missing.getParameterName() + "' is missing.";
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Request parameter '" + mismatch.getName() + "' has an invalid value.";
        }
        return exception.getMessage() == null ? "Bad request." : exception.getMessage();
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            String code,
            List<ApiErrorResponse.FieldErrorDetail> details
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                code,
                details
        );
        return ResponseEntity.status(status).body(response);
    }
}