package com.stockpicker.api.service;

import com.stockpicker.api.specification.StockFilterSpecification;
import com.stockpicker.common.dto.StockFilterRequest;
import com.stockpicker.common.dto.StockFilterResponse;
import com.stockpicker.common.entity.DailyMetrics;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.repository.DailyMetricsRepository;
import com.stockpicker.common.repository.QuarterlyFinancialsRepository;
import com.stockpicker.common.repository.SchemaTableRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFilterService {

    private final StockRepository stockRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final QuarterlyFinancialsRepository quarterlyFinancialsRepository;
    private final SchemaTableRepository schemaTableRepository;

    @Transactional(readOnly = true)
    public StockFilterResponse filterStocks(StockFilterRequest request) {
        log.debug("Filtering stocks with {} criteria, page={}, size={}",
                  request.getCriteria() != null ? request.getCriteria().size() : 0,
                  request.getPage(), request.getSize());

        // Build dynamic specification
        Specification<Stock> spec = StockFilterSpecification.buildFromCriteria(request.getCriteria());

        // Build pageable with optional sorting
        Pageable pageable = buildPageable(request);

        // Execute query
        Page<Stock> stockPage = stockRepository.findAll(spec, pageable);

        // Hydrate each stock with latest metrics
        List<StockFilterResponse.StockRow> rows = stockPage.getContent().stream()
            .map(this::toStockRow)
            .toList();

        return StockFilterResponse.builder()
            .totalElements(stockPage.getTotalElements())
            .totalPages(stockPage.getTotalPages())
            .currentPage(stockPage.getNumber())
            .pageSize(stockPage.getSize())
            .stocks(rows)
            .build();
    }

    private StockFilterResponse.StockRow toStockRow(Stock stock) {
        StockFilterResponse.StockRow.StockRowBuilder builder = StockFilterResponse.StockRow.builder()
            .ticker(stock.getTicker())
            .companyName(stock.getCompanyName())
            .industry(stock.getIndustry())
            .marketCapCategory(stock.getMarketCapCategory() != null
                ? stock.getMarketCapCategory().name() : null)
            .exchange(stock.getExchange());

        // Attach latest daily metrics
        Optional<DailyMetrics> latestDaily = dailyMetricsRepository.findLatestByTicker(stock.getTicker());
        latestDaily.ifPresent(dm -> builder
            .metricsDate(dm.getRecordDate())
            .price(dm.getPrice())
            .peRatio(dm.getPeRatio())
            .pbRatio(dm.getPbRatio())
            .debtToEquity(dm.getDebtToEquity())
            .dividendYield(dm.getDividendYield())
            .eps(dm.getEps())
            .marketCap(dm.getMarketCap())
            .volume(dm.getVolume()));

        // Attach latest quarterly financials
        Optional<QuarterlyFinancials> latestQf =
            quarterlyFinancialsRepository.findLatestByTicker(stock.getTicker());
        latestQf.ifPresent(qf -> builder
            .latestQuarter(qf.getQuarter())
            .fiscalYear(qf.getFiscalYear())
            .revenue(qf.getRevenue())
            .netProfit(qf.getNetProfit())
            .operatingProfit(qf.getOperatingProfit())
            .promoterHolding(qf.getPromoterHolding())
            .promoterPledging(qf.getPromoterPledging()));

        return builder.build();
    }

    private Pageable buildPageable(StockFilterRequest request) {
        Sort sort = Sort.unsorted();
        if (request.getSortBy() != null && !request.getSortBy().isBlank()) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, request.getSortBy());
        }
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
