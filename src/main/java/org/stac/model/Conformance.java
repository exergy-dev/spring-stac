package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OGC API Conformance Response DTO.
 * Represents the conformance declaration response at /conformance endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Conformance {

    /**
     * List of conformance class URIs that this API implements.
     * Examples:
     * - https://api.stacspec.org/v1.0.0/core
     * - https://api.stacspec.org/v1.0.0/item-search
     * - https://api.stacspec.org/v1.0.0/item-search#filter
     * - http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core
     * - http://www.opengis.net/spec/cql2/1.0/conf/cql2-text
     */
    private List<String> conformsTo;
}
