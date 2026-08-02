-- Fifth (optional) answer option, matching the option1-4 pattern (including
-- its own optional per-option dimension override from V19).
ALTER TABLE questions
    ADD COLUMN option5 VARCHAR(255) NULL AFTER option4_dimension,
    ADD COLUMN option5_dimension VARCHAR(20) NULL AFTER option5;
