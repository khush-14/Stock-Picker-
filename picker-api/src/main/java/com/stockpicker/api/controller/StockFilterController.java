package com.stockpicker.api.controller;

import com.stockpicker.api.service.StockFilterService;
import com.stockpicker.common.dto.StockFilterRequest;
import com.stockpicker.common.dto.StockFilterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockFilterController {

    private final StockFilterService stockFilterService;

    /**
     * Dynamic stock filtering with pagination and sorting.
     * Accepts a list of criteria, each with a field, operator, and value.
     */
    @PostMapping("/filter")
    public ResponseEntity<StockFilterResponse> filterStocks(
            @Valid @RequestBody StockFilterRequest request) {
        StockFilterResponse response = stockFilterService.filterStocks(request);
        return ResponseEntity.ok(response);
    }
}
