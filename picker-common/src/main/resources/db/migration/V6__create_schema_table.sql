-- V6: Create schema_table for dynamic UI field definitions
CREATE TABLE IF NOT EXISTS schema_table (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    field_name    VARCHAR(50) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    source_entity VARCHAR(50) NOT NULL,
    data_type     VARCHAR(20) NOT NULL,
    filterable    BOOLEAN NOT NULL DEFAULT TRUE,
    sortable      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_schema_field_name ON schema_table (field_name);
CREATE INDEX IF NOT EXISTS idx_schema_source_entity ON schema_table (source_entity);
