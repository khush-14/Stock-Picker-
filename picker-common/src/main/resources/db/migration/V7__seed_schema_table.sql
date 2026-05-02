-- V7: Seed schema_table with all filterable/sortable columns

-- Daily Metrics fields
INSERT INTO schema_table (field_name, display_name, source_entity, data_type, filterable, sortable) VALUES
('price', 'Current Price', 'DailyMetrics', 'DECIMAL', true, true),
('pe_ratio', 'P/E Ratio', 'DailyMetrics', 'DECIMAL', true, true),
('pb_ratio', 'P/B Ratio', 'DailyMetrics', 'DECIMAL', true, true),
('debt_to_equity', 'Debt to Equity', 'DailyMetrics', 'DECIMAL', true, true),
('dividend_yield', 'Dividend Yield', 'DailyMetrics', 'DECIMAL', true, true),
('eps', 'Earnings Per Share', 'DailyMetrics', 'DECIMAL', true, true),
('market_cap', 'Market Cap', 'DailyMetrics', 'DECIMAL', true, true),
('volume', 'Volume', 'DailyMetrics', 'INTEGER', true, true);

-- Quarterly Financials fields
INSERT INTO schema_table (field_name, display_name, source_entity, data_type, filterable, sortable) VALUES
('revenue', 'Revenue', 'QuarterlyFinancials', 'DECIMAL', true, true),
('net_profit', 'Net Profit', 'QuarterlyFinancials', 'DECIMAL', true, true),
('operating_profit', 'Operating Profit', 'QuarterlyFinancials', 'DECIMAL', true, true),
('cash_flow_from_operations', 'Cash Flow from Ops', 'QuarterlyFinancials', 'DECIMAL', true, true),
('promoter_holding', 'Promoter Holding %', 'QuarterlyFinancials', 'DECIMAL', true, true),
('promoter_pledging', 'Promoter Pledging %', 'QuarterlyFinancials', 'DECIMAL', true, true);

-- Stock metadata fields
INSERT INTO schema_table (field_name, display_name, source_entity, data_type, filterable, sortable) VALUES
('industry', 'Industry', 'Stock', 'STRING', true, true),
('market_cap_category', 'Cap Category', 'Stock', 'STRING', true, true),
('exchange', 'Exchange', 'Stock', 'STRING', true, false);
