package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.survey.SurveyVersionResponseDto;
import com.hbys.survey.application.service.SurveyVersionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/surveys/{surveyId}/versions")
@PreAuthorize("hasRole('ADMIN')")
public class SurveyVersionController {

    private final SurveyVersionService surveyVersionService;

    public SurveyVersionController(SurveyVersionService surveyVersionService) {
        this.surveyVersionService = surveyVersionService;
    }

    @GetMapping
    public List<SurveyVersionResponseDto> listVersions(@PathVariable Long surveyId) {
        return surveyVersionService.listVersions(surveyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyVersionResponseDto createDraft(@PathVariable Long surveyId) {
        return surveyVersionService.createDraft(surveyId);
    }

    @PatchMapping("/{versionId}/publish")
    public SurveyVersionResponseDto publishDraft(
            @PathVariable Long surveyId,
            @PathVariable Long versionId
    ) {
        return surveyVersionService.publishDraft(surveyId, versionId);
    }

    @PatchMapping("/{versionId}/archive")
    public SurveyVersionResponseDto archiveVersion(
            @PathVariable Long surveyId,
            @PathVariable Long versionId
    ) {
        return surveyVersionService.archiveVersion(surveyId, versionId);
    }
}
