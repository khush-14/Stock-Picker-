package com.stockpicker.common.repository;

import com.stockpicker.common.entity.AuditFlag;
import com.stockpicker.common.enums.FlagType;
import com.stockpicker.common.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditFlagRepository extends JpaRepository<AuditFlag, Long> {

    List<AuditFlag> findByTickerOrderByFlaggedAtDesc(String ticker);

    List<AuditFlag> findByTickerAndResolvedFalseOrderByFlaggedAtDesc(String ticker);

    List<AuditFlag> findBySeverityOrderByFlaggedAtDesc(Severity severity);

    boolean existsByTickerAndFlagTypeAndQuarter(String ticker, FlagType flagType, String quarter);
}
