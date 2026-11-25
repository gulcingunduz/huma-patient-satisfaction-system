package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.submission.FeedbackSatisfactionRequest;
import com.hbys.survey.application.dto.submission.FeedbackSatisfactionResponse;
import com.hbys.survey.application.dto.submission.PublishedFeedbackResponse;
import com.hbys.survey.application.dto.submission.SurveySubmissionRequestDto;
import com.hbys.survey.application.dto.submission.SurveySubmissionResponse;
import com.hbys.survey.application.dto.submission.TrackingStatusResponse;

public interface SurveyService {

    SurveySubmissionResponse submitSurvey(
            SurveySubmissionRequestDto request,
            String publicAccessToken,
            String clientCookieValue
    );

    TrackingStatusResponse getTrackingStatus(String trackingNumber);

    PublishedFeedbackResponse getPublishedFeedback(String trackingNumber);

    FeedbackSatisfactionResponse submitFeedbackSatisfaction(String trackingNumber, FeedbackSatisfactionRequest request);
}