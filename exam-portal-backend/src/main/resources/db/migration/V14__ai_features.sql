-- Platform-wide LLM settings, configured once by a SUPER_ADMIN. Single row
-- (id = 1). Key is stored server-side and never returned by any API. Uses an
-- OpenAI-compatible chat API so any compatible provider works via base_url.
CREATE TABLE platform_ai_settings (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    provider    VARCHAR(50)  NOT NULL DEFAULT 'openai',
    base_url    VARCHAR(255) NOT NULL DEFAULT 'https://api.openai.com/v1',
    model       VARCHAR(100) NOT NULL DEFAULT 'gpt-4o-mini',
    api_key     VARCHAR(255) NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO platform_ai_settings (id, provider, base_url, model, api_key)
VALUES (1, 'openai', 'https://api.openai.com/v1', 'gpt-4o-mini', NULL);

-- Cached AI narrative for a report, so we don't re-call the LLM on every view.
ALTER TABLE psychometric_reports
    ADD COLUMN ai_summary TEXT NULL AFTER quot_sq;
