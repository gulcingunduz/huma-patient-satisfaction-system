package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.survey.PublicSurveyInfoResponse;
import com.hbys.survey.application.dto.survey.QuestionCreateRequestDto;
import com.hbys.survey.application.dto.survey.QuestionResponseDto;
import com.hbys.survey.application.dto.survey.QuestionUpdateRequestDto;

import java.util.List;

public interface QuestionService {

    QuestionResponseDto createQuestion(Long surveyId, QuestionCreateRequestDto request);

    QuestionResponseDto updateQuestion(Long questionId, QuestionUpdateRequestDto request);

    QuestionResponseDto deactivateQuestion(Long questionId);

    List<QuestionResponseDto> getActiveQuestionsForPublicAccess(String publicAccessToken);

    PublicSurveyInfoResponse getPublicSurveyInfo(String publicAccessToken);

    List<QuestionResponseDto> getQuestionsForSurvey(Long surveyId);

    List<QuestionResponseDto> getQuestionsForSurvey(Long surveyId, Long versionId, boolean includeInactive);
}
