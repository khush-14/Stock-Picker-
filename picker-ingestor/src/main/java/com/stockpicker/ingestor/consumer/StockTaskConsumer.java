package com.stockpicker.ingestor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.ingestor.strategy.TaskHandlerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockTaskConsumer {

    private final TaskHandlerFactory taskHandlerFactory;
    private final ObjectMapper objectMapper;

    @PulsarListener(
        topics = "persistent://public/default/stock-tasks",
        subscriptionName = "ingestor-subscription"
    )
    public void consume(Message<byte[]> message) {
        try {
            String payload = new String(message.getData());
            log.info("Received Pulsar message: {}", payload);

            StockTaskMessage taskMessage = objectMapper.readValue(payload, StockTaskMessage.class);

            var handler = taskHandlerFactory.getHandler(taskMessage.getTaskType());
            handler.handle(taskMessage);

            log.info("Successfully processed task: {} for ticker: {}",
                     taskMessage.getTaskType(), taskMessage.getTicker());
        } catch (Exception e) {
            log.error("Failed to process Pulsar message: {}", e.getMessage(), e);
            // Message will be negatively acknowledged → retry / DLQ
            throw new RuntimeException("Message processing failed", e);
        }
    }
}
