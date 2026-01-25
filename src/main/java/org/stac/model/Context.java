package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * STAC Context Extension Model.
 * Provides metadata about search results including pagination information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Context(
        /**
         * Number of items returned in this response.
         */
        Integer returned,

        /**
         * Maximum number of items requested (the limit parameter).
         */
        Integer limit,

        /**
         * Total number of items matching the query.
         * May be null if the total count is not available or too expensive to compute.
         */
        Integer matched
) {
    /**
     * Builder pattern support for compatibility with existing code.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer returned;
        private Integer limit;
        private Integer matched;

        public Builder returned(Integer returned) {
            this.returned = returned;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder matched(Integer matched) {
            this.matched = matched;
            return this;
        }

        public Context build() {
            return new Context(returned, limit, matched);
        }
    }

    // Accessor methods for compatibility with code expecting getters
    public Integer getReturned() {
        return returned;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getMatched() {
        return matched;
    }
}
