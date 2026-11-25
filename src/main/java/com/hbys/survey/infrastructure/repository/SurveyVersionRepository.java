package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyVersion;
import com.hbys.survey.domain.entity.SurveyVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyVersionRepository extends JpaRepository<SurveyVersion, Long> {

    List<SurveyVersion> findBySurveyOrderByVersionNumberDesc(Survey survey);

    Optional<SurveyVersion> findBySurveyAndStatus(Survey survey, SurveyVersionStatus status);

    Optional<SurveyVersion> findBySurveyAndVersionNumber(Survey survey, Integer versionNumber);
}
