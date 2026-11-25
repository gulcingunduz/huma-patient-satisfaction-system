package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.ai.AISummaryRequest;
import com.hbys.survey.application.dto.ai.AISummaryResponse;
import com.hbys.survey.application.service.AISummaryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/ai-summaries")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'PATIENT_RIGHTS_OFFICER')")
public class AISummaryController {

    private final AISummaryService aiSummaryService;

    public AISummaryController(AISummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping
    public ResponseEntity<AISummaryResponse> createSummary(
            Authentication authentication,
            @Valid @RequestBody AISummaryRequest request
    ) {
        AISummaryResponse response = aiSummaryService.createSummary(request, authentication.getName());
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{summaryId}/retry")
    public AISummaryResponse retrySummary(
            Authentication authentication,
            @PathVariable Long summaryId
    ) {
        return aiSummaryService.retrySummary(summaryId, authentication.getName());
    }

    @GetMapping("/{summaryId}")
    public AISummaryResponse getSummary(
            Authentication authentication,
            @PathVariable Long summaryId
    ) {
        return aiSummaryService.getSummary(summaryId, authentication.getName());
    }

    @GetMapping
    public List<AISummaryResponse> listSummaries(
            Authentication authentication,
            @RequestParam Long departmentId
    ) {
        return aiSummaryService.listSummaries(departmentId, authentication.getName());
    }
}
