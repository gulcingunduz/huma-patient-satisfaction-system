package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.report.DepartmentReportRequest;
import com.hbys.survey.application.dto.report.DepartmentReportResponse;
import com.hbys.survey.application.service.ComplaintCategoryService;
import com.hbys.survey.application.service.impl.DepartmentReportServiceImpl;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionScope;
import com.hbys.survey.domain.entity.QuestionType;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentReportServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StaffUserRepository staffUserRepository;

    @Mock
    private SurveySubmissionRepository submissionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ComplaintCategoryService complaintCategoryService;

    @InjectMocks
    private DepartmentReportServiceImpl reportService;

    @Test
    void calculatesCommonAndDepartmentMetricAveragesAndComplaintCounts() {
        Department department = new Department("Acil Servis");
        setDepartmentId(department, 4L);
        StaffUser staff = new StaffUser("staff", "hash", "Staff", StaffRole.STAFF, department);

        Survey survey = new Survey("General survey");
        SurveySubmission submission = new SurveySubmission(
                survey, department, "client", "tracking", BigDecimal.valueOf(2)
        );
        setSubmissionId(submission, 12L);

        Question communication = new Question(survey, "Communication", QuestionType.RATING, 1, true);
        communication.updateScope(QuestionScope.COMMON, null, "COMMUNICATION");

        Question waiting = new Question(survey, "Waiting", QuestionType.RATING, 2, true);
        waiting.updateScope(QuestionScope.DEPARTMENT_SPECIFIC, department, "WAITING_TIME");

        Question comment = new Question(survey, "Comment", QuestionType.TEXT, 3, false);
        comment.updateScope(QuestionScope.DEPARTMENT_SPECIFIC, department, "COMMENTS");

        Answer communicationAnswer = new Answer(submission, communication, "2", BigDecimal.valueOf(2));
        Answer waitingAnswer = new Answer(submission, waiting, "4", BigDecimal.valueOf(4));
        Answer commentAnswer = new Answer(submission, comment, "The waiting time was too long.", null);

        DepartmentReportRequest request = new DepartmentReportRequest();
        request.setDepartmentId(4L);
        request.setDateFrom(LocalDate.of(2026, 1, 1));
        request.setDateTo(LocalDate.of(2026, 1, 31));

        when(staffUserRepository.findByUsernameAndActiveTrue("staff")).thenReturn(Optional.of(staff));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(department));
        when(submissionRepository.findByDepartmentAndCreatedAtBetween(eq(department), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(submission));
        when(answerRepository.findBySubmissionIdInWithQuestion(List.of(12L)))
                .thenReturn(List.of(communicationAnswer, waitingAnswer, commentAnswer));
        when(complaintCategoryService.categorize("The waiting time was too long."))
                .thenReturn("Bekleme süresi");

        DepartmentReportResponse response = reportService.generateReport(request, "staff");

        assertThat(response.submissionCount()).isEqualTo(1);
        assertThat(response.answerCount()).isEqualTo(3);
        assertThat(response.commonMetrics()).singleElement().extracting(metric -> metric.metricCode()).isEqualTo("COMMUNICATION");
        assertThat(response.departmentMetrics()).singleElement().extracting(metric -> metric.metricCode()).isEqualTo("WAITING_TIME");
        assertThat(response.complaints()).singleElement().extracting(item -> item.category()).isEqualTo("Bekleme süresi");
    }

    private void setDepartmentId(Department department, Long id) {
        try {
            java.lang.reflect.Field field = Department.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(department, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void setSubmissionId(SurveySubmission submission, Long id) {
        try {
            java.lang.reflect.Field field = SurveySubmission.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(submission, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
