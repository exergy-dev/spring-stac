package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for aggregation queries.
 * <p>
 * Supports aggregating STAC item properties across collections,
 * such as counting items by date range, summarizing cloud cover statistics,
 * or computing value distributions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationRequest {

    /**
     * List of collection IDs to aggregate across.
     * If null or empty, aggregates across all collections.
     */
    private List<String> collections;

    /**
     * List of field names to aggregate.
     * <p>
     * Supports dotted notation for nested properties (e.g., "properties.eo:cloud_cover").
     * The type of aggregation depends on the field type:
     * - Numeric: min, max, avg, sum, percentiles
     * - String/Keyword: terms (value counts)
     * - Datetime: date histogram
     * - Geometry: geohash grid, bounds
     */
    private List<String> aggregations;

    /**
     * Bounding box for spatial filtering.
     * Format: [west, south, east, north] or [west, south, min_elev, east, north, max_elev]
     */
    private double[] bbox;

    /**
     * Datetime filter using ISO 8601 datetime or interval.
     * Single datetime: "2020-01-01T00:00:00Z"
     * Interval: "2020-01-01T00:00:00Z/2020-12-31T23:59:59Z"
     * Open interval: "../2020-12-31T23:59:59Z" or "2020-01-01T00:00:00Z/.."
     */
    private String datetime;

    /**
     * Optional filter to apply before aggregation.
     * Only items matching this filter will be included in aggregations.
     */
    private FilterCriteria filter;

    /**
     * Creates a request to aggregate a single field.
     *
     * @param field the field to aggregate
     * @return AggregationRequest for the specified field
     */
    public static AggregationRequest forField(String field) {
        return AggregationRequest.builder()
                .aggregations(List.of(field))
                .build();
    }

    /**
     * Creates a request to aggregate multiple fields.
     *
     * @param fields the fields to aggregate
     * @return AggregationRequest for the specified fields
     */
    public static AggregationRequest forFields(String... fields) {
        return AggregationRequest.builder()
                .aggregations(List.of(fields))
                .build();
    }

    /**
     * Creates a request to aggregate within specific collections.
     *
     * @param collectionIds the collections to aggregate
     * @param fields        the fields to aggregate
     * @return AggregationRequest with collection scope
     */
    public static AggregationRequest forCollections(List<String> collectionIds, String... fields) {
        return AggregationRequest.builder()
                .collections(collectionIds)
                .aggregations(List.of(fields))
                .build();
    }

    /**
     * Checks if this request specifies collection filtering.
     *
     * @return true if collections list is non-empty
     */
    public boolean hasCollectionScope() {
        return collections != null && !collections.isEmpty();
    }

    /**
     * Checks if this request has a filter.
     *
     * @return true if filter is specified
     */
    public boolean hasFilter() {
        return filter != null;
    }

    /**
     * Gets the number of aggregations requested.
     *
     * @return count of aggregation fields
     */
    public int getAggregationCount() {
        return aggregations != null ? aggregations.size() : 0;
    }
}
