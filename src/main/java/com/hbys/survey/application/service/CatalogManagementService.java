package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.survey.DepartmentCreateRequestDto;
import com.hbys.survey.application.dto.survey.DepartmentResponseDto;
import com.hbys.survey.application.dto.survey.SurveyAccessCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyAccessResponseDto;
import com.hbys.survey.application.dto.survey.SurveyCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyResponseDto;

import java.util.List;

public interface CatalogManagementService {

    List<DepartmentResponseDto> getDepartments(boolean includeInactive);

    List<DepartmentResponseDto> getAccessibleDepartments(String username);

    DepartmentResponseDto createDepartment(DepartmentCreateRequestDto request);

    DepartmentResponseDto updateDepartment(Long departmentId, DepartmentCreateRequestDto request);

    DepartmentResponseDto setDepartmentActive(Long departmentId, boolean active);

    void deleteDepartment(Long departmentId);

    List<SurveyResponseDto> getSurveys(boolean includeInactive);

    SurveyResponseDto createSurvey(SurveyCreateRequestDto request);

    SurveyResponseDto updateSurvey(Long surveyId, SurveyCreateRequestDto request);

    SurveyResponseDto setSurveyActive(Long surveyId, boolean active);

    List<SurveyAccessResponseDto> getSurveyAccesses(Long departmentId);

    SurveyAccessResponseDto createSurveyAccess(SurveyAccessCreateRequestDto request);

    SurveyAccessResponseDto deactivateSurveyAccess(Long accessId);
}