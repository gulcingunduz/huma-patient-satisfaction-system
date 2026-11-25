package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.staff.StaffUserCreateRequestDto;
import com.hbys.survey.application.dto.staff.StaffUserResponseDto;
import com.hbys.survey.application.dto.staff.StaffUserUpdateRequestDto;
import com.hbys.survey.application.exception.BadRequestException;
import com.hbys.survey.application.exception.DepartmentNotFoundException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.service.StaffUserService;
import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.DepartmentRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffUserServiceImpl implements StaffUserService {

    private final StaffUserRepository staffUserRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffUserServiceImpl(
            StaffUserRepository staffUserRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.staffUserRepository = staffUserRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffUserResponseDto> listUsers() {
        return staffUserRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"))
                .stream()
                .map(StaffUserServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StaffUserResponseDto createUser(StaffUserCreateRequestDto request) {
        String normalizedUsername = request.getUsername().trim();
        if (staffUserRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new BadRequestException("A user with this username already exists.");
        }

        Department department = resolveDepartmentForRole(request.getRole(), request.getDepartmentId());
        String passwordHash = passwordEncoder.encode(request.getPassword());
        StaffUser saved = staffUserRepository.save(new StaffUser(
                normalizedUsername,
                passwordHash,
                request.getFullName().trim(),
                request.getRole(),
                department
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StaffUserResponseDto updateUser(Long userId, StaffUserUpdateRequestDto request, String actorUsername) {
        StaffUser user = staffUserRepository.findById(userId)
                .orElseThrow(StaffUserNotFoundException::new);

        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            if (!request.isActive()) {
                throw new BadRequestException("You cannot deactivate your own admin account.");
            }
            if (request.getRole() != StaffRole.ADMIN) {
                throw new BadRequestException("You cannot remove ADMIN role from your own account.");
            }
        }

        Department department = resolveDepartmentForRole(request.getRole(), request.getDepartmentId());
        user.updateDetails(request.getFullName().trim(), request.getRole(), department, request.isActive());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.updatePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        return toResponse(user);
    }

    @Override
    @Transactional
    public StaffUserResponseDto setUserActive(Long userId, boolean active, String actorUsername) {
        StaffUser user = staffUserRepository.findById(userId)
                .orElseThrow(StaffUserNotFoundException::new);
        if (!active && user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new BadRequestException("You cannot deactivate your own admin account.");
        }
        user.setActive(active);
        return toResponse(user);
    }

    private Department resolveDepartmentForRole(StaffRole role, Long departmentId) {
        if (role == StaffRole.STAFF) {
            if (departmentId == null) {
                throw new BadRequestException("Department assignment is required for STAFF role.");
            }
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(DepartmentNotFoundException::new);
            if (!department.isActive()) {
                throw new BadRequestException("Assigned department must be active.");
            }
            return department;
        }
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(DepartmentNotFoundException::new);
        }
        return null;
    }

    private static StaffUserResponseDto toResponse(StaffUser user) {
        return new StaffUserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
