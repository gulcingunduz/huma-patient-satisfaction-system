package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.FeedbackSatisfaction;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackSatisfactionRepository extends JpaRepository<FeedbackSatisfaction, Long> {

    Optional<FeedbackSatisfaction> findBySubmission(SurveySubmission submission);

    boolean existsBySubmission(SurveySubmission submission);
}