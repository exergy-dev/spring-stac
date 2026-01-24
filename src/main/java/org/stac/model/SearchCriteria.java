package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;

/**
 * Internal search criteria model built from SearchRequest.
 * <p>
 * This class represents the parsed and validated search parameters
 * used internally by the STAC API server to execute searches.
 * It is typically built from incoming SearchRequest DTOs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    /**
     * List of collection IDs to search within.
     * If null or empty, searches across all collections.
     */
    private List<String> collections;

    /**
     * List of specific item IDs to retrieve.
     * If specified, returns only items with these IDs.
     */
    private List<String> ids;

    /**
     * Bounding box for spatial filtering.
     * Items intersecting this bbox will be included.
     */
    private BoundingBox bbox;

    /**
     * Temporal extent for datetime filtering.
     * Items within this time range will be included.
     */
    private TemporalExtent datetime;

    /**
     * Geometry for spatial intersection filtering.
     * Items intersecting this geometry will be included.
     * Takes precedence over bbox if both are specified.
     */
    private Geometry intersects;

    /**
     * Maximum number of items to return.
     * Default is typically 10; maximum is backend-dependent.
     */
    private Integer limit;

    /**
     * Pagination cursor token.
     * Used to retrieve the next page of results.
     */
    private String token;

    /**
     * Parsed CQL2 filter criteria.
     * Provides advanced filtering capabilities.
     */
    private FilterCriteria filter;

    /**
     * Sort specifications for ordering results.
     * Applied in order; first specification is primary sort.
     */
    private List<SortBy> sortBy;

    /**
     * Field selection for response shaping.
     * Specifies which fields to include or exclude.
     */
    private FieldSelector fields;

    /**
     * Query extension operations.
     * Maps property names to query operations for filtering.
     */
    private Map<String, QueryOperation> query;

    /**
     * Free-text search query string.
     * Used for full-text search across item metadata.
     */
    private String q;

    /**
     * Creates a builder with default values.
     *
     * @return a new SearchCriteria builder
     */
    public static SearchCriteriaBuilder defaults() {
        return SearchCriteria.builder()
                .limit(10);
    }

    /**
     * Checks if this search criteria has any spatial constraints.
     *
     * @return true if bbox or intersects is specified
     */
    public boolean hasSpatialConstraint() {
        return bbox != null || intersects != null;
    }

    /**
     * Checks if this search criteria has any temporal constraints.
     *
     * @return true if datetime is specified
     */
    public boolean hasTemporalConstraint() {
        return datetime != null && datetime.getInterval() != null && !datetime.getInterval().isEmpty();
    }

    /**
     * Checks if this search criteria has any filter constraints.
     *
     * @return true if filter or query is specified
     */
    public boolean hasFilterConstraint() {
        return filter != null || (query != null && !query.isEmpty());
    }

    /**
     * Checks if this search criteria specifies collection filtering.
     *
     * @return true if collections list is non-empty
     */
    public boolean hasCollectionConstraint() {
        return collections != null && !collections.isEmpty();
    }

    /**
     * Checks if this search criteria specifies ID filtering.
     *
     * @return true if ids list is non-empty
     */
    public boolean hasIdConstraint() {
        return ids != null && !ids.isEmpty();
    }

    /**
     * Gets the effective limit, falling back to default if not specified.
     *
     * @param defaultLimit the default limit to use
     * @return the limit or default if not set
     */
    public int getEffectiveLimit(int defaultLimit) {
        return limit != null ? limit : defaultLimit;
    }

    /**
     * Checks if pagination is requested.
     *
     * @return true if a pagination token is present
     */
    public boolean isPaginated() {
        return token != null && !token.isBlank();
    }
}
