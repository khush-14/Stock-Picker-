package com.stockpicker.api.controller;

import com.stockpicker.api.service.CompareService;
import com.stockpicker.common.dto.CompareResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    /**
     * Side-by-side comparison of two tickers.
     * Returns current metrics + last 4 quarters of financial data.
     */
    @GetMapping
    public ResponseEntity<CompareResponse> compare(
            @RequestParam String tickerA,
            @RequestParam String tickerB) {
        return ResponseEntity.ok(compareService.compare(tickerA, tickerB));
    }
}
