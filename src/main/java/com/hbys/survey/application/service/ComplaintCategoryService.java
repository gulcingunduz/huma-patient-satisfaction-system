package com.hbys.survey.application.service;

import java.util.List;

public interface ComplaintCategoryService {

    String categorize(String text);

    default List<String> categorizeAll(String text) {
        String primary = categorize(text);
        if (primary == null || "Diğer".equals(primary)) {
            return List.of();
        }
        return List.of(primary);
    }
}
