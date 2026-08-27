-- Preserve the original super-admin identity when an impersonated session
-- rotates its access/refresh tokens. Normal sessions keep this column NULL.
ALTER TABLE refresh_tokens
    ADD COLUMN impersonator_id BIGINT NULL AFTER user_id;
