package com.stockpicker.ingestor.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.common.enums.TaskType;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Java-native scheduler that replaces the Go stock-scheduler microservice.
 * Produces StockTaskMessage to Apache Pulsar on a cron schedule.
 *
 * Daily:     18:00 IST Mon-Fri  → FETCH_PRICE_DAILY for all tickers
 * Weekly:    10:00 IST Saturday → FETCH_FINANCIALS_QUARTERLY (scrape Screener.in)
 * Quarterly: 06:00 IST 1st of Jan/Apr/Jul/Oct → FETCH_FINANCIALS_QUARTERLY
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTaskScheduler {

    private static final String TOPIC = "persistent://public/default/stock-tasks";

    private final StockRepository stockRepository;
    private final PulsarTemplate<byte[]> pulsarTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Daily cron: 18:00 IST, Monday to Friday.
     * Triggers price fetch for all active tickers.
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Kolkata")
    public void dailyPriceFetch() {
        log.info("[SCHEDULER] Daily price fetch triggered at 18:00 IST");
        publishForAllTickers(TaskType.FETCH_PRICE_DAILY);
    }

    /**
     * Weekly cron: Saturday 10:00 IST.
     * Triggers financials scrape from Screener.in for all tickers.
     */
    @Scheduled(cron = "0 0 10 * * SAT", zone = "Asia/Kolkata")
    public void weeklyFinancialsScrape() {
        log.info("[SCHEDULER] Weekly financials scrape triggered (Saturday 10:00 IST)");
        publishForAllTickers(TaskType.FETCH_FINANCIALS_QUARTERLY);
    }

    /**
     * Quarterly cron: 1st of Jan, Apr, Jul, Oct at 06:00 IST.
     * Full quarterly data refresh.
     */
    @Scheduled(cron = "0 0 6 1 1,4,7,10 *", zone = "Asia/Kolkata")
    public void quarterlyFullRefresh() {
        log.info("[SCHEDULER] Quarterly full refresh triggered");
        publishForAllTickers(TaskType.FETCH_FINANCIALS_QUARTERLY);
    }

    /**
     * Publish a task for ALL active tickers.
     * Each ticker is processed independently — one failure doesn't stop the batch.
     */
    public void publishForAllTickers(TaskType taskType) {
        var stocks = stockRepository.findAll();
        log.info("[SCHEDULER] Publishing {} tasks for {} tickers", taskType, stocks.size());

        int success = 0;
        int failed = 0;

        for (var stock : stocks) {
            try {
                publishTask(stock.getTicker(), taskType);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("[SCHEDULER] Failed to publish {} for {}: {}",
                          taskType, stock.getTicker(), e.getMessage());
            }
        }

        log.info("[SCHEDULER] Batch complete: {} succeeded, {} failed", success, failed);
    }

    /**
     * Publish a single task message to the Pulsar topic.
     */
    public void publishTask(String ticker, TaskType taskType) {
        try {
            StockTaskMessage message = StockTaskMessage.builder()
                .ticker(ticker)
                .taskType(taskType)
                .targetDate(LocalDate.now().toString())
                .build();

            byte[] payload = objectMapper.writeValueAsBytes(message);
            pulsarTemplate.send(TOPIC, payload);

            log.debug("[SCHEDULER] Published {} for {}", taskType, ticker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message for " + ticker, e);
        }
    }
}
