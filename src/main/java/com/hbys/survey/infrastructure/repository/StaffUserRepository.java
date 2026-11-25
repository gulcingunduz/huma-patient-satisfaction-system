package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsername(String username);

    Optional<StaffUser> findByUsernameAndActiveTrue(String username);

    List<StaffUser> findByActiveTrueOrderByFullNameAsc();

    List<StaffUser> findByRoleAndActiveTrueOrderByFullNameAsc(StaffRole role);

    List<StaffUser> findByDepartmentAndActiveTrue(Department department);

    List<StaffUser> findByDepartmentAndRoleAndActiveTrue(Department department, StaffRole role);

    boolean existsByDepartmentId(Long departmentId);
}
