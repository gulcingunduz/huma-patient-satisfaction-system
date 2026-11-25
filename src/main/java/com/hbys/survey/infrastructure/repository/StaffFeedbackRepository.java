package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.StaffFeedback;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffFeedbackRepository extends JpaRepository<StaffFeedback, Long> {

    List<StaffFeedback> findByStaffUserOrderByCreatedAtDesc(StaffUser staffUser);

    Optional<StaffFeedback> findBySubmission(SurveySubmission submission);

    List<StaffFeedback> findByStaffUserAndSubmissionOrderByCreatedAtDesc(StaffUser staffUser, SurveySubmission submission);

    @Query("select sf from StaffFeedback sf where sf.submission.id = :submissionId order by sf.createdAt desc")
    List<StaffFeedback> findBySubmissionIdOrderByCreatedAtDesc(@Param("submissionId") Long submissionId);

    @Query("select sf from StaffFeedback sf where sf.staffUser = :staffUser and sf.createdAt >= :since order by sf.createdAt desc")
    List<StaffFeedback> findRecentByStaffUser(@Param("staffUser") StaffUser staffUser, @Param("since") Instant since);
}