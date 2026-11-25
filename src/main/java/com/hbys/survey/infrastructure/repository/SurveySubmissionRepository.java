package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, Long> {

    List<SurveySubmission> findByDepartmentOrderByCreatedAtDesc(Department department);

    boolean existsByDepartmentId(Long departmentId);

    List<SurveySubmission> findByDepartmentAndActiveTrueOrderByCreatedAtDesc(Department department);

    List<SurveySubmission> findBySurveyAndActiveTrueOrderByCreatedAtDesc(Survey survey);

    List<SurveySubmission> findAllByOrderByCreatedAtDesc();

    Optional<SurveySubmission> findByTrackingTokenHash(String trackingTokenHash);

    Optional<SurveySubmission> findBySurveyIdAndClientTokenHashAndCreatedAtAfter(Long surveyId, String clientTokenHash, Instant cutoff);

    @Query("select s from SurveySubmission s where s.survey.id = :surveyId and s.clientTokenHash = :clientTokenHash and s.createdAt >= :since and s.active = true")
    Optional<SurveySubmission> findRecentDuplicateSubmission(
            @Param("surveyId") Long surveyId,
            @Param("clientTokenHash") String clientTokenHash,
            @Param("since") Instant since
    );

    @Query("select s from SurveySubmission s where s.department = :department and s.createdAt >= :from and s.createdAt <= :to order by s.createdAt desc")
    List<SurveySubmission> findByDepartmentAndCreatedAtBetween(
            @Param("department") Department department,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("select s from SurveySubmission s where s.createdAt >= :from and s.createdAt <= :to order by s.createdAt desc")
    List<SurveySubmission> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}