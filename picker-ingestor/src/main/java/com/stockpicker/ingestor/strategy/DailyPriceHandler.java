package com.stockpicker.ingestor.strategy;

import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.common.entity.DailyMetrics;
import com.stockpicker.common.repository.DailyMetricsRepository;
import com.stockpicker.ingestor.client.ScreenerInScraper;
import com.stockpicker.ingestor.client.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Fetches daily price from Yahoo Finance and key ratios from Screener.in,
 * then saves to DailyMetrics (TimescaleDB hypertable).
 */
@Slf4j
@Component("dailyPriceHandler")
@RequiredArgsConstructor
public class DailyPriceHandler implements TaskHandler {

    private final YahooFinanceClient yahooClient;
    private final ScreenerInScraper screenerScraper;
    private final DailyMetricsRepository dailyMetricsRepository;

    @Override
    public void handle(StockTaskMessage message) {
        String ticker = message.getTicker();
        LocalDate targetDate = message.getTargetDate() != null
            ? LocalDate.parse(message.getTargetDate())
            : LocalDate.now();

        // Idempotency: skip if already exists
        if (dailyMetricsRepository.existsByTickerAndRecordDate(ticker, targetDate)) {
            log.info("Daily metrics already exist for {} on {}. Skipping.", ticker, targetDate);
            return;
        }

        log.info("Fetching daily data for {} on {}", ticker, targetDate);

        // Step 1: Get price from Yahoo Finance
        YahooFinanceClient.PriceData priceData = yahooClient.fetchDailyPrice(ticker);

        // Step 2: Get ratios from Screener.in (PE, PB, Debt/Equity)
        ScreenerInScraper.FinancialsData financials = screenerScraper.scrapeFinancials(ticker);

        DailyMetrics metrics = DailyMetrics.builder()
            .ticker(ticker)
            .recordDate(targetDate)
            .price(priceData.getPrice())
            .peRatio(financials.getPeRatio())
            .pbRatio(financials.getPbRatio())
            .debtToEquity(financials.getDebtToEquity())
            .dividendYield(financials.getDividendYield())
            .eps(null) // computed downstream
            .marketCap(null)
            .volume(priceData.getVolume())
            .build();

        dailyMetricsRepository.save(metrics);
        log.info("Saved daily metrics for {} on {} — price=₹{}", ticker, targetDate, priceData.getPrice());
    }
}
