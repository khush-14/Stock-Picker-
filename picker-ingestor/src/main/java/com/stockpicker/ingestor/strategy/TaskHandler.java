package com.stockpicker.ingestor.strategy;

import com.stockpicker.common.dto.StockTaskMessage;

/**
 * Strategy interface for handling different stock data task types.
 */
public interface TaskHandler {
    void handle(StockTaskMessage message);
}
