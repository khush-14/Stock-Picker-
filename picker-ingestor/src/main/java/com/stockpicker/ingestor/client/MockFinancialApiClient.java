package com.stockpicker.ingestor.client;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Mock Financial API client that generates realistic dummy data.
 * Replace with actual API integration (e.g., Alpha Vantage, Yahoo Finance) in production.
 */
@Slf4j
@Component
public class MockFinancialApiClient {

    private final Random random = new Random();

    public DailyMetricsData fetchDailyMetrics(String ticker) {
        simulateLatency();
        log.debug("Mock API: Generating daily metrics for {}", ticker);

        return DailyMetricsData.builder()
            .price(randomDecimal(50, 5000))
            .peRatio(randomDecimal(5, 80))
            .pbRatio(randomDecimal(0.5, 15))
            .debtToEquity(randomDecimal(0, 3))
            .dividendYield(randomDecimal(0, 5))
            .eps(randomDecimal(1, 200))
            .marketCap(randomDecimal(1000, 1000000))
            .volume(random.nextLong(100000, 50000000))
            .build();
    }

    public QuarterlyFinancialsData fetchQuarterlyFinancials(String ticker) {
        simulateLatency();
        log.debug("Mock API: Generating quarterly financials for {}", ticker);

        BigDecimal revenue = randomDecimal(100, 50000);
        BigDecimal netProfit = revenue.multiply(BigDecimal.valueOf(random.nextDouble(0.05, 0.25)));
        BigDecimal operatingProfit = revenue.multiply(BigDecimal.valueOf(random.nextDouble(0.10, 0.35)));
        BigDecimal cashFlow = netProfit.multiply(BigDecimal.valueOf(random.nextDouble(0.7, 1.5)));

        String[] auditors = {"Deloitte", "PricewaterhouseCoopers", "Ernst & Young", "KPMG", "BDO India"};

        return QuarterlyFinancialsData.builder()
            .revenue(revenue.setScale(2, RoundingMode.HALF_UP))
            .netProfit(netProfit.setScale(2, RoundingMode.HALF_UP))
            .operatingProfit(operatingProfit.setScale(2, RoundingMode.HALF_UP))
            .cashFlowFromOperations(cashFlow.setScale(2, RoundingMode.HALF_UP))
            .promoterHolding(randomDecimal(30, 75))
            .promoterPledging(randomDecimal(0, 20))
            .auditorName(auditors[random.nextInt(auditors.length)])
            .build();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(random.nextLong(100, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private BigDecimal randomDecimal(double min, double max) {
        double value = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    @Getter @Setter @Builder
    public static class DailyMetricsData {
        private BigDecimal price;
        private BigDecimal peRatio;
        private BigDecimal pbRatio;
        private BigDecimal debtToEquity;
        private BigDecimal dividendYield;
        private BigDecimal eps;
        private BigDecimal marketCap;
        private Long volume;
    }

    @Getter @Setter @Builder
    public static class QuarterlyFinancialsData {
        private BigDecimal revenue;
        private BigDecimal netProfit;
        private BigDecimal operatingProfit;
        private BigDecimal cashFlowFromOperations;
        private BigDecimal promoterHolding;
        private BigDecimal promoterPledging;
        private String auditorName;
    }
}
