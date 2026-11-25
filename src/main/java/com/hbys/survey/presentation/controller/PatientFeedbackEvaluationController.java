package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationRequest;
import com.hbys.survey.application.dto.evaluation.PatientFeedbackEvaluationResponse;
import com.hbys.survey.application.dto.evaluation.PatientRightsQueueItemResponse;
import com.hbys.survey.application.service.PatientFeedbackEvaluationService;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient-rights/evaluations")
@PreAuthorize("hasAnyRole('PATIENT_RIGHTS_OFFICER','ADMIN')")
public class PatientFeedbackEvaluationController {

    private final PatientFeedbackEvaluationService patientFeedbackEvaluationService;

    public PatientFeedbackEvaluationController(PatientFeedbackEvaluationService patientFeedbackEvaluationService) {
        this.patientFeedbackEvaluationService = patientFeedbackEvaluationService;
    }

    @PostMapping
    public PatientFeedbackEvaluationResponse evaluate(
            Authentication authentication,
            @Valid @RequestBody PatientFeedbackEvaluationRequest request
    ) {
        return patientFeedbackEvaluationService.evaluate(authentication.getName(), request);
    }

    @GetMapping("/status/{status}")
    public List<PatientFeedbackEvaluationResponse> getByStatus(@PathVariable PatientFeedbackEvaluationStatus status) {
        return patientFeedbackEvaluationService.getByStatus(status);
    }

    @GetMapping("/queue")
    public List<PatientRightsQueueItemResponse> getQueue(
            @RequestParam(required = false) PatientFeedbackEvaluationStatus status
    ) {
        return patientFeedbackEvaluationService.getEvaluationQueue(status);
    }
}
