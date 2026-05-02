package com.stockpicker.common.dto;

import com.stockpicker.common.enums.FilterOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FilterCriteria {

    @NotBlank(message = "Field name is required")
    private String field;

    @NotNull(message = "Operator is required")
    private FilterOperator operator;

    @NotNull(message = "Value is required")
    private Object value;

    /** Used only for BETWEEN operator */
    private Object valueTo;
}
