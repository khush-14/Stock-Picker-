package com.stockpicker.api.controller;

import com.stockpicker.api.service.AuditService;
import com.stockpicker.api.service.ValuationService;
import com.stockpicker.common.dto.ValuationResult;
import com.stockpicker.common.entity.AuditFlag;
import com.stockpicker.common.repository.AuditFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final ValuationService valuationService;
    private final AuditService auditService;
    private final AuditFlagRepository auditFlagRepository;

    @GetMapping("/{ticker}/valuation")
    public ResponseEntity<ValuationResult> getValuation(@PathVariable String ticker) {
        return ResponseEntity.ok(valuationService.calculateValuation(ticker));
    }

    @GetMapping("/{ticker}/audit-flags")
    public ResponseEntity<List<AuditFlag>> getAuditFlags(@PathVariable String ticker) {
        return ResponseEntity.ok(auditFlagRepository.findByTickerAndResolvedFalseOrderByFlaggedAtDesc(ticker));
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runAnalysis() {
        int flagsCreated = auditService.analyzeAllStocks();
        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "newFlagsCreated", flagsCreated
        ));
    }

    @PostMapping("/run/{ticker}")
    public ResponseEntity<List<AuditFlag>> runAnalysisForTicker(@PathVariable String ticker) {
        return ResponseEntity.ok(auditService.analyzeStock(ticker));
    }
}
