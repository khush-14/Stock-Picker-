package com.stockpicker.common.entity;

import com.stockpicker.common.enums.MarketCapCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "stocks", indexes = {
    @Index(name = "idx_stocks_industry", columnList = "industry"),
    @Index(name = "idx_stocks_market_cap", columnList = "market_cap_category")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Stock {

    @Id
    @Column(name = "ticker", length = 20)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "industry", length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_cap_category", length = 20)
    private MarketCapCategory marketCapCategory;

    @Column(name = "exchange", length = 10)
    private String exchange;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
