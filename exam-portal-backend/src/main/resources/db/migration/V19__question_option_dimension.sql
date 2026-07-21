-- Optional per-option dimension override, so a single question's options can
-- score into different psychometric dimensions (e.g. Yes -> one trait, No ->
-- another). NULL means "use the question's own dimension" (existing behavior).
ALTER TABLE questions
    ADD COLUMN option1_dimension VARCHAR(20) NULL AFTER option1,
    ADD COLUMN option2_dimension VARCHAR(20) NULL AFTER option2,
    ADD COLUMN option3_dimension VARCHAR(20) NULL AFTER option3,
    ADD COLUMN option4_dimension VARCHAR(20) NULL AFTER option4;
