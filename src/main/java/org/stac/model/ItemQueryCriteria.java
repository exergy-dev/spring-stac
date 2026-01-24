package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query criteria for listing items within a collection.
 * <p>
 * This is a simplified search criteria specifically for the
 * /collections/{collectionId}/items endpoint, which supports
 * a subset of the full search parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemQueryCriteria {

    /**
     * Bounding box for spatial filtering.
     * Items intersecting this bbox will be included.
     */
    private BoundingBox bbox;

    /**
     * Datetime filter string.
     * <p>
     * Supports the following formats:
     * - Single datetime: "2020-01-01T00:00:00Z"
     * - Interval: "2020-01-01T00:00:00Z/2020-12-31T23:59:59Z"
     * - Open start: "../2020-12-31T23:59:59Z"
     * - Open end: "2020-01-01T00:00:00Z/.."
     */
    private String datetime;

    /**
     * Maximum number of items to return.
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
     * Creates a builder with default values.
     *
     * @return a new ItemQueryCriteria builder
     */
    public static ItemQueryCriteriaBuilder defaults() {
        return ItemQueryCriteria.builder()
                .limit(10);
    }

    /**
     * Checks if this query has any constraints.
     *
     * @return true if any filtering is specified
     */
    public boolean hasConstraints() {
        return bbox != null ||
               (datetime != null && !datetime.isBlank()) ||
               filter != null;
    }

    /**
     * Checks if pagination is requested.
     *
     * @return true if a pagination token is present
     */
    public boolean isPaginated() {
        return token != null && !token.isBlank();
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
     * Converts this ItemQueryCriteria to a full SearchCriteria.
     * <p>
     * Useful when the same search logic needs to be applied to both
     * item listing and search operations.
     *
     * @param collectionId the collection ID to include
     * @return SearchCriteria equivalent of this query
     */
    public SearchCriteria toSearchCriteria(String collectionId) {
        SearchCriteria.SearchCriteriaBuilder builder = SearchCriteria.builder()
                .bbox(bbox)
                .limit(limit)
                .token(token)
                .filter(filter);

        if (collectionId != null) {
            builder.collections(java.util.List.of(collectionId));
        }

        // Parse datetime string to TemporalExtent if present
        if (datetime != null && !datetime.isBlank()) {
            builder.datetime(parseDatetime(datetime));
        }

        return builder.build();
    }

    /**
     * Parses a datetime string into a TemporalExtent.
     *
     * @param datetime the datetime string to parse
     * @return TemporalExtent representation
     */
    private TemporalExtent parseDatetime(String datetime) {
        String[] parts = datetime.split("/");
        java.util.List<String> interval;

        if (parts.length == 1) {
            // Single instant - treat as point in time
            interval = java.util.Arrays.asList(parts[0], parts[0]);
        } else if (parts.length == 2) {
            // Interval
            String start = "..".equals(parts[0]) ? null : parts[0];
            String end = "..".equals(parts[1]) ? null : parts[1];
            interval = java.util.Arrays.asList(start, end);
        } else {
            throw new IllegalArgumentException("Invalid datetime format: " + datetime);
        }

        return TemporalExtent.builder()
                .interval(java.util.List.of(interval))
                .build();
    }
}
