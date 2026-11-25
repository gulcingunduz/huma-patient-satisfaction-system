package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.staff.StaffFeedbackRequest;
import com.hbys.survey.application.dto.staff.StaffFeedbackResponse;
import com.hbys.survey.application.service.StaffFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/feedback")
public class StaffFeedbackController {

    private final StaffFeedbackService staffFeedbackService;

    public StaffFeedbackController(StaffFeedbackService staffFeedbackService) {
        this.staffFeedbackService = staffFeedbackService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<StaffFeedbackResponse> createFeedback(
            Authentication authentication,
            @Valid @RequestBody StaffFeedbackRequest request
    ) {
        StaffFeedbackResponse response = staffFeedbackService.createFeedback(authentication.getName(), request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{feedbackId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public StaffFeedbackResponse updateFeedback(
            Authentication authentication,
            @PathVariable Long feedbackId,
            @Valid @RequestBody StaffFeedbackRequest request
    ) {
        return staffFeedbackService.updateFeedback(authentication.getName(), feedbackId, request);
    }

    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','PATIENT_RIGHTS_OFFICER')")
    public StaffFeedbackResponse getFeedbackForSubmission(
            Authentication authentication,
            @PathVariable Long submissionId
    ) {
        return staffFeedbackService.getFeedbackForSubmission(authentication.getName(), submissionId);
    }
}
