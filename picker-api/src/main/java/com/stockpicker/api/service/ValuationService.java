package com.stockpicker.api.service;

import com.stockpicker.common.dto.ValuationResult;
import com.stockpicker.common.entity.DailyMetrics;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.repository.DailyMetricsRepository;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Calculates undervalued scores (0–5) based on:
 * 1. Current PE < 15
 * 2. Debt-to-Equity < 1
 * 3. Consistent QoQ Revenue Growth
 * 4. P/B Ratio < 3
 * 5. Current PE < 5-year Median PE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationService {

    private final StockRepository stockRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final QuarterlyFinancialsRepository quarterlyFinancialsRepository;

    @Transactional(readOnly = true)
    public ValuationResult calculateValuation(String ticker) {
        Stock stock = stockRepository.findById(ticker)
            .orElseThrow(() -> new NoSuchElementException("Stock not found: " + ticker));

        DailyMetrics latest = dailyMetricsRepository.findLatestByTicker(ticker)
            .orElseThrow(() -> new NoSuchElementException("No daily metrics found for: " + ticker));

        Map<String, ValuationResult.FactorResult> factors = new LinkedHashMap<>();
        int score = 0;

        // Factor 1: Current PE < 15 (absolute value check)
        if (latest.getPeRatio() != null) {
            BigDecimal threshold = BigDecimal.valueOf(15);
            boolean passed = latest.getPeRatio().compareTo(threshold) < 0
                && latest.getPeRatio().compareTo(BigDecimal.ZERO) > 0;
            if (passed) score++;

            factors.put("pe_absolute", ValuationResult.FactorResult.builder()
                .factorName("P/E Ratio Below 15")
                .passed(passed)
                .currentValue(latest.getPeRatio())
                .threshold(threshold)
                .description(passed
                    ? "P/E is in value territory (below 15)"
                    : "P/E exceeds value threshold of 15")
                .build());
        }

        // Factor 2: Debt-to-Equity < 1.0
        if (latest.getDebtToEquity() != null) {
            boolean passed = latest.getDebtToEquity().compareTo(BigDecimal.ONE) < 0;
            if (passed) score++;

            factors.put("low_debt", ValuationResult.FactorResult.builder()
                .factorName("Low Debt-to-Equity")
                .passed(passed)
                .currentValue(latest.getDebtToEquity())
                .threshold(BigDecimal.ONE)
                .description(passed ? "Company has conservative leverage" : "Company is highly leveraged")
                .build());
        }

        // Factor 3: Consistent QoQ Revenue Growth (last 3+ quarters)
        List<QuarterlyFinancials> quarters = quarterlyFinancialsRepository
            .findLast4QuartersByTicker(ticker);

        if (quarters.size() >= 3) {
            boolean consistentGrowth = true;
            for (int i = 0; i < quarters.size() - 1; i++) {
                BigDecimal current = quarters.get(i).getRevenue();
                BigDecimal previous = quarters.get(i + 1).getRevenue();
                if (current == null || previous == null
                    || current.compareTo(previous) <= 0) {
                    consistentGrowth = false;
                    break;
                }
            }

            if (consistentGrowth) score++;

            factors.put("revenue_growth", ValuationResult.FactorResult.builder()
                .factorName("Consistent QoQ Revenue Growth")
                .passed(consistentGrowth)
                .currentValue(quarters.get(0).getRevenue())
                .threshold(quarters.get(quarters.size() - 1).getRevenue())
                .description(consistentGrowth
                    ? "Revenue growing consistently across last " + quarters.size() + " quarters"
                    : "Revenue growth not consistent across quarters")
                .build());
        }

        // Factor 4: P/B Ratio < 3
        if (latest.getPbRatio() != null) {
            BigDecimal threshold = BigDecimal.valueOf(3);
            boolean passed = latest.getPbRatio().compareTo(threshold) < 0;
            if (passed) score++;

            factors.put("pb_ratio", ValuationResult.FactorResult.builder()
                .factorName("P/B Ratio Below 3")
                .passed(passed)
                .currentValue(latest.getPbRatio())
                .threshold(threshold)
                .description(passed ? "Trading below 3x book value" : "Premium to book value")
                .build());
        }

        // Factor 5: Current PE < 5-year Median PE
        Optional<Double> medianPe = dailyMetricsRepository.findMedianPeRatio(
            ticker, LocalDate.now().minusYears(5));

        if (medianPe.isPresent() && latest.getPeRatio() != null) {
            BigDecimal median = BigDecimal.valueOf(medianPe.get());
            boolean passed = latest.getPeRatio().compareTo(median) < 0;
            if (passed) score++;

            factors.put("pe_below_median", ValuationResult.FactorResult.builder()
                .factorName("P/E Below 5-Year Median")
                .passed(passed)
                .currentValue(latest.getPeRatio())
                .threshold(median)
                .description(passed
                    ? "Currently trading below its historical P/E median — undervalued signal"
                    : "Currently above historical P/E median")
                .build());
        }

        String verdict = switch (score) {
            case 5 -> "DEEPLY UNDERVALUED";
            case 4 -> "UNDERVALUED";
            case 3 -> "FAIRLY VALUED";
            case 2 -> "SLIGHTLY OVERVALUED";
            case 1 -> "OVERVALUED";
            default -> "AVOID";
        };

        return ValuationResult.builder()
            .ticker(ticker)
            .companyName(stock.getCompanyName())
            .undervaluedScore(score)
            .verdict(verdict)
            .factors(factors)
            .build();
    }
}
