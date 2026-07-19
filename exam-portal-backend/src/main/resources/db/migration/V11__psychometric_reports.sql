-- One computed psychometric profile per exam attempt, persisted at submit time
-- so the report page is a cheap read (no rescoring).
CREATE TABLE psychometric_reports (
    report_id            BIGINT       NOT NULL AUTO_INCREMENT,
    quiz_res_id          BIGINT       NOT NULL,

    -- Multiple-intelligence share of total, in percent (all 9 sum to ~100).
    mi_logical           DOUBLE       NOT NULL DEFAULT 0,
    mi_musical           DOUBLE       NOT NULL DEFAULT 0,
    mi_naturalist        DOUBLE       NOT NULL DEFAULT 0,
    mi_verbal            DOUBLE       NOT NULL DEFAULT 0,
    mi_interpersonal     DOUBLE       NOT NULL DEFAULT 0,
    mi_kinesthetic       DOUBLE       NOT NULL DEFAULT 0,
    mi_spatial           DOUBLE       NOT NULL DEFAULT 0,
    mi_intrapersonal     DOUBLE       NOT NULL DEFAULT 0,
    mi_existential       DOUBLE       NOT NULL DEFAULT 0,

    -- RIASEC interest scores on a 0-10 scale (mean of that letter's questions).
    riasec_r             DOUBLE       NOT NULL DEFAULT 0,
    riasec_i             DOUBLE       NOT NULL DEFAULT 0,
    riasec_a             DOUBLE       NOT NULL DEFAULT 0,
    riasec_s             DOUBLE       NOT NULL DEFAULT 0,
    riasec_e             DOUBLE       NOT NULL DEFAULT 0,
    riasec_c             DOUBLE       NOT NULL DEFAULT 0,

    -- Derived quotients in percent (formulas documented in the scoring service).
    quot_iq              DOUBLE       NOT NULL DEFAULT 0,
    quot_eq              DOUBLE       NOT NULL DEFAULT 0,
    quot_aq              DOUBLE       NOT NULL DEFAULT 0,
    quot_cq              DOUBLE       NOT NULL DEFAULT 0,
    quot_sq              DOUBLE       NOT NULL DEFAULT 0,

    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (report_id),
    UNIQUE KEY uq_psych_report_attempt (quiz_res_id),
    CONSTRAINT fk_psych_report_result FOREIGN KEY (quiz_res_id)
        REFERENCES quiz_results (quiz_res_id) ON DELETE CASCADE
);
