package com.stockpicker.common.repository;

import com.stockpicker.common.entity.DailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, Long> {

    @Query("SELECT dm FROM DailyMetrics dm WHERE dm.ticker = :ticker AND dm.recordDate = " +
           "(SELECT MAX(dm2.recordDate) FROM DailyMetrics dm2 WHERE dm2.ticker = :ticker)")
    Optional<DailyMetrics> findLatestByTicker(@Param("ticker") String ticker);

    @Query("SELECT dm FROM DailyMetrics dm WHERE dm.ticker = :ticker ORDER BY dm.recordDate DESC")
    List<DailyMetrics> findByTickerOrderByRecordDateDesc(@Param("ticker") String ticker);

    boolean existsByTickerAndRecordDate(String ticker, LocalDate recordDate);

    @Query("SELECT dm FROM DailyMetrics dm WHERE dm.ticker = :ticker " +
           "AND dm.recordDate >= :startDate ORDER BY dm.recordDate ASC")
    List<DailyMetrics> findByTickerAndDateRange(@Param("ticker") String ticker,
                                                 @Param("startDate") LocalDate startDate);

    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY pe_ratio) " +
                   "FROM daily_metrics WHERE ticker = :ticker AND record_date >= :startDate",
           nativeQuery = true)
    Optional<Double> findMedianPeRatio(@Param("ticker") String ticker,
                                        @Param("startDate") LocalDate startDate);
}
