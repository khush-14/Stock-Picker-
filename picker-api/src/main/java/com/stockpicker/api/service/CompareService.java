package com.stockpicker.api.service;

import com.stockpicker.common.dto.CompareResponse;
import com.stockpicker.common.entity.DailyMetrics;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.repository.DailyMetricsRepository;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompareService {

    private final StockRepository stockRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final QuarterlyFinancialsRepository quarterlyRepo;

    @Transactional(readOnly = true)
    public CompareResponse compare(String tickerA, String tickerB) {
        return CompareResponse.builder()
            .stockA(buildComparison(tickerA))
            .stockB(buildComparison(tickerB))
            .build();
    }

    private CompareResponse.StockComparison buildComparison(String ticker) {
        Stock stock = stockRepository.findById(ticker)
            .orElseThrow(() -> new NoSuchElementException("Stock not found: " + ticker));

        DailyMetrics latest = dailyMetricsRepository.findLatestByTicker(ticker)
            .orElse(null);

        List<QuarterlyFinancials> last4 = quarterlyRepo.findLast4QuartersByTicker(ticker);

        List<CompareResponse.QuarterData> quarters = last4.stream()
            .map(qf -> CompareResponse.QuarterData.builder()
                .quarter(qf.getQuarter())
                .fiscalYear(qf.getFiscalYear())
                .revenue(qf.getRevenue())
                .netProfit(qf.getNetProfit())
                .operatingProfit(qf.getOperatingProfit())
                .cashFlowFromOperations(qf.getCashFlowFromOperations())
                .promoterHolding(qf.getPromoterHolding())
                .build())
            .toList();

        return CompareResponse.StockComparison.builder()
            .ticker(ticker)
            .companyName(stock.getCompanyName())
            .industry(stock.getIndustry())
            .currentPrice(latest != null ? latest.getPrice() : null)
            .peRatio(latest != null ? latest.getPeRatio() : null)
            .pbRatio(latest != null ? latest.getPbRatio() : null)
            .debtToEquity(latest != null ? latest.getDebtToEquity() : null)
            .quarters(quarters)
            .build();
    }
}
