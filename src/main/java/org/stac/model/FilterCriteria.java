package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Parsed CQL2 filter representation.
 * <p>
 * This class represents a parsed CQL2 filter expression as a tree structure.
 * Logical operators (AND, OR, NOT) have children, while comparison, spatial,
 * and temporal operators have property names and values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {

    /**
     * The type of filter operation (AND, OR, NOT, COMPARISON, SPATIAL, TEMPORAL).
     */
    private FilterType type;

    /**
     * The specific operator for this filter.
     * <p>
     * For COMPARISON: "=", "<>", "<", "<=", ">", ">=", "LIKE", "BETWEEN", "IN", "IS NULL"
     * For SPATIAL: "INTERSECTS", "WITHIN", "CONTAINS", "DISJOINT", "TOUCHES", "CROSSES", "OVERLAPS"
     * For TEMPORAL: "T_INTERSECTS", "T_BEFORE", "T_AFTER", "T_DURING", "T_EQUALS"
     * For logical types: "AND", "OR", "NOT"
     */
    private String operator;

    /**
     * Child filter criteria for logical operators (AND, OR, NOT).
     * <p>
     * For AND/OR: contains two or more child filters
     * For NOT: contains exactly one child filter
     * For other types: should be null or empty
     */
    private List<FilterCriteria> children;

    /**
     * The property name being filtered on.
     * <p>
     * Supports dotted notation for nested properties (e.g., "properties.datetime").
     */
    private String propertyName;

    /**
     * The value to compare against.
     * <p>
     * Type depends on the operator:
     * - String for text comparisons
     * - Number for numeric comparisons
     * - List for IN operator
     * - Geometry for spatial operators
     * - Temporal interval for temporal operators
     */
    private Object value;

    /**
     * Creates an AND filter combining multiple criteria.
     *
     * @param criteria the criteria to combine
     * @return a new FilterCriteria with AND type
     */
    public static FilterCriteria and(List<FilterCriteria> criteria) {
        return FilterCriteria.builder()
                .type(FilterType.AND)
                .operator("AND")
                .children(criteria)
                .build();
    }

    /**
     * Creates an OR filter combining multiple criteria.
     *
     * @param criteria the criteria to combine
     * @return a new FilterCriteria with OR type
     */
    public static FilterCriteria or(List<FilterCriteria> criteria) {
        return FilterCriteria.builder()
                .type(FilterType.OR)
                .operator("OR")
                .children(criteria)
                .build();
    }

    /**
     * Creates a NOT filter negating a criterion.
     *
     * @param criterion the criterion to negate
     * @return a new FilterCriteria with NOT type
     */
    public static FilterCriteria not(FilterCriteria criterion) {
        return FilterCriteria.builder()
                .type(FilterType.NOT)
                .operator("NOT")
                .children(List.of(criterion))
                .build();
    }

    /**
     * Creates an equality comparison filter.
     *
     * @param propertyName the property to compare
     * @param value        the value to compare against
     * @return a new FilterCriteria with equality comparison
     */
    public static FilterCriteria equals(String propertyName, Object value) {
        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator("=")
                .propertyName(propertyName)
                .value(value)
                .build();
    }

    /**
     * Creates a spatial intersection filter.
     *
     * @param propertyName the geometry property (usually "geometry")
     * @param geometry     the geometry to test intersection with
     * @return a new FilterCriteria with INTERSECTS operator
     */
    public static FilterCriteria intersects(String propertyName, Object geometry) {
        return FilterCriteria.builder()
                .type(FilterType.SPATIAL)
                .operator("INTERSECTS")
                .propertyName(propertyName)
                .value(geometry)
                .build();
    }

    /**
     * Checks if this is a logical operator (AND, OR, NOT).
     *
     * @return true if this is a logical operator
     */
    public boolean isLogical() {
        return type == FilterType.AND || type == FilterType.OR || type == FilterType.NOT;
    }

    /**
     * Checks if this is a leaf node (no children).
     *
     * @return true if this has no children
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }
}
