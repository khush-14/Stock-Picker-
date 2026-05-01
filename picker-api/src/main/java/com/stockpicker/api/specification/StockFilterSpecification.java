package com.stockpicker.api.specification;

import com.stockpicker.common.dto.FilterCriteria;
import com.stockpicker.common.entity.DailyMetrics;
import com.stockpicker.common.entity.QuarterlyFinancials;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.enums.FilterOperator;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

/**
 * Builds dynamic JPA Specifications to filter stocks based on user-defined criteria.
 * Joins Stock → latest DailyMetrics and latest QuarterlyFinancials via correlated subqueries.
 */
public class StockFilterSpecification {

    private StockFilterSpecification() {}

    /**
     * Build a composite Specification from a list of filter criteria.
     * All criteria are combined with AND logic.
     */
    public static Specification<Stock> buildFromCriteria(List<FilterCriteria> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return Specification.where(null);
        }

        Specification<Stock> spec = Specification.where(buildSingle(criteria.get(0)));
        for (int i = 1; i < criteria.size(); i++) {
            spec = spec.and(buildSingle(criteria.get(i)));
        }
        return spec;
    }

    private static Specification<Stock> buildSingle(FilterCriteria criteria) {
        String field = criteria.getField();

        // Route to the correct entity based on field name
        return switch (field) {
            // Stock-level fields
            case "industry", "market_cap_category", "exchange" ->
                buildStockFieldSpec(field, criteria);

            // DailyMetrics fields — need subquery join
            case "price", "pe_ratio", "pb_ratio", "debt_to_equity",
                 "dividend_yield", "eps", "market_cap", "volume" ->
                buildDailyMetricsSpec(field, criteria);

            // QuarterlyFinancials fields — need subquery join
            case "revenue", "net_profit", "operating_profit",
                 "cash_flow_from_operations", "promoter_holding", "promoter_pledging" ->
                buildQuarterlyFinancialsSpec(field, criteria);

            default -> throw new IllegalArgumentException("Unknown filter field: " + field);
        };
    }

    private static Specification<Stock> buildStockFieldSpec(String field, FilterCriteria criteria) {
        return (root, query, cb) -> {
            String javaField = snakeToCamel(field);
            return buildPredicate(cb, root.get(javaField), criteria);
        };
    }

    /**
     * Filters by a DailyMetrics field from the latest record per ticker.
     * Uses: EXISTS (SELECT 1 FROM DailyMetrics dm WHERE dm.ticker = s.ticker
     *              AND dm.recordDate = (SELECT MAX(...)) AND dm.field <op> value)
     */
    private static Specification<Stock> buildDailyMetricsSpec(String field, FilterCriteria criteria) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<DailyMetrics> dmRoot = subquery.from(DailyMetrics.class);
            subquery.select(cb.literal(1L));

            // Get the max date per ticker for "latest record"
            Subquery<java.time.LocalDate> maxDateSubquery = query.subquery(java.time.LocalDate.class);
            Root<DailyMetrics> dmMaxRoot = maxDateSubquery.from(DailyMetrics.class);
            maxDateSubquery.select(cb.greatest(dmMaxRoot.<java.time.LocalDate>get("recordDate")));
            maxDateSubquery.where(cb.equal(dmMaxRoot.get("ticker"), root.get("ticker")));

            String javaField = snakeToCamel(field);
            Predicate valuePredicate = buildPredicate(cb, dmRoot.get(javaField), criteria);

            subquery.where(
                cb.equal(dmRoot.get("ticker"), root.get("ticker")),
                cb.equal(dmRoot.get("recordDate"), maxDateSubquery),
                valuePredicate
            );

            return cb.exists(subquery);
        };
    }

    /**
     * Filters by a QuarterlyFinancials field from the latest record per ticker.
     */
    private static Specification<Stock> buildQuarterlyFinancialsSpec(String field, FilterCriteria criteria) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<QuarterlyFinancials> qfRoot = subquery.from(QuarterlyFinancials.class);
            subquery.select(cb.literal(1L));

            // Latest quarterly = max id for ticker (id is monotonically increasing)
            Subquery<Long> maxIdSubquery = query.subquery(Long.class);
            Root<QuarterlyFinancials> qfMaxRoot = maxIdSubquery.from(QuarterlyFinancials.class);
            maxIdSubquery.select(cb.max(qfMaxRoot.get("id")));
            maxIdSubquery.where(cb.equal(qfMaxRoot.get("ticker"), root.get("ticker")));

            String javaField = snakeToCamel(field);
            Predicate valuePredicate = buildPredicate(cb, qfRoot.get(javaField), criteria);

            subquery.where(
                cb.equal(qfRoot.get("ticker"), root.get("ticker")),
                cb.equal(qfRoot.get("id"), maxIdSubquery),
                valuePredicate
            );

            return cb.exists(subquery);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildPredicate(CriteriaBuilder cb, Path<?> path, FilterCriteria criteria) {
        FilterOperator op = criteria.getOperator();
        Object rawValue = criteria.getValue();

        if (op == FilterOperator.IN) {
            // Value should be a collection
            if (rawValue instanceof List<?> list) {
                return path.in(list);
            }
            return path.in(rawValue);
        }

        Comparable value = toComparable(rawValue);

        return switch (op) {
            case EQ -> cb.equal(path, value);
            case NEQ -> cb.notEqual(path, value);
            case GT -> cb.greaterThan((Path<Comparable>) path, value);
            case GTE -> cb.greaterThanOrEqualTo((Path<Comparable>) path, value);
            case LT -> cb.lessThan((Path<Comparable>) path, value);
            case LTE -> cb.lessThanOrEqualTo((Path<Comparable>) path, value);
            case BETWEEN -> {
                Comparable valueTo = toComparable(criteria.getValueTo());
                yield cb.between((Path<Comparable>) path, value, valueTo);
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private static Comparable<?> toComparable(Object value) {
        if (value instanceof Number num) {
            return new BigDecimal(num.toString());
        }
        if (value instanceof String str) {
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                return str;
            }
        }
        if (value instanceof Comparable<?> comp) {
            return comp;
        }
        return value.toString();
    }

    private static String snakeToCamel(String snake) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return result.toString();
    }
}
