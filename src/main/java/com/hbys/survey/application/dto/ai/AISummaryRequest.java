package com.hbys.survey.application.dto.ai;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AISummaryRequest {

    @NotNull(message = "Department id is required")
    private Long departmentId;

    @NotNull(message = "Start date is required")
    private LocalDate dateFrom;

    @NotNull(message = "End date is required")
    private LocalDate dateTo;

    private boolean lowScoreOnly;

    public AISummaryRequest() {
    }

    public AISummaryRequest(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
        this.departmentId = departmentId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.lowScoreOnly = false;
    }

    public AISummaryRequest(Long departmentId, LocalDate dateFrom, LocalDate dateTo, boolean lowScoreOnly) {
        this.departmentId = departmentId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.lowScoreOnly = lowScoreOnly;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public boolean isLowScoreOnly() {
        return lowScoreOnly;
    }

    public void setLowScoreOnly(boolean lowScoreOnly) {
        this.lowScoreOnly = lowScoreOnly;
    }
}
