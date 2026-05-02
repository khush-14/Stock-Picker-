package com.stockpicker.common.enums;

public enum FilterOperator {
    EQ,      // equals
    NEQ,     // not equals
    GT,      // greater than
    GTE,     // greater than or equal
    LT,      // less than
    LTE,     // less than or equal
    BETWEEN, // range (uses value and valueTo)
    IN       // in a set of values
}
