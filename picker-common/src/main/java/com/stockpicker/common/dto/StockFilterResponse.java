package com.stockpicker.common.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StockFilterResponse {

    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private List<StockRow> stocks;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class StockRow {
        private String ticker;
        private String companyName;
        private String industry;
        private String marketCapCategory;
        private String exchange;

        // Latest daily metrics
        private LocalDate metricsDate;
        private BigDecimal price;
        private BigDecimal peRatio;
        private BigDecimal pbRatio;
        private BigDecimal debtToEquity;
        private BigDecimal dividendYield;
        private BigDecimal eps;
        private BigDecimal marketCap;
        private Long volume;

        // Latest quarterly financials
        private String latestQuarter;
        private Integer fiscalYear;
        private BigDecimal revenue;
        private BigDecimal netProfit;
        private BigDecimal operatingProfit;
        private BigDecimal promoterHolding;
        private BigDecimal promoterPledging;
    }
}
