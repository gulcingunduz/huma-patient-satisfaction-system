package com.hbys.survey.application.dto.mapper;

import com.hbys.survey.application.dto.survey.SurveyCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyResponseDto;
import com.hbys.survey.domain.entity.Survey;

public final class SurveyMapper {

    private SurveyMapper() {
    }

    public static Survey toEntity(SurveyCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return new Survey(dto.getTitle());
    }

    public static SurveyResponseDto toResponse(Survey survey) {
        if (survey == null) {
            return null;
        }
        return new SurveyResponseDto(
                survey.getId(),
                survey.getTitle(),
                survey.isActive(),
                survey.getCreatedAt()
        );
    }
}
