-- Optional branding logo for schools (ADMIN) and platform admins (SUPER_ADMIN),
-- stored as a data URL (base64 PNG). Nullable; students don't use it.
ALTER TABLE users
    ADD COLUMN logo MEDIUMTEXT NULL AFTER school_name;
