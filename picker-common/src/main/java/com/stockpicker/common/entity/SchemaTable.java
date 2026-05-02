package com.stockpicker.common.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Dynamic schema metadata — maps DB columns to human-readable names for the UI.
 * Used by the filter engine to validate and display available filter fields.
 */
@Entity
@Table(name = "schema_table", indexes = {
    @Index(name = "idx_schema_field_name", columnList = "field_name"),
    @Index(name = "idx_schema_source_entity", columnList = "source_entity")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SchemaTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_name", nullable = false, unique = true, length = 50)
    private String fieldName;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "source_entity", nullable = false, length = 50)
    private String sourceEntity;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "filterable", nullable = false)
    @Builder.Default
    private Boolean filterable = true;

    @Column(name = "sortable", nullable = false)
    @Builder.Default
    private Boolean sortable = true;
}
