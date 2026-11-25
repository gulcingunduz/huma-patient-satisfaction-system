package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationRequest;
import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationResponse;
import com.hbys.survey.application.dto.evaluation.PatientRightsQueueItemResponse;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;

import java.util.List;

public interface PatientFeedbackEvaluationService {

    PatientFeedbackEvaluationResponse evaluate(String username, PatientFeedbackEvaluationRequest request);

    List<PatientFeedbackEvaluationResponse> getByStatus(PatientFeedbackEvaluationStatus status);

    List<PatientRightsQueueItemResponse> getEvaluationQueue(PatientFeedbackEvaluationStatus statusFilter);
}
