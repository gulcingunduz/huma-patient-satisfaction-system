package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.staff.StaffFeedbackRequest;
import com.hbys.survey.application.dto.staff.StaffFeedbackResponse;

public interface StaffFeedbackService {

    StaffFeedbackResponse createFeedback(String username, StaffFeedbackRequest request);

    StaffFeedbackResponse updateFeedback(String username, Long feedbackId, StaffFeedbackRequest request);

    StaffFeedbackResponse getFeedbackForSubmission(String username, Long submissionId);
}