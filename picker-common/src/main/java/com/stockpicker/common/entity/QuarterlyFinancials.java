package com.stockpicker.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "quarterly_financials",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_quarterly_ticker_quarter_year",
        columnNames = {"ticker", "quarter", "fiscal_year"}
    ),
    indexes = {
        @Index(name = "idx_quarterly_ticker", columnList = "ticker"),
        @Index(name = "idx_quarterly_report_date", columnList = "report_date")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuarterlyFinancials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "quarter", nullable = false, length = 10)
    private String quarter;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "revenue", precision = 18, scale = 2)
    private BigDecimal revenue;

    @Column(name = "net_profit", precision = 18, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "operating_profit", precision = 18, scale = 2)
    private BigDecimal operatingProfit;

    @Column(name = "cash_flow_from_operations", precision = 18, scale = 2)
    private BigDecimal cashFlowFromOperations;

    @Column(name = "promoter_holding", precision = 6, scale = 2)
    private BigDecimal promoterHolding;

    @Column(name = "promoter_pledging", precision = 6, scale = 2)
    private BigDecimal promoterPledging;

    @Column(name = "auditor_name", length = 200)
    private String auditorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", insertable = false, updatable = false)
    private Stock stock;
}
