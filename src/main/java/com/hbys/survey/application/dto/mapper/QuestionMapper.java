package com.hbys.survey.application.dto.mapper;

import com.hbys.survey.application.dto.survey.QuestionCreateRequestDto;
import com.hbys.survey.application.dto.survey.QuestionResponseDto;
import com.hbys.survey.application.dto.survey.QuestionUpdateRequestDto;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionScope;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Survey;

public final class QuestionMapper {

    private QuestionMapper() {
    }

    public static Question toEntity(QuestionCreateRequestDto dto, Survey survey) {
        if (dto == null) {
            return null;
        }
        Question question = new Question(survey, dto.getText(), dto.getType(), dto.getDisplayOrder(), dto.isRequired());
        question.updateScope(dto.getScope() == null ? QuestionScope.COMMON : dto.getScope(), null, dto.getMetricCode());
        return question;
    }

    public static Question toEntity(QuestionCreateRequestDto dto, Survey survey, Department department) {
        if (dto == null) {
            return null;
        }
        Question question = new Question(survey, dto.getText(), dto.getType(), dto.getDisplayOrder(), dto.isRequired());
        QuestionScope scope = dto.getScope() == null ? QuestionScope.COMMON : dto.getScope();
        question.updateScope(scope, scope == QuestionScope.DEPARTMENT_SPECIFIC ? department : null, dto.getMetricCode());
        return question;
    }

    public static Question toEntity(QuestionUpdateRequestDto dto, Question existingQuestion, Survey survey) {
        if (dto == null) {
            return null;
        }

        if (existingQuestion == null) {
            return new Question(survey, dto.getText(), dto.getType(), dto.getDisplayOrder(), dto.isRequired());
        }

        existingQuestion.updateDetails(
                dto.getText(),
                dto.getType(),
                dto.getDisplayOrder(),
                dto.isRequired(),
                dto.isActive()
        );
        return existingQuestion;
    }

    public static QuestionResponseDto toResponse(Question question) {
        if (question == null) {
            return null;
        }

        QuestionResponseDto response = new QuestionResponseDto(
                question.getId(),
                question.getSurvey() != null ? question.getSurvey().getId() : null,
                question.getText(),
                question.getType(),
                question.getDisplayOrder(),
                question.isRequired(),
                question.isActive()
        );
        response.setScope(question.getScope());
        response.setDepartmentId(question.getDepartment() == null ? null : question.getDepartment().getId());
        response.setDepartmentName(question.getDepartment() == null ? null : question.getDepartment().getName());
        response.setMetricCode(question.getMetricCode());
        response.setSurveyVersionId(question.getSurveyVersion() == null ? null : question.getSurveyVersion().getId());
        response.setSurveyVersionNumber(question.getSurveyVersion() == null ? null : question.getSurveyVersion().getVersionNumber());
        return response;
    }
}
