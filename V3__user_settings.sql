-- Flyway migration V3: user_settings table
CREATE TABLE IF NOT EXISTS user_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(16),
    locale VARCHAR(16),
    number_format VARCHAR(32),
    page_size INT,
    saved_filters TEXT
);

-- Helpful index for joins
CREATE INDEX IF NOT EXISTS idx_user_settings_user_id ON user_settings(user_id);
