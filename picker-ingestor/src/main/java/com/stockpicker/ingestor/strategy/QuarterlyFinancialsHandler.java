package com.stockpicker.ingestor.strategy;

import com.stockpicker.common.dto.StockTaskMessage;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import com.stockpicker.ingestor.client.ScreenerInScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scrapes quarterly financials from Screener.in and saves them.
 * Uses idempotency check on (ticker, quarter, fiscal_year).
 */
@Slf4j
@Component("quarterlyFinancialsHandler")
@RequiredArgsConstructor
public class QuarterlyFinancialsHandler implements TaskHandler {

    private final ScreenerInScraper screenerScraper;
    private final QuarterlyFinancialsRepository quarterlyRepo;

    @Override
    public void handle(StockTaskMessage message) {
        String ticker = message.getTicker();

        // Determine current quarter
        LocalDate now = LocalDate.now();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        String quarter = "Q" + currentQuarter;
        int fiscalYear = now.getYear();

        // Idempotency: skip duplicate quarterly records
        if (quarterlyRepo.existsByTickerAndQuarterAndFiscalYear(ticker, quarter, fiscalYear)) {
            log.info("Quarterly financials already exist for {} {} FY{}. Skipping.",
                     ticker, quarter, fiscalYear);
            return;
        }

        log.info("Scraping quarterly financials for {} {} FY{}", ticker, quarter, fiscalYear);
        ScreenerInScraper.FinancialsData data = screenerScraper.scrapeFinancials(ticker);

        QuarterlyFinancials financials = QuarterlyFinancials.builder()
            .ticker(ticker)
            .quarter(quarter)
            .fiscalYear(fiscalYear)
            .reportDate(now)
            .revenue(data.getRevenue())
            .netProfit(data.getNetProfit())
            .operatingProfit(data.getOperatingProfit())
            .cashFlowFromOperations(data.getCashFlowFromOperations())
            .promoterHolding(data.getPromoterHolding())
            .promoterPledging(data.getPromoterPledging())
            .auditorName(data.getAuditorName())
            .build();

        quarterlyRepo.save(financials);
        log.info("Saved quarterly financials for {} {} FY{} — Revenue=₹{}Cr",
                 ticker, quarter, fiscalYear, data.getRevenue());
    }
}
