package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.submission.SubmissionDetailResponse;
import com.hbys.survey.application.dto.submission.SubmissionSummaryResponse;

import java.util.List;

public interface SubmissionResultsService {

    List<SubmissionSummaryResponse> getSubmissions(String username);

    SubmissionDetailResponse getSubmissionDetail(String username, Long submissionId);
}