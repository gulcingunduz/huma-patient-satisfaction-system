package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.mapper.QuestionMapper;
import com.hbys.survey.application.dto.survey.PublicSurveyInfoResponse;
import com.hbys.survey.application.dto.survey.QuestionCreateRequestDto;
import com.hbys.survey.application.dto.survey.QuestionResponseDto;
import com.hbys.survey.application.dto.survey.QuestionUpdateRequestDto;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.InvalidSubmissionException;
import com.hbys.survey.application.exception.InvalidSurveyAccessException;
import com.hbys.survey.application.exception.QuestionNotFoundException;
import com.hbys.survey.application.exception.SurveyNotFoundException;
import com.hbys.survey.application.service.QuestionService;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.QuestionScope;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyAccess;
import com.hbys.survey.domain.entity.SurveyVersion;
import com.hbys.survey.domain.entity.SurveyVersionStatus;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.SurveyAccessRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyAccessRepository surveyAccessRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final DepartmentRepository departmentRepository;
    private final TokenHashService tokenHashService;

    public QuestionServiceImpl(
            QuestionRepository questionRepository,
            SurveyRepository surveyRepository,
            SurveyAccessRepository surveyAccessRepository,
            SurveyVersionRepository surveyVersionRepository,
            DepartmentRepository departmentRepository,
            TokenHashService tokenHashService
    ) {
        this.questionRepository = questionRepository;
        this.surveyRepository = surveyRepository;
        this.surveyAccessRepository = surveyAccessRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.departmentRepository = departmentRepository;
        this.tokenHashService = tokenHashService;
    }

    @Override
    @Transactional
    public QuestionResponseDto createQuestion(Long surveyId, QuestionCreateRequestDto request) {
        if (request == null) {
            throw new InvalidSubmissionException("Question request cannot be null.");
        }

        Survey survey = surveyRepository.findByIdAndActiveTrue(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Active survey was not found."));
        SurveyVersion targetVersion = resolveTargetVersionForCreate(survey, request.getSurveyVersionId());
        QuestionScope scope = request.getScope() == null ? QuestionScope.COMMON : request.getScope();
        Department department = resolveQuestionDepartment(scope, request.getDepartmentId());
        String resolvedMetricCode = resolveMetricCode(request.getMetricCode(), survey.getId(), null);

        validateUniqueMetricCode(survey.getId(), targetVersion, department, resolvedMetricCode, null);

        Question question = QuestionMapper.toEntity(request, survey, department);
        question.updateScope(scope, department, resolvedMetricCode);
        question.setSurveyVersion(targetVersion);
        return QuestionMapper.toResponse(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponseDto updateQuestion(Long questionId, QuestionUpdateRequestDto request) {
        if (request == null) {
            throw new InvalidSubmissionException("Question request cannot be null.");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(QuestionNotFoundException::new);
        assertMutableVersion(question.getSurveyVersion());

        QuestionScope scope = request.getScope() == null ? QuestionScope.COMMON : request.getScope();
        Department department = resolveQuestionDepartment(scope, request.getDepartmentId());
        String resolvedMetricCode = resolveMetricCode(request.getMetricCode(), question.getSurvey().getId(), question.getId());

        validateUniqueMetricCode(
                question.getSurvey().getId(),
                question.getSurveyVersion(),
                department,
                resolvedMetricCode,
                question.getId()
        );

        QuestionMapper.toEntity(request, question, question.getSurvey());
        question.updateScope(scope, department, resolvedMetricCode);
        return QuestionMapper.toResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponseDto deactivateQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(QuestionNotFoundException::new);
        assertMutableVersion(question.getSurveyVersion());

        question.updateDetails(
                question.getText(),
                question.getType(),
                question.getDisplayOrder(),
                question.isRequired(),
                false
        );
        return QuestionMapper.toResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponseDto> getActiveQuestionsForPublicAccess(String publicAccessToken) {
        SurveyAccess access = resolveActivePublicAccess(publicAccessToken);
        return findPublicQuestions(access)
                .stream()
                .map(QuestionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicSurveyInfoResponse getPublicSurveyInfo(String publicAccessToken) {
        SurveyAccess access = resolveActivePublicAccess(publicAccessToken);
        Optional<SurveyVersion> publishedVersion = surveyVersionRepository
                .findBySurveyAndStatus(access.getSurvey(), SurveyVersionStatus.PUBLISHED);
        List<Question> questions = findPublicQuestions(access);

        return new PublicSurveyInfoResponse(
                access.getSurvey().getId(),
                access.getSurvey().getTitle(),
                access.getDepartment().getId(),
                access.getDepartment().getName(),
                publishedVersion.map(SurveyVersion::getVersionNumber).orElse(null),
                questions.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponseDto> getQuestionsForSurvey(Long surveyId) {
        return getQuestionsForSurvey(surveyId, null, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponseDto> getQuestionsForSurvey(Long surveyId, Long versionId, boolean includeInactive) {
        List<Question> questions;
        if (versionId != null) {
            SurveyVersion version = surveyVersionRepository.findById(versionId)
                    .filter(item -> item.getSurvey().getId().equals(surveyId))
                    .orElseThrow(() -> new BadRequestException("Survey version was not found for this survey."));
            questions = includeInactive
                    ? questionRepository.findBySurveyVersionIdOrderByDisplayOrderAscIdAsc(version.getId())
                    : questionRepository.findBySurveyVersionIdAndActiveTrueOrderByDisplayOrderAscIdAsc(version.getId());
        } else {
            questions = includeInactive
                    ? questionRepository.findBySurveyIdOrderByDisplayOrderAscIdAsc(surveyId)
                    : questionRepository.findBySurveyIdAndActiveTrueOrderByDisplayOrderAsc(surveyId);
        }

        return questions.stream()
                .map(QuestionMapper::toResponse)
                .toList();
    }

    private SurveyAccess resolveActivePublicAccess(String publicAccessToken) {
        if (publicAccessToken == null || publicAccessToken.isBlank()) {
            throw new InvalidSurveyAccessException();
        }
        String tokenHash = tokenHashService.sha256(publicAccessToken);
        return surveyAccessRepository.findByPublicTokenHashAndActiveTrue(tokenHash)
                .filter(item -> item.getSurvey().isActive() && item.getDepartment().isActive())
                .orElseThrow(InvalidSurveyAccessException::new);
    }

    private List<Question> findPublicQuestions(SurveyAccess access) {
        Optional<SurveyVersion> publishedVersion = surveyVersionRepository
                .findBySurveyAndStatus(access.getSurvey(), SurveyVersionStatus.PUBLISHED);
        return publishedVersion
                .map(version -> questionRepository.findPublishedQuestionsBySurveyVersionAndDepartment(
                        access.getSurvey().getId(),
                        version.getId(),
                        access.getDepartment().getId()
                ))
                .orElseGet(() -> questionRepository.findPublishedQuestionsBySurveyAndDepartment(
                        access.getSurvey().getId(),
                        access.getDepartment().getId()
                ));
    }

    private SurveyVersion resolveTargetVersionForCreate(Survey survey, Long requestedVersionId) {
        if (requestedVersionId != null) {
            SurveyVersion version = surveyVersionRepository.findById(requestedVersionId)
                    .filter(item -> item.getSurvey().getId().equals(survey.getId()))
                    .orElseThrow(() -> new BadRequestException("Survey version was not found for this survey."));
            if (version.getStatus() != SurveyVersionStatus.DRAFT) {
                throw new BadRequestException("Questions can only be added to a DRAFT survey version.");
            }
            return version;
        }

        Optional<SurveyVersion> draftVersion = surveyVersionRepository.findBySurveyAndStatus(survey, SurveyVersionStatus.DRAFT);
        if (draftVersion.isPresent()) {
            return draftVersion.get();
        }

        if (surveyVersionRepository.findBySurveyAndStatus(survey, SurveyVersionStatus.PUBLISHED).isPresent()) {
            throw new BadRequestException("Published survey versions cannot be modified directly. Create a draft version first.");
        }

        return null;
    }

    private void assertMutableVersion(SurveyVersion version) {
        if (version != null && version.getStatus() != SurveyVersionStatus.DRAFT) {
            throw new BadRequestException("Published or archived survey questions cannot be modified. Create a draft version first.");
        }
    }

    private void validateUniqueMetricCode(
            Long surveyId,
            SurveyVersion targetVersion,
            Department department,
            String metricCode,
            Long excludeQuestionId
    ) {
        if (targetVersion == null) {
            return;
        }
        Long targetDepartmentId = department == null ? null : department.getId();
        boolean duplicate = questionRepository
                .findBySurveyVersionIdOrderByDisplayOrderAscIdAsc(targetVersion.getId())
                .stream()
                .filter(existing -> excludeQuestionId == null || !existing.getId().equals(excludeQuestionId))
                .anyMatch(existing -> Objects.equals(
                        existing.getDepartment() == null ? null : existing.getDepartment().getId(),
                        targetDepartmentId
                ) && metricCode.equalsIgnoreCase(existing.getMetricCode()));

        if (duplicate) {
            throw new BadRequestException("A question with metric code '" + metricCode + "' already exists in this version and scope.");
        }
    }

    private Department resolveQuestionDepartment(QuestionScope scope, Long departmentId) {
        QuestionScope resolvedScope = scope == null ? QuestionScope.COMMON : scope;
        if (resolvedScope == QuestionScope.COMMON) {
            return null;
        }
        if (departmentId == null) {
            throw new BadRequestException("Department is required for department-specific questions.");
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BadRequestException("Active department was not found."));
        if (!department.isActive()) {
            throw new BadRequestException("Active department was not found.");
        }
        return department;
    }

    private String resolveMetricCode(String metricCode, Long surveyId, Long questionId) {
        if (metricCode != null && !metricCode.isBlank()) {
            return normalizeMetricCode(metricCode);
        }
        if (questionId != null) {
            return "SURVEY_" + surveyId + "_QUESTION_" + questionId;
        }
        return "SURVEY_" + surveyId + "_METRIC_" + System.nanoTime();
    }

    private String normalizeMetricCode(String metricCode) {
        return metricCode.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_");
    }
}
