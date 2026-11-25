package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.ai.AISummaryRequest;
import com.hbys.survey.application.dto.ai.AISummaryResponse;

import java.util.List;

public interface AISummaryService {

    AISummaryResponse createSummary(AISummaryRequest request, String username);

    AISummaryResponse retrySummary(Long summaryId, String username);

    AISummaryResponse getSummary(Long summaryId, String username);

    List<AISummaryResponse> listSummaries(Long departmentId, String username);
}
