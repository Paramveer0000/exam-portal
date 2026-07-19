-- Per-owner scoping: every category and quiz is owned by the admin who created it.
-- created_by is nullable so legacy rows (created before ownership existed) remain;
-- the service layer treats NULL-owner rows as visible to SUPER_ADMIN only.
ALTER TABLE categories
    ADD COLUMN created_by BIGINT    NULL,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);

ALTER TABLE quizzes
    ADD COLUMN created_by BIGINT    NULL,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_quizzes_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);

-- Questions are owned transitively through their quiz; a timestamp still helps
-- activity feeds and auditing.
ALTER TABLE questions
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_categories_created_by ON categories (created_by);
CREATE INDEX idx_quizzes_created_by    ON quizzes (created_by);
