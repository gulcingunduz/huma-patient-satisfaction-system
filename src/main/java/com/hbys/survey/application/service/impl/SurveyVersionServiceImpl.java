package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.survey.SurveyVersionResponseDto;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.SurveyNotFoundException;
import com.hbys.survey.application.service.SurveyVersionService;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyVersion;
import com.hbys.survey.domain.entity.SurveyVersionStatus;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyVersionServiceImpl implements SurveyVersionService {

    private final SurveyRepository surveyRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final QuestionRepository questionRepository;

    public SurveyVersionServiceImpl(
            SurveyRepository surveyRepository,
            SurveyVersionRepository surveyVersionRepository,
            QuestionRepository questionRepository
    ) {
        this.surveyRepository = surveyRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyVersionResponseDto> listVersions(Long surveyId) {
        Survey survey = findSurvey(surveyId);
        return surveyVersionRepository.findBySurveyOrderByVersionNumberDesc(survey)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SurveyVersionResponseDto createDraft(Long surveyId) {
        Survey survey = findSurvey(surveyId);
        if (surveyVersionRepository.findBySurveyAndStatus(survey, SurveyVersionStatus.DRAFT).isPresent()) {
            throw new BadRequestException("A draft survey version already exists.");
        }

        List<SurveyVersion> existingVersions = surveyVersionRepository.findBySurveyOrderByVersionNumberDesc(survey);
        Integer nextNumber = existingVersions.stream()
                .findFirst()
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        Optional<SurveyVersion> sourceVersion = surveyVersionRepository
                .findBySurveyAndStatus(survey, SurveyVersionStatus.PUBLISHED)
                .or(() -> existingVersions.stream().findFirst());

        SurveyVersion draft = surveyVersionRepository.save(new SurveyVersion(survey, nextNumber));

        if (sourceVersion.isPresent()) {
            List<Question> sourceQuestions = questionRepository
                    .findBySurveyVersionIdAndActiveTrueOrderByDisplayOrderAscIdAsc(sourceVersion.get().getId());
            cloneQuestionsIntoVersion(sourceQuestions, survey, draft);
        } else {
            List<Question> unversionedQuestions = questionRepository
                    .findBySurveyIdAndSurveyVersionIsNullAndActiveTrueOrderByDisplayOrderAscIdAsc(survey.getId());
            cloneQuestionsIntoVersion(unversionedQuestions, survey, draft);
        }

        return toResponse(draft);
    }

    @Override
    @Transactional
    public SurveyVersionResponseDto publishDraft(Long surveyId, Long versionId) {
        Survey survey = findSurvey(surveyId);
        SurveyVersion version = findVersion(survey, versionId);
        if (version.getStatus() != SurveyVersionStatus.DRAFT) {
            throw new BadRequestException("Only draft survey versions can be published.");
        }

        if (!questionRepository.existsBySurveyVersionIdAndActiveTrue(versionId)) {
            throw new BadRequestException("A draft survey version must contain at least one active question.");
        }

        surveyVersionRepository.findBySurveyAndStatus(survey, SurveyVersionStatus.PUBLISHED)
                .ifPresent(published -> {
                    if (!published.getId().equals(versionId)) {
                        published.archive();
                    }
                });
        version.publish();
        return toResponse(version);
    }

    @Override
    @Transactional
    public SurveyVersionResponseDto archiveVersion(Long surveyId, Long versionId) {
        Survey survey = findSurvey(surveyId);
        SurveyVersion version = findVersion(survey, versionId);
        version.archive();
        return toResponse(version);
    }

    private void cloneQuestionsIntoVersion(List<Question> sourceQuestions, Survey survey, SurveyVersion targetVersion) {
        if (sourceQuestions.isEmpty()) {
            return;
        }
        List<Question> cloned = sourceQuestions.stream()
                .map(source -> {
                    Question copy = new Question(
                            survey,
                            source.getText(),
                            source.getType(),
                            source.getDisplayOrder(),
                            source.isRequired()
                    );
                    copy.updateScope(source.getScope(), source.getDepartment(), source.getMetricCode());
                    copy.setSurveyVersion(targetVersion);
                    return copy;
                })
                .toList();
        questionRepository.saveAll(cloned);
    }

    private Survey findSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId).orElseThrow(SurveyNotFoundException::new);
    }

    private SurveyVersion findVersion(Survey survey, Long versionId) {
        return surveyVersionRepository.findById(versionId)
                .filter(version -> version.getSurvey().getId().equals(survey.getId()))
                .orElseThrow(() -> new BadRequestException("Survey version was not found for this survey."));
    }

    private SurveyVersionResponseDto toResponse(SurveyVersion version) {
        return new SurveyVersionResponseDto(
                version.getId(),
                version.getSurvey().getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getCreatedAt(),
                version.getPublishedAt()
        );
    }
}
