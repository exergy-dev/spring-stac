package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Query extension operation for STAC API.
 * <p>
 * Represents a query operation from the STAC Query Extension,
 * which allows filtering items by property values using various operators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryOperation {

    /**
     * The operator for this query operation.
     * <p>
     * Supported operators:
     * - "eq": Equal to
     * - "neq": Not equal to
     * - "lt": Less than
     * - "lte": Less than or equal to
     * - "gt": Greater than
     * - "gte": Greater than or equal to
     * - "startsWith": String starts with
     * - "endsWith": String ends with
     * - "contains": String contains
     * - "in": Value in list
     */
    private String operator;

    /**
     * The value to compare against.
     * <p>
     * Type depends on the operator and property:
     * - String for text properties
     * - Number for numeric properties
     * - List for "in" operator
     */
    private Object value;

    /**
     * Creates an equality operation.
     *
     * @param value the value to match
     * @return QueryOperation with eq operator
     */
    public static QueryOperation eq(Object value) {
        return QueryOperation.builder()
                .operator("eq")
                .value(value)
                .build();
    }

    /**
     * Creates a not-equal operation.
     *
     * @param value the value to not match
     * @return QueryOperation with neq operator
     */
    public static QueryOperation neq(Object value) {
        return QueryOperation.builder()
                .operator("neq")
                .value(value)
                .build();
    }

    /**
     * Creates a less-than operation.
     *
     * @param value the upper bound (exclusive)
     * @return QueryOperation with lt operator
     */
    public static QueryOperation lt(Object value) {
        return QueryOperation.builder()
                .operator("lt")
                .value(value)
                .build();
    }

    /**
     * Creates a less-than-or-equal operation.
     *
     * @param value the upper bound (inclusive)
     * @return QueryOperation with lte operator
     */
    public static QueryOperation lte(Object value) {
        return QueryOperation.builder()
                .operator("lte")
                .value(value)
                .build();
    }

    /**
     * Creates a greater-than operation.
     *
     * @param value the lower bound (exclusive)
     * @return QueryOperation with gt operator
     */
    public static QueryOperation gt(Object value) {
        return QueryOperation.builder()
                .operator("gt")
                .value(value)
                .build();
    }

    /**
     * Creates a greater-than-or-equal operation.
     *
     * @param value the lower bound (inclusive)
     * @return QueryOperation with gte operator
     */
    public static QueryOperation gte(Object value) {
        return QueryOperation.builder()
                .operator("gte")
                .value(value)
                .build();
    }

    /**
     * Creates a starts-with string operation.
     *
     * @param prefix the prefix to match
     * @return QueryOperation with startsWith operator
     */
    public static QueryOperation startsWith(String prefix) {
        return QueryOperation.builder()
                .operator("startsWith")
                .value(prefix)
                .build();
    }

    /**
     * Creates an ends-with string operation.
     *
     * @param suffix the suffix to match
     * @return QueryOperation with endsWith operator
     */
    public static QueryOperation endsWith(String suffix) {
        return QueryOperation.builder()
                .operator("endsWith")
                .value(suffix)
                .build();
    }

    /**
     * Creates a contains string operation.
     *
     * @param substring the substring to find
     * @return QueryOperation with contains operator
     */
    public static QueryOperation contains(String substring) {
        return QueryOperation.builder()
                .operator("contains")
                .value(substring)
                .build();
    }

    /**
     * Creates an in-list operation.
     *
     * @param values the list of values to match
     * @return QueryOperation with in operator
     */
    public static QueryOperation in(List<?> values) {
        return QueryOperation.builder()
                .operator("in")
                .value(values)
                .build();
    }

    /**
     * Checks if this is a comparison operator (lt, lte, gt, gte).
     *
     * @return true if this is a range comparison
     */
    public boolean isRangeComparison() {
        return "lt".equals(operator) || "lte".equals(operator) ||
               "gt".equals(operator) || "gte".equals(operator);
    }

    /**
     * Checks if this is a string operation (startsWith, endsWith, contains).
     *
     * @return true if this is a string operation
     */
    public boolean isStringOperation() {
        return "startsWith".equals(operator) || "endsWith".equals(operator) ||
               "contains".equals(operator);
    }
}
