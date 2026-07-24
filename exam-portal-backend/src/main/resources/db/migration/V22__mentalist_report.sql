-- The Mentalist PDF report: student profile fields not previously captured,
-- plus a table tracking one generated report per quiz attempt.
ALTER TABLE users ADD COLUMN father_name VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN mother_name VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN dob DATE NULL;
ALTER TABLE users ADD COLUMN city VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN gender VARCHAR(20) NULL;

CREATE TABLE mentalist_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_res_id BIGINT NOT NULL,
    report_number VARCHAR(40) NOT NULL,
    counsellor_name VARCHAR(100) NULL,
    counsellor_remarks TEXT NULL,
    pdf_path VARCHAR(500) NOT NULL,
    ai_content MEDIUMTEXT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentalist_reports_quiz_result FOREIGN KEY (quiz_res_id) REFERENCES quiz_results (quiz_res_id),
    CONSTRAINT uq_mentalist_reports_quiz_res UNIQUE (quiz_res_id)
);
