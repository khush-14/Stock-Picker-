package com.stockpicker.ingestor.strategy;

import com.stockpicker.common.enums.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskHandlerFactory {

    private final Map<String, TaskHandler> handlers;

    public TaskHandler getHandler(TaskType taskType) {
        String beanName = switch (taskType) {
            case FETCH_PRICE_DAILY -> "dailyPriceHandler";
            case FETCH_FINANCIALS_QUARTERLY -> "quarterlyFinancialsHandler";
            case SCRAPE_NEWS_SENTIMENT -> "newsSentimentHandler";
        };

        TaskHandler handler = handlers.get(beanName);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for task type: " + taskType);
        }
        return handler;
    }
}
