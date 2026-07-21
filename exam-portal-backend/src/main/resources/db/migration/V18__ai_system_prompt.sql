-- Editable system prompt driving the psychometric-report AI narrative.
-- NULL/blank falls back to the built-in default in code.
ALTER TABLE platform_ai_settings
    ADD COLUMN system_prompt TEXT NULL AFTER model;
