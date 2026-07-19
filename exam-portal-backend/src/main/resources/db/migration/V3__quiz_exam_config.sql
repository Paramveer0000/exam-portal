-- Per-quiz exam configuration: how many questions to serve from the quiz's pool,
-- and whether to randomize question order and answer-option order per student.
ALTER TABLE quizzes
    ADD COLUMN questions_per_exam  INT     NULL AFTER num_of_questions,
    ADD COLUMN randomize_questions BIT     NOT NULL DEFAULT 0 AFTER questions_per_exam,
    ADD COLUMN randomize_options   BIT     NOT NULL DEFAULT 0 AFTER randomize_questions,
    ADD COLUMN passing_percentage  INT     NOT NULL DEFAULT 33 AFTER randomize_options;

-- Preserve current behavior for existing quizzes: serve the whole pool.
-- num_of_questions historically holds the pool size for existing rows.
UPDATE quizzes
SET questions_per_exam = num_of_questions
WHERE questions_per_exam IS NULL;
