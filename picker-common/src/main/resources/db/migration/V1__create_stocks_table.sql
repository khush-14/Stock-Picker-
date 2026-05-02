-- V1: Enable TimescaleDB extension and create stocks table
CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS stocks (
    ticker        VARCHAR(20) PRIMARY KEY,
    company_name  VARCHAR(255) NOT NULL,
    industry      VARCHAR(100),
    market_cap_category VARCHAR(20),
    exchange      VARCHAR(10),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stocks_industry ON stocks (industry);
CREATE INDEX IF NOT EXISTS idx_stocks_market_cap ON stocks (market_cap_category);
