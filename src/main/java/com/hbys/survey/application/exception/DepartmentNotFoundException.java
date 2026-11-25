package com.hbys.survey.application.exception;

public class DepartmentNotFoundException extends ResourceNotFoundException {

    public DepartmentNotFoundException() {
        super("Department", "Department was not found.");
    }
}
