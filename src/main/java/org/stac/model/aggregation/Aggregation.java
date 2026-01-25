package org.stac.model.aggregation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents a single aggregation result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Aggregation(
        /**
         * The name/identifier of this aggregation.
         */
        String name,

        /**
         * The data type of the aggregated values.
         */
        @JsonProperty("data_type")
        String dataType,

        /**
         * For numeric aggregations: minimum value.
         */
        Number minimum,

        /**
         * For numeric aggregations: maximum value.
         */
        Number maximum,

        /**
         * For numeric aggregations: average value.
         */
        Number average,

        /**
         * For numeric aggregations: sum of values.
         */
        Number sum,

        /**
         * Total count of values (non-null).
         */
        Long count,

        /**
         * For term/bucket aggregations: list of buckets with counts.
         */
        List<Bucket> buckets,

        /**
         * For datetime aggregations: frequency distribution.
         */
        List<FrequencyDistribution> frequency,

        /**
         * For geometry aggregations: bounding box encompassing all geometries.
         */
        double[] bounds,

        /**
         * For centroid aggregations: centroid point.
         */
        Map<String, Object> centroid,

        /**
         * Overflow bucket for values outside the main buckets.
         */
        @JsonProperty("overflow")
        Long overflow
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String dataType;
        private Number minimum;
        private Number maximum;
        private Number average;
        private Number sum;
        private Long count;
        private List<Bucket> buckets;
        private List<FrequencyDistribution> frequency;
        private double[] bounds;
        private Map<String, Object> centroid;
        private Long overflow;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder minimum(Number minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder maximum(Number maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder average(Number average) {
            this.average = average;
            return this;
        }

        public Builder sum(Number sum) {
            this.sum = sum;
            return this;
        }

        public Builder count(Long count) {
            this.count = count;
            return this;
        }

        public Builder buckets(List<Bucket> buckets) {
            this.buckets = buckets;
            return this;
        }

        public Builder frequency(List<FrequencyDistribution> frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder bounds(double[] bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder centroid(Map<String, Object> centroid) {
            this.centroid = centroid;
            return this;
        }

        public Builder overflow(Long overflow) {
            this.overflow = overflow;
            return this;
        }

        public Aggregation build() {
            return new Aggregation(name, dataType, minimum, maximum, average, sum,
                    count, buckets, frequency, bounds, centroid, overflow);
        }
    }
}
