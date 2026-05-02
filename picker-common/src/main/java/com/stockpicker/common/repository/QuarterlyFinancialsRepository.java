package com.stockpicker.common.repository;

import com.stockpicker.common.entity.QuarterlyFinancials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuarterlyFinancialsRepository extends JpaRepository<QuarterlyFinancials, Long> {

    boolean existsByTickerAndQuarterAndFiscalYear(String ticker, String quarter, Integer fiscalYear);

    @Query("SELECT qf FROM QuarterlyFinancials qf WHERE qf.ticker = :ticker " +
           "ORDER BY qf.fiscalYear DESC, qf.quarter DESC")
    List<QuarterlyFinancials> findByTickerOrderByLatest(@Param("ticker") String ticker);

    @Query("SELECT qf FROM QuarterlyFinancials qf WHERE qf.ticker = :ticker " +
           "ORDER BY qf.fiscalYear DESC, qf.quarter DESC LIMIT 1")
    Optional<QuarterlyFinancials> findLatestByTicker(@Param("ticker") String ticker);

    @Query("SELECT qf FROM QuarterlyFinancials qf WHERE qf.ticker = :ticker " +
           "ORDER BY qf.fiscalYear DESC, qf.quarter DESC LIMIT 4")
    List<QuarterlyFinancials> findLast4QuartersByTicker(@Param("ticker") String ticker);

    @Query("SELECT qf FROM QuarterlyFinancials qf WHERE qf.ticker = :ticker " +
           "ORDER BY qf.fiscalYear DESC, qf.quarter DESC LIMIT 2")
    List<QuarterlyFinancials> findLast2QuartersByTicker(@Param("ticker") String ticker);
}
