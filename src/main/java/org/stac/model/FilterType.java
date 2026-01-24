package org.stac.model;

/**
 * Enumeration of CQL2 filter types.
 * <p>
 * These types represent the different categories of filter operations
 * that can be used in CQL2 expressions.
 */
public enum FilterType {
    /**
     * Logical AND operation combining multiple filters.
     */
    AND,

    /**
     * Logical OR operation combining multiple filters.
     */
    OR,

    /**
     * Logical NOT operation negating a filter.
     */
    NOT,

    /**
     * Comparison operations (=, <>, <, <=, >, >=, LIKE, BETWEEN, IN, IS NULL).
     */
    COMPARISON,

    /**
     * Spatial operations (INTERSECTS, WITHIN, CONTAINS, DISJOINT, etc.).
     */
    SPATIAL,

    /**
     * Temporal operations (T_INTERSECTS, T_BEFORE, T_AFTER, T_DURING, etc.).
     */
    TEMPORAL
}
