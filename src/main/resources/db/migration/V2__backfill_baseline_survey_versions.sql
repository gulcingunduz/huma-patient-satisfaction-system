INSERT INTO survey_versions (survey_id, version_number, status, created_at, published_at)
SELECT s.id, 1, 'PUBLISHED', COALESCE(s.created_at, CURRENT_TIMESTAMP), COALESCE(s.created_at, CURRENT_TIMESTAMP)
FROM surveys s
WHERE NOT EXISTS (
    SELECT 1 FROM survey_versions sv WHERE sv.survey_id = s.id AND sv.version_number = 1
);

UPDATE questions q
SET survey_version_id = sv.id
FROM survey_versions sv
WHERE sv.survey_id = q.survey_id
  AND sv.version_number = 1
  AND q.survey_version_id IS NULL;

UPDATE survey_submissions ss
SET survey_version_id = sv.id
FROM survey_versions sv
WHERE sv.survey_id = ss.survey_id
  AND sv.version_number = 1
  AND ss.survey_version_id IS NULL;

UPDATE questions
SET metric_code = 'COMMON_QUESTION_' || id
WHERE scope = 'COMMON'
  AND (metric_code IS NULL OR metric_code = '');

UPDATE questions
SET metric_code = 'DEPARTMENT_QUESTION_' || id
WHERE scope = 'DEPARTMENT_SPECIFIC'
  AND (metric_code IS NULL OR metric_code = '');

ALTER TABLE questions ALTER COLUMN metric_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_questions_metric_code_version
    ON questions (survey_id, survey_version_id, COALESCE(department_id, 0), metric_code)
    WHERE survey_version_id IS NOT NULL;
