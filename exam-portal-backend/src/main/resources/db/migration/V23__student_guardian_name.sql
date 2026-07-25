-- A single guardian name on the student profile replaces the separate
-- father_name / mother_name captured in V22.
ALTER TABLE users ADD COLUMN guardian_name VARCHAR(100) NULL;
ALTER TABLE users DROP COLUMN father_name;
ALTER TABLE users DROP COLUMN mother_name;
