-- V2: Create daily_metrics table (will be converted to hypertable in V3)
CREATE TABLE IF NOT EXISTS daily_metrics (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    ticker         VARCHAR(20) NOT NULL REFERENCES stocks(ticker),
    record_date    DATE NOT NULL,
    price          NUMERIC(12, 4),
    pe_ratio       NUMERIC(10, 4),
    pb_ratio       NUMERIC(10, 4),
    debt_to_equity NUMERIC(10, 4),
    dividend_yield NUMERIC(8, 4),
    eps            NUMERIC(10, 4),
    market_cap     NUMERIC(18, 2),
    volume         BIGINT
);

-- Unique constraint includes partition column for TimescaleDB compatibility
CREATE UNIQUE INDEX IF NOT EXISTS uq_daily_ticker_date ON daily_metrics (ticker, record_date);
CREATE INDEX IF NOT EXISTS idx_daily_ticker ON daily_metrics (ticker);
CREATE INDEX IF NOT EXISTS idx_daily_date ON daily_metrics (record_date);
