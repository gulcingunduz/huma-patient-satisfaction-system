package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.survey.PublicSurveyInfoResponse;
import com.hbys.survey.application.dto.survey.QuestionCreateRequestDto;
import com.hbys.survey.application.dto.survey.QuestionResponseDto;
import com.hbys.survey.application.dto.survey.QuestionUpdateRequestDto;
import com.hbys.survey.application.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/public/surveys/access/{publicAccessToken}")
    public PublicSurveyInfoResponse getPublicSurveyInfo(@PathVariable String publicAccessToken) {
        return questionService.getPublicSurveyInfo(publicAccessToken);
    }

    @GetMapping("/public/surveys/access/{publicAccessToken}/questions")
    public List<QuestionResponseDto> getPublicQuestions(@PathVariable String publicAccessToken) {
        return questionService.getActiveQuestionsForPublicAccess(publicAccessToken);
    }

    @GetMapping("/admin/surveys/{surveyId}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QuestionResponseDto> getSurveyQuestions(
            @PathVariable Long surveyId,
            @RequestParam(required = false) Long versionId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return questionService.getQuestionsForSurvey(surveyId, versionId, includeInactive);
    }

    @PostMapping("/admin/surveys/{surveyId}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @PathVariable Long surveyId,
            @Valid @RequestBody QuestionCreateRequestDto request
    ) {
        return ResponseEntity.status(201).body(questionService.createQuestion(surveyId, request));
    }

    @PutMapping("/admin/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public QuestionResponseDto updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateRequestDto request
    ) {
        return questionService.updateQuestion(questionId, request);
    }

    @PatchMapping("/admin/questions/{questionId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public QuestionResponseDto deactivateQuestion(@PathVariable Long questionId) {
        return questionService.deactivateQuestion(questionId);
    }
}
