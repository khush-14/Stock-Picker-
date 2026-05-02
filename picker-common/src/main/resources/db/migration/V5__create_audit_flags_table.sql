-- V5: Create audit_flags table for fraud/red flag tracking
CREATE TABLE IF NOT EXISTS audit_flags (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker      VARCHAR(20) NOT NULL REFERENCES stocks(ticker),
    flag_type   VARCHAR(40) NOT NULL,
    severity    VARCHAR(10) NOT NULL,
    description TEXT,
    quarter     VARCHAR(10),
    flagged_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_audit_ticker ON audit_flags (ticker);
CREATE INDEX IF NOT EXISTS idx_audit_flag_type ON audit_flags (flag_type);
CREATE INDEX IF NOT EXISTS idx_audit_severity ON audit_flags (severity);
