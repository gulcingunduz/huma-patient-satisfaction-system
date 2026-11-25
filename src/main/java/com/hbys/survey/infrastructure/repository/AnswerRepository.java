package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Answer;
import com.hbys.survey.domain.entity.Question;
import com.hbys.survey.domain.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySubmissionOrderByQuestion_DisplayOrderAsc(SurveySubmission submission);

    List<Answer> findBySubmissionAndQuestion(SurveySubmission submission, Question question);

    @Query("select a from Answer a join fetch a.question q where a.submission = :submission order by q.displayOrder asc")
    List<Answer> findBySubmissionWithQuestion(@Param("submission") SurveySubmission submission);

    @Query("select a from Answer a where a.submission.survey.id = :surveyId and a.question.id = :questionId")
    List<Answer> findAnswersForSurveyQuestion(@Param("surveyId") Long surveyId, @Param("questionId") Long questionId);

    @Query("select a from Answer a where a.submission.department.id = :departmentId and a.createdAt >= :from and a.createdAt <= :to order by a.createdAt asc")
    List<Answer> findByDepartmentAndCreatedAtBetween(
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select a from Answer a
            join fetch a.question q
            join fetch a.submission s
            where s.id in :submissionIds
            order by q.displayOrder asc, a.createdAt asc
            """)
    List<Answer> findBySubmissionIdInWithQuestion(@Param("submissionIds") List<Long> submissionIds);

    boolean existsByQuestion_Department_Id(Long departmentId);
}
