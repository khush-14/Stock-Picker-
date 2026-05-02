-- V3: Convert daily_metrics into a TimescaleDB hypertable
-- Partitioned by record_date with a 7-day chunk interval for optimal time-series performance
SELECT create_hypertable(
    'daily_metrics',
    by_range('record_date', INTERVAL '7 days'),
    if_not_exists => TRUE
);
