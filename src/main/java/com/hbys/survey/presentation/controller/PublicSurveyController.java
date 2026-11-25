package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.submission.FeedbackSatisfactionRequest;
import com.hbys.survey.application.dto.submission.FeedbackSatisfactionResponse;
import com.hbys.survey.application.dto.submission.PublishedFeedbackResponse;
import com.hbys.survey.application.dto.submission.SurveySubmissionRequestDto;
import com.hbys.survey.application.dto.submission.SurveySubmissionResponse;
import com.hbys.survey.application.dto.submission.TrackingStatusResponse;
import com.hbys.survey.application.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/surveys")
public class PublicSurveyController {

    private static final String CLIENT_COOKIE_NAME = "HBYS_CLIENT_TOKEN";
    private static final Duration CLIENT_COOKIE_MAX_AGE = Duration.ofDays(1);

    private final SurveyService surveyService;
    private final boolean clientCookieSecure;

    public PublicSurveyController(
            SurveyService surveyService,
            @Value("${hbys.security.client-cookie-secure:false}") boolean clientCookieSecure
    ) {
        this.surveyService = surveyService;
        this.clientCookieSecure = clientCookieSecure;
    }

    @PostMapping("/{publicAccessToken}/submissions")
    public ResponseEntity<SurveySubmissionResponse> submitSurvey(
            @PathVariable String publicAccessToken,
            @CookieValue(value = CLIENT_COOKIE_NAME, required = false) String clientCookieValue,
            @Valid @RequestBody SurveySubmissionRequestDto request
    ) {
        boolean newCookie = clientCookieValue == null || clientCookieValue.isBlank();
        String effectiveClientCookie = newCookie ? UUID.randomUUID().toString() : clientCookieValue;

        SurveySubmissionResponse response = surveyService.submitSurvey(
                request,
                publicAccessToken,
                effectiveClientCookie
        );

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(201);
        if (newCookie) {
            ResponseCookie cookie = ResponseCookie.from(CLIENT_COOKIE_NAME, effectiveClientCookie)
                    .httpOnly(true)
                    .secure(clientCookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(CLIENT_COOKIE_MAX_AGE)
                    .build();
            responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return responseBuilder.body(response);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public TrackingStatusResponse getTrackingStatus(@PathVariable String trackingNumber) {
        return surveyService.getTrackingStatus(trackingNumber);
    }

    @GetMapping("/tracking/{trackingNumber}/feedback")
    public PublishedFeedbackResponse getPublishedFeedback(@PathVariable String trackingNumber) {
        return surveyService.getPublishedFeedback(trackingNumber);
    }

    @PostMapping("/tracking/{trackingNumber}/feedback/satisfaction")
    public FeedbackSatisfactionResponse submitFeedbackSatisfaction(
            @PathVariable String trackingNumber,
            @Valid @RequestBody FeedbackSatisfactionRequest request
    ) {
        return surveyService.submitFeedbackSatisfaction(trackingNumber, request);
    }
}