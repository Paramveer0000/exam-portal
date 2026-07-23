-- Class(=categories) -> Subject(new) -> Quiz restructure, and one class per student.
-- Previously: Class(categories) -> Quiz directly, students assigned MANY classes
-- via student_class. Now a Subject tier sits between class and quiz, quizzes belong
-- to a subject, and each student belongs to exactly one class (users.class_id).

-- 1. Subjects belong to a class (category).
CREATE TABLE IF NOT EXISTS subjects (
    subject_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  VARCHAR(5000),
    class_id     BIGINT NOT NULL,
    created_by   BIGINT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subjects_class FOREIGN KEY (class_id) REFERENCES categories (cat_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 2. Give every existing class one default "General" subject so existing quizzes
--    (which pointed straight at a class) have a home.
INSERT INTO subjects (title, class_id, created_by)
SELECT 'General', cat_id, created_by FROM categories;

-- 3. Quiz now belongs to a subject; backfill from its old class's General subject.
ALTER TABLE quizzes ADD COLUMN subject_id BIGINT;
UPDATE quizzes q
  JOIN subjects s ON s.class_id = q.category_cat_id AND s.title = 'General'
  SET q.subject_id = s.subject_id;
ALTER TABLE quizzes
  ADD CONSTRAINT fk_quizzes_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);
-- The old quiz->category FK has a Hibernate-generated name; look it up to drop it.
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quizzes'
              AND COLUMN_NAME = 'category_cat_id' AND REFERENCED_TABLE_NAME IS NOT NULL
            LIMIT 1);
SET @sql := CONCAT('ALTER TABLE quizzes DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE quizzes DROP COLUMN category_cat_id;

-- 4. One class per student. Backfill from the old many-class join (lowest class id).
ALTER TABLE users ADD COLUMN class_id BIGINT;
ALTER TABLE users
  ADD CONSTRAINT fk_users_class FOREIGN KEY (class_id) REFERENCES categories (cat_id);
UPDATE users u
  JOIN (SELECT user_id, MIN(cat_id) AS cid FROM student_class GROUP BY user_id) x
    ON u.user_id = x.user_id
  SET u.class_id = x.cid;

-- 5. Drop the many-class join table.
DROP TABLE student_class;
