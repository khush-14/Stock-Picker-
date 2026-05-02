package com.stockpicker.ingestor.controller;

import com.stockpicker.common.enums.TaskType;
import com.stockpicker.ingestor.scheduler.StockTaskScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoint for manually triggering data ingestion tasks.
 * Useful for debugging and testing individual tickers.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminTriggerController {

    private final StockTaskScheduler scheduler;

    /**
     * Manual trigger: push all task types for a specific ticker.
     */
    @PostMapping("/trigger/{ticker}")
    public ResponseEntity<Map<String, Object>> triggerTicker(@PathVariable String ticker) {
        scheduler.publishTask(ticker, TaskType.FETCH_PRICE_DAILY);
        scheduler.publishTask(ticker, TaskType.FETCH_FINANCIALS_QUARTERLY);
        scheduler.publishTask(ticker, TaskType.SCRAPE_NEWS_SENTIMENT);

        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "ticker", ticker,
            "tasksPublished", 3,
            "message", "Full refresh triggered for " + ticker
        ));
    }

    /**
     * Trigger a specific task type for a ticker.
     */
    @PostMapping("/trigger/{ticker}/{taskType}")
    public ResponseEntity<Map<String, Object>> triggerSpecific(
            @PathVariable String ticker,
            @PathVariable TaskType taskType) {
        scheduler.publishTask(ticker, taskType);

        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "ticker", ticker,
            "taskType", taskType.name()
        ));
    }

    /**
     * Trigger all tickers for a given task type.
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<Map<String, Object>> triggerAll(
            @RequestParam(defaultValue = "FETCH_PRICE_DAILY") TaskType taskType) {
        scheduler.publishForAllTickers(taskType);

        return ResponseEntity.accepted().body(Map.of(
            "status", "accepted",
            "taskType", taskType.name(),
            "message", "Batch trigger submitted"
        ));
    }
}
