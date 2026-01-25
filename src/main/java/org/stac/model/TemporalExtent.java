package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * STAC Temporal Extent model representing the temporal extents of a Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#temporal-extent-object">STAC Temporal Extent Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemporalExtent(
        /**
         * Potential temporal extents covered by the Collection.
         * Each extent is an array of two timestamps in RFC 3339 format (ISO 8601 datetime).
         * The first extent describes the overall temporal extent of the data.
         * Open date ranges are supported by setting the start or end time to null.
         */
        @JsonProperty("interval")
        List<List<String>> interval
) {
    /**
     * Builder pattern support for compatibility with existing code.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<List<String>> interval;

        public Builder interval(List<List<String>> interval) {
            this.interval = interval;
            return this;
        }

        public TemporalExtent build() {
            return new TemporalExtent(interval);
        }
    }

    // Accessor method for compatibility with code expecting getter
    public List<List<String>> getInterval() {
        return interval;
    }
}
