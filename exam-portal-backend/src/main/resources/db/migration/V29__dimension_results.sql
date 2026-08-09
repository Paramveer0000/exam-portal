-- Generic per-dimension result store, one row per (attempt, dimension) actually
-- answered. Additive only -- existing psychometric_reports mi_*/riasec_*/quot_*
-- columns and scoring are untouched; this table is a NEW, separate persistence
-- target so old reports keep rendering exactly as before (no rows exist for
-- them, callers fall back to prior behaviour).
--
-- Phase A only populates rows for EQ/LEADERSHIP dimension_type codes (the ones
-- already computed by PsychometricReportServiceImpl.scoreAndPersist but
-- previously discarded). Generic shape so future dimension types can reuse it
-- without another migration.
CREATE TABLE dimension_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_res_id BIGINT NOT NULL,
    dimension_code VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    raw_score DOUBLE NOT NULL,
    max_score DOUBLE NOT NULL,
    percentage DOUBLE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dimension_results_quiz_res FOREIGN KEY (quiz_res_id) REFERENCES quiz_results (quiz_res_id),
    CONSTRAINT fk_dimension_results_dimension FOREIGN KEY (dimension_code) REFERENCES dimensions (dimension_code),
    CONSTRAINT uq_dimension_results_attempt_dim UNIQUE (quiz_res_id, dimension_code)
);
