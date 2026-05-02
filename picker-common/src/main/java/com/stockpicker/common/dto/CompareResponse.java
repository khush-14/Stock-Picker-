package com.stockpicker.common.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CompareResponse {

    private StockComparison stockA;
    private StockComparison stockB;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class StockComparison {
        private String ticker;
        private String companyName;
        private String industry;
        private BigDecimal currentPrice;
        private BigDecimal peRatio;
        private BigDecimal pbRatio;
        private BigDecimal debtToEquity;
        private List<QuarterData> quarters;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class QuarterData {
        private String quarter;
        private Integer fiscalYear;
        private BigDecimal revenue;
        private BigDecimal netProfit;
        private BigDecimal operatingProfit;
        private BigDecimal cashFlowFromOperations;
        private BigDecimal promoterHolding;
    }
}
