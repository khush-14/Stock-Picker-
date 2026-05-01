package com.stockpicker.api.controller;

import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.common.enums.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String TOPIC = "persistent://public/default/stock-tasks";
    private final PulsarTemplate<StockTaskMessage> pulsarTemplate;

    @PostMapping("/trigger/{ticker}")
    public ResponseEntity<String> triggerTasksForTicker(@PathVariable String ticker) {
        log.info("Manually triggering tasks for ticker: {}", ticker);

        try {
            // Trigger Daily Price
            StockTaskMessage priceMessage = StockTaskMessage.builder()
                    .ticker(ticker)
                    .taskType(TaskType.FETCH_PRICE_DAILY)
                    .targetDate(LocalDate.now().toString())
                    .build();
            pulsarTemplate.send(TOPIC, priceMessage);

            // Trigger Quarterly Financials
            StockTaskMessage financialsMessage = StockTaskMessage.builder()
                    .ticker(ticker)
                    .taskType(TaskType.FETCH_FINANCIALS_QUARTERLY)
                    .targetDate(LocalDate.now().toString())
                    .build();
            pulsarTemplate.send(TOPIC, financialsMessage);

            return ResponseEntity.ok("Successfully injected tasks for " + ticker + " into Pulsar.");
        } catch (Exception e) {
            log.error("Failed to inject tasks for {}: {}", ticker, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
