package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.AISummary;
import com.hbys.survey.domain.entity.AISummaryStatus;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AISummaryRepository extends JpaRepository<AISummary, Long> {

    List<AISummary> findBySubmissionOrderByCreatedAtDesc(SurveySubmission submission);

    Optional<AISummary> findBySubmissionAndModelName(SurveySubmission submission, String modelName);

    List<AISummary> findByModelNameOrderByCreatedAtDesc(String modelName);

    List<AISummary> findByDepartment_IdOrderByCreatedAtDesc(Long departmentId);

    boolean existsByDepartment_Id(Long departmentId);

    List<AISummary> findByDepartment_IdAndStatusOrderByCreatedAtDesc(Long departmentId, AISummaryStatus status);

    @Query("select a from AISummary a where a.department.id = :departmentId order by a.createdAt desc")
    List<AISummary> findByDepartmentIdOrderByCreatedAtDesc(@Param("departmentId") Long departmentId);

    @Query("select a from AISummary a where a.submission.survey.id = :surveyId order by a.createdAt desc")
    List<AISummary> findBySurveyIdOrderByCreatedAtDesc(@Param("surveyId") Long surveyId);
}
