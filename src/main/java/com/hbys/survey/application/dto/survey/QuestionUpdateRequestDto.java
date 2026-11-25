package com.hbys.survey.application.dto.survey;

import com.hbys.survey.domain.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class QuestionUpdateRequestDto {

    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 500, message = "Question text must be at most 500 characters")
    private String text;

    @NotNull(message = "Question type is required")
    private QuestionType type;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    private boolean required;
    private boolean active;

    private com.hbys.survey.domain.entity.QuestionScope scope = com.hbys.survey.domain.entity.QuestionScope.COMMON;

    private Long departmentId;

    private String metricCode;

    public QuestionUpdateRequestDto() {
    }

    public QuestionUpdateRequestDto(String text, QuestionType type, Integer displayOrder, boolean required, boolean active) {
        this.text = text;
        this.type = type;
        this.displayOrder = displayOrder;
        this.required = required;
        this.active = active;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public com.hbys.survey.domain.entity.QuestionScope getScope() {
        return scope;
    }

    public void setScope(com.hbys.survey.domain.entity.QuestionScope scope) {
        this.scope = scope;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }
}
