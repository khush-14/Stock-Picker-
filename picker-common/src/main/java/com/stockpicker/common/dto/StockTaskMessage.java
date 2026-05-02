package com.stockpicker.common.dto;

import com.stockpicker.common.enums.TaskType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StockTaskMessage {

    private String ticker;
    private TaskType taskType;

    /** ISO date string for the target date, if applicable */
    private String targetDate;
}
