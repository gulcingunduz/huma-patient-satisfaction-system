package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.PatientFeedbackEvaluation;
import com.hbys.survey.domain.entity.PatientFeedbackEvaluationStatus;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientFeedbackEvaluationRepository extends JpaRepository<PatientFeedbackEvaluation, Long> {

    List<PatientFeedbackEvaluation> findByEvaluatedByOrderByCreatedAtDesc(StaffUser evaluatedBy);

    List<PatientFeedbackEvaluation> findByStatusOrderByCreatedAtDesc(PatientFeedbackEvaluationStatus status);

    Optional<PatientFeedbackEvaluation> findBySubmission(SurveySubmission submission);

    List<PatientFeedbackEvaluation> findBySubmissionDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    @Query("select pfe from PatientFeedbackEvaluation pfe where pfe.submission.department.id = :departmentId and pfe.status = :status order by pfe.createdAt desc")
    List<PatientFeedbackEvaluation> findByDepartmentAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") PatientFeedbackEvaluationStatus status
    );
}
