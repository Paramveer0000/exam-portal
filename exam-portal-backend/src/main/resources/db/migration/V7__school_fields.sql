-- School (admin) profile details.
ALTER TABLE users
    ADD COLUMN address     VARCHAR(500) NULL,
    ADD COLUMN school_type VARCHAR(100) NULL;
