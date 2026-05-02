package com.stockpicker.common.entity;

import com.stockpicker.common.enums.FlagType;
import com.stockpicker.common.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "audit_flags", indexes = {
    @Index(name = "idx_audit_ticker", columnList = "ticker"),
    @Index(name = "idx_audit_flag_type", columnList = "flag_type"),
    @Index(name = "idx_audit_severity", columnList = "severity")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 40)
    private FlagType flagType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quarter", length = 10)
    private String quarter;

    @Column(name = "flagged_at", nullable = false)
    private Instant flaggedAt;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", insertable = false, updatable = false)
    private Stock stock;

    @PrePersist
    protected void onCreate() {
        if (this.flaggedAt == null) {
            this.flaggedAt = Instant.now();
        }
    }
}
