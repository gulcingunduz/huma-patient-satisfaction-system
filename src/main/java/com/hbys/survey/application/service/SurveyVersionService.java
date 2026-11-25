package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.survey.SurveyVersionResponseDto;

import java.util.List;

public interface SurveyVersionService {

    List<SurveyVersionResponseDto> listVersions(Long surveyId);

    SurveyVersionResponseDto createDraft(Long surveyId);

    SurveyVersionResponseDto publishDraft(Long surveyId, Long versionId);

    SurveyVersionResponseDto archiveVersion(Long surveyId, Long versionId);
}
