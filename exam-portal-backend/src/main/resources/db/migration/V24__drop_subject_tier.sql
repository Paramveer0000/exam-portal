-- Undo V20's Subject tier: back to Class(=categories) -> Quiz directly.
-- The Subject tier was never manageable from the UI (no page ever created one),
-- so every class carried a placeholder "General" subject just to host its quizzes.
-- A quiz's class is recovered from its subject's class_id, so no data is lost.
-- V20's other change (one class per student, users.class_id) is deliberately kept.

-- 1. Quiz points at its class again.
ALTER TABLE quizzes ADD COLUMN category_cat_id BIGINT;

UPDATE quizzes q
  JOIN subjects s ON s.subject_id = q.subject_id
  SET q.category_cat_id = s.class_id;

ALTER TABLE quizzes
  ADD CONSTRAINT fk_quizzes_category FOREIGN KEY (category_cat_id) REFERENCES categories (cat_id);

-- 2. Drop the quiz -> subject link. The FK name is known (set in V20), but look it
--    up anyway so this works on schemas where Hibernate generated it instead.
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quizzes'
              AND COLUMN_NAME = 'subject_id' AND REFERENCED_TABLE_NAME IS NOT NULL
            LIMIT 1);
SET @sql := CONCAT('ALTER TABLE quizzes DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE quizzes DROP COLUMN subject_id;

-- 3. The tier itself is gone.
DROP TABLE subjects;
