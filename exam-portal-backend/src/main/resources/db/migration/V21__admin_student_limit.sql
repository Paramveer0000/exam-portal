-- SUPER_ADMIN can cap how many students a school (ADMIN) may create. NULL = unlimited.
-- Set/changed at admin creation and editable later via PUT /api/admin/{id}.
ALTER TABLE users ADD COLUMN student_limit INT NULL;
