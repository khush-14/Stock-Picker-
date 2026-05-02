-- V8: Create user-related tables (app_users, watchlists, user_preferences)

CREATE TABLE IF NOT EXISTS app_users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username   VARCHAR(50) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS watchlists (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS watchlist_tickers (
    watchlist_id BIGINT NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    ticker       VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlists (user_id);
CREATE INDEX IF NOT EXISTS idx_watchlist_tickers_wid ON watchlist_tickers (watchlist_id);

CREATE TABLE IF NOT EXISTS user_preferences (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    preference_name VARCHAR(100) NOT NULL,
    filter_criteria TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pref_user ON user_preferences (user_id);
