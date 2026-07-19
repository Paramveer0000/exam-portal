-- Optional per-quiz exam timer, configured by the owning school (admin).
-- timer_enabled toggles the timer; timer_minutes is the exam duration and is
-- only meaningful (and required by the service layer) when the timer is on.
ALTER TABLE quizzes
    ADD COLUMN timer_enabled BIT NOT NULL DEFAULT 0 AFTER passing_percentage,
    ADD COLUMN timer_minutes INT NULL AFTER timer_enabled;
