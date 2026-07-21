-- Platform-wide company logo, set once by a SUPER_ADMIN and shown in the header
-- on every page for everyone. Single row (id = 1), base64 PNG data URL.
CREATE TABLE platform_settings (
    id           BIGINT     NOT NULL AUTO_INCREMENT,
    company_logo MEDIUMTEXT NULL,
    PRIMARY KEY (id)
);

INSERT INTO platform_settings (id, company_logo) VALUES (1, NULL);
