-- Content model change: only SUPER_ADMIN creates classes (categories), quizzes
-- and questions. Schools (ADMIN) instead ASSIGN existing classes to their
-- students, who then see the published quizzes in those classes.

-- 1. Which classes a student is assigned to (many-to-many).
CREATE TABLE student_class (
    user_id BIGINT NOT NULL,
    cat_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, cat_id),
    CONSTRAINT fk_sc_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_cat  FOREIGN KEY (cat_id)  REFERENCES categories (cat_id) ON DELETE CASCADE
);

-- 2. Migrate all existing content ownership to the lowest-id SUPER_ADMIN so
--    nothing is lost and it becomes platform-owned content.
SET @sa := (
    SELECT u.user_id
    FROM users u
    JOIN user_role ur ON u.user_id = ur.user_id
    WHERE ur.role_id = 'SUPER_ADMIN'
    ORDER BY u.user_id
    LIMIT 1
);

UPDATE categories SET created_by = @sa WHERE @sa IS NOT NULL;
UPDATE quizzes    SET created_by = @sa WHERE @sa IS NOT NULL;
