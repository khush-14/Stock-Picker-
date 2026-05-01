package com.stockpicker.api.scheduler;

import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.enums.TaskType;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.stockpicker.common.repository.DailyMetricsRepository;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTaskScheduler {

    private static final String TOPIC = "persistent://public/default/stock-tasks";

    private final StockRepository stockRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final QuarterlyFinancialsRepository quarterlyFinancialsRepository;
    private final PulsarTemplate<StockTaskMessage> pulsarTemplate;

    /**
     * Daily Task: 18:00 IST Monday-Friday to trigger DAILY_PRICE updates.
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduleDailyPriceTasks() {
        log.info("Starting daily price task scheduling");
        List<Stock> stocks = stockRepository.findAll();
        
        for (Stock stock : stocks) {
            try {
                StockTaskMessage message = StockTaskMessage.builder()
                        .ticker(stock.getTicker())
                        .taskType(TaskType.FETCH_PRICE_DAILY)
                        .targetDate(LocalDate.now().toString())
                        .build();

                pulsarTemplate.send(TOPIC, message);
                log.debug("Sent DAILY_PRICE task for {}", stock.getTicker());
            } catch (Exception e) {
                log.error("Failed to send DAILY_PRICE task for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("Finished daily price task scheduling for {} stocks", stocks.size());
    }

    /**
     * Quarterly Task: 10:00 IST on Saturday to trigger QUARTERLY_FINANCIALS.
     */
    @Scheduled(cron = "0 0 10 * * SAT", zone = "Asia/Kolkata")
    public void scheduleQuarterlyFinancialsTasks() {
        log.info("Starting quarterly financials task scheduling");
        List<Stock> stocks = stockRepository.findAll();
        
        for (Stock stock : stocks) {
            try {
                StockTaskMessage message = StockTaskMessage.builder()
                        .ticker(stock.getTicker())
                        .taskType(TaskType.FETCH_FINANCIALS_QUARTERLY)
                        .targetDate(LocalDate.now().toString())
                        .build();

                pulsarTemplate.send(TOPIC, message);
                log.debug("Sent QUARTERLY_FINANCIALS task for {}", stock.getTicker());
            } catch (Exception e) {
                log.error("Failed to send QUARTERLY_FINANCIALS task for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("Finished quarterly financials task scheduling for {} stocks", stocks.size());
    }

    /**
     * Startup Backfill: Triggers missing tasks asynchronously upon application startup.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startupBackfill() {
        log.info("Starting Startup Backfill mechanism...");
        List<Stock> stocks = stockRepository.findAll();
        
        LocalDate today = LocalDate.now();
        LocalDate lastTradingDay = today;
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            lastTradingDay = today.minusDays(1);
        } else if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastTradingDay = today.minusDays(2);
        }

        int currentQuarter = (today.getMonthValue() - 1) / 3 + 1;
        String expectedQuarter = "Q" + currentQuarter;
        int expectedFiscalYear = today.getYear();

        int dailyTasksDispatched = 0;
        int quarterlyTasksDispatched = 0;

        for (Stock stock : stocks) {
            String ticker = stock.getTicker();
            
            // Check Daily Prices
            if (!dailyMetricsRepository.existsByTickerAndRecordDate(ticker, lastTradingDay)) {
                StockTaskMessage dailyMessage = StockTaskMessage.builder()
                        .ticker(ticker)
                        .taskType(TaskType.FETCH_PRICE_DAILY)
                        .targetDate(lastTradingDay.toString())
                        .build();
                try {
                    pulsarTemplate.send(TOPIC, dailyMessage);
                    dailyTasksDispatched++;
                    Thread.sleep(50); // Rate limiting
                } catch (Exception e) {
                    log.error("Failed to backfill DAILY_PRICE for {}: {}", ticker, e.getMessage());
                }
            }

            // Check Quarterly Financials
            if (!quarterlyFinancialsRepository.existsByTickerAndQuarterAndFiscalYear(ticker, expectedQuarter, expectedFiscalYear)) {
                StockTaskMessage quarterlyMessage = StockTaskMessage.builder()
                        .ticker(ticker)
                        .taskType(TaskType.FETCH_FINANCIALS_QUARTERLY)
                        .targetDate(today.toString())
                        .build();
                try {
                    pulsarTemplate.send(TOPIC, quarterlyMessage);
                    quarterlyTasksDispatched++;
                    Thread.sleep(50); // Rate limiting
                } catch (Exception e) {
                    log.error("Failed to backfill QUARTERLY_FINANCIALS for {}: {}", ticker, e.getMessage());
                }
            }
        }

        log.info("Startup Backfill completed. Dispatched {} DAILY_PRICE tasks and {} QUARTERLY_FINANCIALS tasks.", 
                 dailyTasksDispatched, quarterlyTasksDispatched);
    }
}
