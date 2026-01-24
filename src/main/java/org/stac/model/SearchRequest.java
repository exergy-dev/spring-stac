package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * STAC Search Request DTO.
 * Represents a search request to the STAC API /search endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRequest {

    /**
     * Array of collection IDs to search within.
     */
    private List<String> collections;

    /**
     * Array of Item IDs to return.
     */
    private List<String> ids;

    /**
     * Maximum number of results to return.
     */
    @Builder.Default
    private Integer limit = 10;

    /**
     * Bounding box for spatial filtering.
     * Format: "west,south,east,north" or "west,south,min_elev,east,north,max_elev"
     */
    private String bbox;

    /**
     * Datetime filter using ISO 8601 datetime or interval.
     * Single datetime: "2020-01-01T00:00:00Z"
     * Interval: "2020-01-01T00:00:00Z/2020-12-31T23:59:59Z"
     * Open interval: "../2020-12-31T23:59:59Z" or "2020-01-01T00:00:00Z/.."
     */
    private String datetime;

    /**
     * GeoJSON geometry string for spatial intersection filtering.
     */
    private String intersects;

    /**
     * CQL2 filter expression as a JSON object.
     */
    private JsonNode filter;

    /**
     * Filter language: "cql2-text" or "cql2-json".
     */
    @JsonProperty("filter-lang")
    private String filterLang;

    /**
     * Property-based queries (legacy STAC query extension).
     */
    private Map<String, Object> query;

    /**
     * Sort order specifications.
     * Format: "+property" for ascending, "-property" for descending.
     */
    private List<String> sortby;

    /**
     * Field selector expression for including/excluding properties.
     */
    private String fields;

    /**
     * Pagination cursor token for retrieving the next page of results.
     */
    private String token;

    /**
     * Free-text search parameter for full-text search across item metadata.
     * Searches properties like title, description, and other text fields.
     * Supported by pgSTAC backend for full-text search functionality.
     */
    private String q;
}
