ALTER TABLE ai_summaries ADD COLUMN IF NOT EXISTS survey_version_scope varchar(255);

DROP INDEX IF EXISTS uk_questions_metric_code_version;
CREATE UNIQUE INDEX IF NOT EXISTS uk_questions_metric_code_version
    ON questions (survey_id, survey_version_id, COALESCE(department_id, 0), metric_code)
    WHERE survey_version_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_summaries_department_created
    ON ai_summaries (department_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created
    ON notifications (staff_user_id, read, created_at DESC);
