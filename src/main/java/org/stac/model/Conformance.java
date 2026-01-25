package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * OGC API Conformance Response DTO.
 * Represents the conformance declaration response at /conformance endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Conformance(
        /**
         * List of conformance class URIs that this API implements.
         * Examples:
         * - https://api.stacspec.org/v1.0.0/core
         * - https://api.stacspec.org/v1.0.0/item-search
         * - https://api.stacspec.org/v1.0.0/item-search#filter
         * - http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core
         * - http://www.opengis.net/spec/cql2/1.0/conf/cql2-text
         */
        List<String> conformsTo
) {
    /**
     * Builder pattern support for compatibility with existing code.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> conformsTo;

        public Builder conformsTo(List<String> conformsTo) {
            this.conformsTo = conformsTo;
            return this;
        }

        public Conformance build() {
            return new Conformance(conformsTo);
        }
    }

    // Accessor method for compatibility with code expecting getter
    public List<String> getConformsTo() {
        return conformsTo;
    }
}
