package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Department;
import com.hbys.survey.domain.entity.Survey;
import com.hbys.survey.domain.entity.SurveyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyAccessRepository extends JpaRepository<SurveyAccess, Long> {

    Optional<SurveyAccess> findByPublicTokenHashAndActiveTrue(String publicTokenHash);

    List<SurveyAccess> findByDepartmentAndActiveTrueOrderByCreatedAtDesc(Department department);

    List<SurveyAccess> findByDepartmentOrderByCreatedAtDesc(Department department);

    List<SurveyAccess> findBySurveyAndActiveTrueOrderByCreatedAtDesc(Survey survey);

    Optional<SurveyAccess> findBySurveyAndDepartmentAndActiveTrue(Survey survey, Department department);

    boolean existsByDepartmentId(Long departmentId);

    @Query("select sa.survey from SurveyAccess sa where sa.department = :department and sa.active = true and sa.survey.active = true order by sa.createdAt desc")
    List<Survey> findSurveysByDepartment(@Param("department") Department department);
}
