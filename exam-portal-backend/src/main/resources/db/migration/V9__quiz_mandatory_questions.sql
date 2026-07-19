-- When on, a student must answer every served question before they can submit
-- the exam. Configured by the owning school on the Add/Update Subject page.
ALTER TABLE quizzes
    ADD COLUMN all_questions_mandatory BIT NOT NULL DEFAULT 0 AFTER timer_minutes;
