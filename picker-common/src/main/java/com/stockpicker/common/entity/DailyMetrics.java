package com.stockpicker.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * High-frequency time-series entity for daily stock metrics.
 * Backed by a TimescaleDB hypertable partitioned by record_date (7-day interval).
 * No DB-level PRIMARY KEY constraint (TimescaleDB requires PK to include partition column).
 * JPA uses the 'id' column as its logical identifier.
 */
@Entity
@Table(name = "daily_metrics", indexes = {
    @Index(name = "idx_daily_ticker", columnList = "ticker"),
    @Index(name = "idx_daily_date", columnList = "record_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DailyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "price", precision = 12, scale = 4)
    private BigDecimal price;

    @Column(name = "pe_ratio", precision = 10, scale = 4)
    private BigDecimal peRatio;

    @Column(name = "pb_ratio", precision = 10, scale = 4)
    private BigDecimal pbRatio;

    @Column(name = "debt_to_equity", precision = 10, scale = 4)
    private BigDecimal debtToEquity;

    @Column(name = "dividend_yield", precision = 8, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "eps", precision = 10, scale = 4)
    private BigDecimal eps;

    @Column(name = "market_cap", precision = 18, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "volume")
    private Long volume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", insertable = false, updatable = false)
    private Stock stock;
}
