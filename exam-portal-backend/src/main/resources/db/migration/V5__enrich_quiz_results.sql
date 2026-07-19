-- Richer results for reporting: total marks available, pass/fail outcome, and
-- an attempt counter. Also promote user_id to a real foreign key.
ALTER TABLE quiz_results
    ADD COLUMN total_marks    FLOAT NOT NULL DEFAULT 0 AFTER total_obtained_marks,
    ADD COLUMN is_passed      BIT   NOT NULL DEFAULT 0 AFTER total_marks,
    ADD COLUMN attempt_number INT   NOT NULL DEFAULT 1 AFTER is_passed;

-- Index user lookups (per-student reports) before adding the FK constraint.
CREATE INDEX idx_quiz_results_user_id ON quiz_results (user_id);

ALTER TABLE quiz_results
    ADD CONSTRAINT fk_quiz_results_user FOREIGN KEY (user_id) REFERENCES users (user_id);
