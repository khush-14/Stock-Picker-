package com.stockpicker.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StockFilterRequest {

    @Valid
    private List<FilterCriteria> criteria;

    @Min(0)
    @Builder.Default
    private int page = 0;

    @Min(1) @Max(200)
    @Builder.Default
    private int size = 50;

    /** Field name to sort by (must exist in SchemaTable) */
    private String sortBy;

    /** Sort direction: ASC or DESC */
    @Builder.Default
    private String sortDirection = "ASC";
}
