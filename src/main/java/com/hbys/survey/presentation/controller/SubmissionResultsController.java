package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.submission.SubmissionDetailResponse;
import com.hbys.survey.application.dto.submission.SubmissionSummaryResponse;
import com.hbys.survey.application.service.SubmissionResultsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/submissions")
@PreAuthorize("hasAnyRole('STAFF','ADMIN','PATIENT_RIGHTS_OFFICER')")
public class SubmissionResultsController {

    private final SubmissionResultsService submissionResultsService;

    public SubmissionResultsController(SubmissionResultsService submissionResultsService) {
        this.submissionResultsService = submissionResultsService;
    }

    @GetMapping
    public List<SubmissionSummaryResponse> getSubmissions(Authentication authentication) {
        return submissionResultsService.getSubmissions(authentication.getName());
    }

    @GetMapping("/{submissionId}")
    public SubmissionDetailResponse getSubmissionDetail(
            Authentication authentication,
            @PathVariable Long submissionId
    ) {
        return submissionResultsService.getSubmissionDetail(authentication.getName(), submissionId);
    }
}