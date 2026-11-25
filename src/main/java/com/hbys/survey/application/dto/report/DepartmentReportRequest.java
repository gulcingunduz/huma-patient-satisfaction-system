package com.hbys.survey.application.dto.report;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class DepartmentReportRequest {

    @NotNull
    private Long departmentId;

    @NotNull
    private LocalDate dateFrom;

    @NotNull
    private LocalDate dateTo;

    private boolean lowScoreOnly;

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
