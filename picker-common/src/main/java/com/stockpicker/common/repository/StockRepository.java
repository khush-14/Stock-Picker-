package com.stockpicker.common.repository;

import com.stockpicker.common.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, String>, JpaSpecificationExecutor<Stock> {

    List<Stock> findByIndustry(String industry);

    List<Stock> findByMarketCapCategory(com.stockpicker.common.enums.MarketCapCategory category);

    List<Stock> findByTickerIn(List<String> tickers);
}
