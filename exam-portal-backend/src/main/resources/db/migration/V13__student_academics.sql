-- Academic details a student fills in during onboarding (step 2), after
-- basic info (name/phone/school) is complete. Nullable: completeness is
-- computed from whether these are filled, not tracked with a separate flag.
ALTER TABLE users
    ADD COLUMN grade       VARCHAR(20)  NULL AFTER school_type,
    ADD COLUMN board       VARCHAR(50)  NULL AFTER grade,
    ADD COLUMN school_name VARCHAR(255) NULL AFTER board;
