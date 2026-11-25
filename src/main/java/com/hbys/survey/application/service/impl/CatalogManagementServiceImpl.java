package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.survey.DepartmentCreateRequestDto;
import com.hbys.survey.application.dto.survey.DepartmentResponseDto;
import com.hbys.survey.application.dto.survey.SurveyAccessCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyAccessResponseDto;
import com.hbys.survey.application.dto.survey.SurveyCreateRequestDto;
import com.hbys.survey.application.dto.survey.SurveyResponseDto;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.DepartmentNotFoundException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.exception.SurveyNotFoundException;
import com.hbys.survey.application.service.CatalogManagementService;
import com.hbys.survey.application.service.TokenHashService;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyAccess;
import com.hbys.survey.infrastructure.repository.AISummaryRepository;
import com.hbys.survey.infrastructure.repository.AnswerRepository;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.QuestionRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import com.hbys.survey.infrastructure.repository.SurveyAccessRepository;
import com.hbys.survey.infrastructure.repository.SurveyRepository;
import com.hbys.survey.infrastructure.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class CatalogManagementServiceImpl implements CatalogManagementService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DepartmentRepository departmentRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyAccessRepository surveyAccessRepository;
    private final TokenHashService tokenHashService;
    private final StaffUserRepository staffUserRepository;
    private final QuestionRepository questionRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final AISummaryRepository aiSummaryRepository;
    private final AnswerRepository answerRepository;

    public CatalogManagementServiceImpl(
            DepartmentRepository departmentRepository,
            SurveyRepository surveyRepository,
            SurveyAccessRepository surveyAccessRepository,
            TokenHashService tokenHashService,
            StaffUserRepository staffUserRepository,
            QuestionRepository questionRepository,
            SurveySubmissionRepository surveySubmissionRepository,
            AISummaryRepository aiSummaryRepository,
            AnswerRepository answerRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.surveyRepository = surveyRepository;
        this.surveyAccessRepository = surveyAccessRepository;
        this.tokenHashService = tokenHashService;
        this.staffUserRepository = staffUserRepository;
        this.questionRepository = questionRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.aiSummaryRepository = aiSummaryRepository;
        this.answerRepository = answerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getDepartments(boolean includeInactive) {
        List<Department> departments = includeInactive
                ? departmentRepository.findAll()
                : departmentRepository.findByActiveTrueOrderByNameAsc();
        return departments.stream().map(CatalogManagementServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAccessibleDepartments(String username) {
        StaffUser staffUser = staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
        if (staffUser.getRole() == StaffRole.STAFF) {
            Department department = staffUser.getDepartment();
            return department == null || !department.isActive()
                    ? List.of()
                    : List.of(toResponse(department));
        }
        return departmentRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(CatalogManagementServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentCreateRequestDto request) {
        String name = request.getName().trim();
        if (departmentRepository.findByName(name).isPresent()) {
            throw new BadRequestException("A department with this name already exists.");
        }
        return toResponse(departmentRepository.save(new Department(name)));
    }

    @Override
    @Transactional
    public DepartmentResponseDto updateDepartment(Long departmentId, DepartmentCreateRequestDto request) {
        Department department = findDepartment(departmentId);
        String name = request.getName().trim();
        departmentRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(departmentId))
                .ifPresent(existing -> {
                    throw new BadRequestException("A department with this name already exists.");
                });
        department.updateName(name);
        return toResponse(department);
    }

    @Override
    @Transactional
    public DepartmentResponseDto setDepartmentActive(Long departmentId, boolean active) {
        Department department = findDepartment(departmentId);
        department.setActive(active);
        return toResponse(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);
        boolean hasHistory = surveySubmissionRepository.existsByDepartmentId(departmentId)
                || surveyAccessRepository.existsByDepartmentId(departmentId)
                || staffUserRepository.existsByDepartmentId(departmentId)
                || aiSummaryRepository.existsByDepartment_Id(departmentId)
                || answerRepository.existsByQuestion_Department_Id(departmentId);
        if (hasHistory) {
            throw new BadRequestException(
                    "Bu birime bağlı anket gönderimi, erişim bağlantısı, AI özeti veya kullanıcı kaydı bulunduğundan silinemez; bunun yerine pasifleştirebilirsiniz."
            );
        }
        questionRepository.deleteByDepartmentId(departmentId);
        departmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyResponseDto> getSurveys(boolean includeInactive) {
        List<Survey> surveys = includeInactive
                ? surveyRepository.findAll()
                : surveyRepository.findByActiveTrueOrderByCreatedAtDesc();
        return surveys.stream().map(CatalogManagementServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public SurveyResponseDto createSurvey(SurveyCreateRequestDto request) {
        return toResponse(surveyRepository.save(new Survey(request.getTitle().trim())));
    }

    @Override
    @Transactional
    public SurveyResponseDto updateSurvey(Long surveyId, SurveyCreateRequestDto request) {
        Survey survey = findSurvey(surveyId);
        survey.updateTitle(request.getTitle().trim());
        return toResponse(survey);
    }

    @Override
    @Transactional
    public SurveyResponseDto setSurveyActive(Long surveyId, boolean active) {
        Survey survey = findSurvey(surveyId);
        survey.setActive(active);
        return toResponse(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyAccessResponseDto> getSurveyAccesses(Long departmentId) {
        Department department = findDepartment(departmentId);
        return surveyAccessRepository.findByDepartmentOrderByCreatedAtDesc(department)
                .stream()
                .map(access -> toResponse(access, null))
                .toList();
    }

    @Override
    @Transactional
    public SurveyAccessResponseDto createSurveyAccess(SurveyAccessCreateRequestDto request) {
        Survey survey = surveyRepository.findByIdAndActiveTrue(request.getSurveyId())
                .orElseThrow(SurveyNotFoundException::new);
        Department department = findDepartment(request.getDepartmentId());
        if (!department.isActive()) {
            throw new BadRequestException("Cannot create access for an inactive department.");
        }
        if (surveyAccessRepository.findBySurveyAndDepartmentAndActiveTrue(survey, department).isPresent()) {
            throw new BadRequestException("An active access already exists for this survey and department.");
        }

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String publicToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        SurveyAccess access = surveyAccessRepository.save(
                new SurveyAccess(survey, department, tokenHashService.sha256(publicToken))
        );
        return toResponse(access, publicToken);
    }

    @Override
    @Transactional
    public SurveyAccessResponseDto deactivateSurveyAccess(Long accessId) {
        SurveyAccess access = surveyAccessRepository.findById(accessId)
                .orElseThrow(() -> new BadRequestException("Survey access was not found."));
        access.deactivate();
        return toResponse(access, null);
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(DepartmentNotFoundException::new);
    }

    private Survey findSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId).orElseThrow(SurveyNotFoundException::new);
    }

    private static DepartmentResponseDto toResponse(Department department) {
        return new DepartmentResponseDto(department.getId(), department.getName(), department.isActive());
    }

    private static SurveyResponseDto toResponse(Survey survey) {
        return new SurveyResponseDto(survey.getId(), survey.getTitle(), survey.isActive(), survey.getCreatedAt());
    }

    private static SurveyAccessResponseDto toResponse(SurveyAccess access, String publicToken) {
        return new SurveyAccessResponseDto(
                access.getId(),
                access.getSurvey().getId(),
                access.getSurvey().getTitle(),
                access.getDepartment().getId(),
                access.getDepartment().getName(),
                access.isActive(),
                access.getCreatedAt(),
                publicToken
        );
    }
}
