-- A student (USER) belongs to a teacher (ADMIN). Null for admins/super admins.
ALTER TABLE users
    ADD COLUMN teacher_id BIGINT NULL;

-- Backfill existing students to the lowest-id admin (one-time assignment;
-- they can be reassigned later).
UPDATE users
SET teacher_id = (SELECT MIN(user_id) FROM user_role WHERE role_id = 'ADMIN')
WHERE teacher_id IS NULL
  AND user_id IN (SELECT user_id FROM user_role WHERE role_id = 'USER');

ALTER TABLE users
    ADD CONSTRAINT fk_users_teacher FOREIGN KEY (teacher_id) REFERENCES users (user_id);

CREATE INDEX idx_users_teacher_id ON users (teacher_id);
