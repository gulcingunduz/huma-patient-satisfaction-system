package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySurveyAndActiveTrueOrderByDisplayOrderAsc(Survey survey);

    List<Question> findBySurveyIdAndActiveTrueOrderByDisplayOrderAsc(Long surveyId);

    List<Question> findBySurveyIdOrderByDisplayOrderAscIdAsc(Long surveyId);

    List<Question> findBySurveyVersionIdOrderByDisplayOrderAscIdAsc(Long surveyVersionId);

    List<Question> findBySurveyVersionIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long surveyVersionId);

    List<Question> findBySurveyIdAndSurveyVersionIsNullAndActiveTrueOrderByDisplayOrderAscIdAsc(Long surveyId);

    boolean existsBySurveyVersionIdAndActiveTrue(Long surveyVersionId);

    void deleteByDepartmentId(Long departmentId);

    @Query("""
            select q from Question q
            where q.survey.id = :surveyId
              and q.active = true
              and q.surveyVersion.id = :surveyVersionId
              and (q.scope = com.hbys.survey.domain.entity.QuestionScope.COMMON
                   or (q.scope = com.hbys.survey.domain.entity.QuestionScope.DEPARTMENT_SPECIFIC
                       and q.department.id = :departmentId))
            order by q.displayOrder asc, q.id asc
            """)
    List<Question> findPublishedQuestionsBySurveyVersionAndDepartment(
            @Param("surveyId") Long surveyId,
            @Param("surveyVersionId") Long surveyVersionId,
            @Param("departmentId") Long departmentId
    );

    @Query("""
            select q from Question q
            where q.survey.id = :surveyId
              and q.active = true
              and (q.scope = com.hbys.survey.domain.entity.QuestionScope.COMMON
                   or (q.scope = com.hbys.survey.domain.entity.QuestionScope.DEPARTMENT_SPECIFIC
                       and q.department.id = :departmentId))
            order by q.displayOrder asc, q.id asc
            """)
    List<Question> findPublishedQuestionsBySurveyAndDepartment(
            @Param("surveyId") Long surveyId,
            @Param("departmentId") Long departmentId
    );

    @Query("select q from Question q where q.survey.id = :surveyId and q.active = true and q.scope = com.hbys.survey.domain.entity.QuestionScope.COMMON order by q.displayOrder asc")
    List<Question> findPublishedCommonQuestionsBySurveyId(@Param("surveyId") Long surveyId);
}
