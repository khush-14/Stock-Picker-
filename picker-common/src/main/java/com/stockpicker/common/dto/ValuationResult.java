package com.stockpicker.common.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ValuationResult {

    private String ticker;
    private String companyName;

    /** 0-4 score: 0 = overvalued, 4 = deeply undervalued */
    private int undervaluedScore;
    private String verdict;

    /** Breakdown of each scoring factor */
    private Map<String, FactorResult> factors;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FactorResult {
        private String factorName;
        private boolean passed;
        private BigDecimal currentValue;
        private BigDecimal threshold;
        private String description;
    }
}
