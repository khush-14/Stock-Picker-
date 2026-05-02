-- V4: Create quarterly_financials table
CREATE TABLE IF NOT EXISTS quarterly_financials (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker                    VARCHAR(20) NOT NULL REFERENCES stocks(ticker),
    quarter                   VARCHAR(10) NOT NULL,
    fiscal_year               INTEGER NOT NULL,
    report_date               DATE,
    revenue                   NUMERIC(18, 2),
    net_profit                NUMERIC(18, 2),
    operating_profit          NUMERIC(18, 2),
    cash_flow_from_operations NUMERIC(18, 2),
    promoter_holding          NUMERIC(6, 2),
    promoter_pledging         NUMERIC(6, 2),
    auditor_name              VARCHAR(200),
    CONSTRAINT uq_quarterly_ticker_quarter_year UNIQUE (ticker, quarter, fiscal_year)
);

CREATE INDEX IF NOT EXISTS idx_quarterly_ticker ON quarterly_financials (ticker);
CREATE INDEX IF NOT EXISTS idx_quarterly_report_date ON quarterly_financials (report_date);
