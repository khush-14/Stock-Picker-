package com.stockpicker.api.service;

import com.stockpicker.common.entity.AuditFlag;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.enums.FlagType;
import com.stockpicker.common.enums.Severity;
import com.stockpicker.common.repository.AuditFlagRepository;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fraud detection service with enhanced rules:
 * 1. Promoter pledging increase > 2% QoQ
 * 2. Auditor resignation (name change)
 * 3. Revenue increases but Cash Flow from Operations doesn't
 * 4. Cash Flow consistently negative while Net Profit positive
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditFlagRepository auditFlagRepository;
    private final QuarterlyFinancialsRepository quarterlyRepo;
    private final StockRepository stockRepository;
    private final NewsSentimentService newsSentimentService;

    @Transactional
    public List<AuditFlag> analyzeStock(String ticker) {
        List<QuarterlyFinancials> quarters = quarterlyRepo.findLast4QuartersByTicker(ticker);
        List<AuditFlag> newFlags = new ArrayList<>();

        if (quarters.size() >= 2) {
            QuarterlyFinancials current = quarters.get(0);
            QuarterlyFinancials previous = quarters.get(1);
            String currentQuarter = current.getQuarter() + "-FY" + current.getFiscalYear();

            // Rule 1: Promoter Pledging increase > 2%
            checkPromoterPledging(ticker, current, previous, currentQuarter, newFlags);

            // Rule 2: Auditor change (potential resignation)
            checkAuditorChange(ticker, current, previous, currentQuarter, newFlags);

            // Rule 3: Revenue ↑ but Cash Flow ↓ or flat
            checkRevenueCashFlowMismatch(ticker, current, previous, currentQuarter, newFlags);
        }

        // Rule 4: Consistent negative cash flow + positive profit (check last 4 quarters)
        checkConsistentCashFlowFraud(ticker, quarters, newFlags);

        // Rule 5: News sentiment — fraud/SEBI probe keywords
        checkNewsSentiment(ticker, newFlags);

        if (!newFlags.isEmpty()) {
            auditFlagRepository.saveAll(newFlags);
            log.info("Created {} audit flags for {}", newFlags.size(), ticker);
        }

        return newFlags;
    }

    @Transactional
    public int analyzeAllStocks() {
        List<String> tickers = stockRepository.findAll().stream()
            .map(s -> s.getTicker())
            .toList();

        int totalFlags = 0;
        for (String ticker : tickers) {
            try {
                totalFlags += analyzeStock(ticker).size();
            } catch (Exception e) {
                log.error("Audit analysis failed for {}: {}", ticker, e.getMessage());
            }
        }
        log.info("Audit analysis complete. {} new flags across {} stocks", totalFlags, tickers.size());
        return totalFlags;
    }

    private void checkPromoterPledging(String ticker, QuarterlyFinancials current,
                                        QuarterlyFinancials previous, String quarter,
                                        List<AuditFlag> flags) {
        if (current.getPromoterPledging() == null || previous.getPromoterPledging() == null) return;

        BigDecimal increase = current.getPromoterPledging().subtract(previous.getPromoterPledging());
        // Lowered threshold to 2% for more aggressive fraud detection
        if (increase.compareTo(BigDecimal.valueOf(2)) > 0) {
            if (!auditFlagRepository.existsByTickerAndFlagTypeAndQuarter(
                    ticker, FlagType.PROMOTER_PLEDGE_INCREASE, quarter)) {
                flags.add(AuditFlag.builder()
                    .ticker(ticker)
                    .flagType(FlagType.PROMOTER_PLEDGE_INCREASE)
                    .severity(Severity.HIGH)
                    .description(String.format(
                        "Promoter pledging increased by %.2f%% (from %.2f%% to %.2f%%) — threshold is 2%%",
                        increase, previous.getPromoterPledging(), current.getPromoterPledging()))
                    .quarter(quarter)
                    .flaggedAt(Instant.now())
                    .resolved(false)
                    .build());
            }
        }
    }

    private void checkAuditorChange(String ticker, QuarterlyFinancials current,
                                     QuarterlyFinancials previous, String quarter,
                                     List<AuditFlag> flags) {
        if (current.getAuditorName() == null || previous.getAuditorName() == null) return;

        if (!current.getAuditorName().equalsIgnoreCase(previous.getAuditorName())) {
            if (!auditFlagRepository.existsByTickerAndFlagTypeAndQuarter(
                    ticker, FlagType.AUDITOR_RESIGNATION, quarter)) {
                flags.add(AuditFlag.builder()
                    .ticker(ticker)
                    .flagType(FlagType.AUDITOR_RESIGNATION)
                    .severity(Severity.CRITICAL)
                    .description(String.format(
                        "Auditor changed from '%s' to '%s' — potential resignation red flag",
                        previous.getAuditorName(), current.getAuditorName()))
                    .quarter(quarter)
                    .flaggedAt(Instant.now())
                    .resolved(false)
                    .build());
            }
        }
    }

    private void checkRevenueCashFlowMismatch(String ticker, QuarterlyFinancials current,
                                               QuarterlyFinancials previous, String quarter,
                                               List<AuditFlag> flags) {
        if (current.getRevenue() == null || previous.getRevenue() == null
            || current.getCashFlowFromOperations() == null
            || previous.getCashFlowFromOperations() == null) return;

        boolean revenueUp = current.getRevenue().compareTo(previous.getRevenue()) > 0;
        boolean cashFlowDownOrFlat = current.getCashFlowFromOperations()
            .compareTo(previous.getCashFlowFromOperations()) <= 0;

        if (revenueUp && cashFlowDownOrFlat) {
            if (!auditFlagRepository.existsByTickerAndFlagTypeAndQuarter(
                    ticker, FlagType.REVENUE_CASHFLOW_MISMATCH, quarter)) {
                flags.add(AuditFlag.builder()
                    .ticker(ticker)
                    .flagType(FlagType.REVENUE_CASHFLOW_MISMATCH)
                    .severity(Severity.HIGH)
                    .description(String.format(
                        "Revenue ↑ (₹%.2fCr → ₹%.2fCr) but Cash Flow from Ops ↓ (₹%.2fCr → ₹%.2fCr)",
                        previous.getRevenue(), current.getRevenue(),
                        previous.getCashFlowFromOperations(), current.getCashFlowFromOperations()))
                    .quarter(quarter)
                    .flaggedAt(Instant.now())
                    .resolved(false)
                    .build());
            }
        }
    }

    /**
     * Rule 4: Cash Flow from Operations is consistently negative while Net Profit is positive.
     * Checks across last 4 quarters — if ALL have this mismatch, flag it.
     */
    private void checkConsistentCashFlowFraud(String ticker, List<QuarterlyFinancials> quarters,
                                               List<AuditFlag> flags) {
        if (quarters.size() < 3) return;

        boolean allMismatch = quarters.stream().allMatch(q ->
            q.getNetProfit() != null && q.getCashFlowFromOperations() != null
            && q.getNetProfit().compareTo(BigDecimal.ZERO) > 0
            && q.getCashFlowFromOperations().compareTo(BigDecimal.ZERO) < 0);

        if (allMismatch) {
            String quarter = quarters.get(0).getQuarter() + "-FY" + quarters.get(0).getFiscalYear();
            if (!auditFlagRepository.existsByTickerAndFlagTypeAndQuarter(
                    ticker, FlagType.REVENUE_CASHFLOW_MISMATCH, quarter + "-PERSISTENT")) {
                flags.add(AuditFlag.builder()
                    .ticker(ticker)
                    .flagType(FlagType.REVENUE_CASHFLOW_MISMATCH)
                    .severity(Severity.CRITICAL)
                    .description(String.format(
                        "PERSISTENT: Net Profit positive but Cash Flow from Ops negative for %d consecutive quarters — high fraud risk",
                        quarters.size()))
                    .quarter(quarter + "-PERSISTENT")
                    .flaggedAt(Instant.now())
                    .resolved(false)
                    .build());
            }
        }
    }

    /**
     * Rule 5: News sentiment from MoneyControl RSS feed.
     */
    private void checkNewsSentiment(String ticker, List<AuditFlag> flags) {
        try {
            List<AuditFlag> sentimentFlags = newsSentimentService.checkForFraudNews(ticker);
            flags.addAll(sentimentFlags);
        } catch (Exception e) {
            log.warn("News sentiment check failed for {}: {}", ticker, e.getMessage());
        }
    }
}
