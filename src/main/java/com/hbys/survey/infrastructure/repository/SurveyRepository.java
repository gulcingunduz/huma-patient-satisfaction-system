package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByActiveTrueOrderByCreatedAtDesc();

    Optional<Survey> findByIdAndActiveTrue(Long id);

    List<Survey> findByTitleContainingIgnoreCase(String title);
}
