package com.stockpicker.ingestor.strategy;

import com.stockpicker.common.dto.StockTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("newsSentimentHandler")
public class NewsSentimentHandler implements TaskHandler {

    @Override
    public void handle(StockTaskMessage message) {
        // Stub implementation — news sentiment scraping will be added in a future phase
        log.info("News sentiment scraping for ticker {} is not yet implemented. Skipping.",
                 message.getTicker());
    }
}
