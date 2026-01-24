package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response model for STAC Aggregation Extension.
 * <p>
 * Contains aggregated statistics and facet information for STAC items,
 * following the STAC Aggregation Extension specification.
 *
 * @see <a href="https://github.com/stac-api-extensions/aggregation">STAC Aggregation Extension</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AggregationResponse {

    /**
     * Type identifier for the aggregation response.
     */
    @Builder.Default
    private String type = "AggregationCollection";

    /**
     * List of aggregation results, one per requested aggregation field.
     */
    private List<Aggregation> aggregations;

    /**
     * Links related to this aggregation response.
     */
    private List<Link> links;

    /**
     * Represents a single aggregation result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aggregation {

        /**
         * The name/identifier of this aggregation.
         */
        private String name;

        /**
         * The data type of the aggregated values.
         */
        @JsonProperty("data_type")
        private String dataType;

        /**
         * For numeric aggregations: minimum value.
         */
        private Number minimum;

        /**
         * For numeric aggregations: maximum value.
         */
        private Number maximum;

        /**
         * For numeric aggregations: average value.
         */
        private Number average;

        /**
         * For numeric aggregations: sum of values.
         */
        private Number sum;

        /**
         * Total count of values (non-null).
         */
        private Long count;

        /**
         * For term/bucket aggregations: list of buckets with counts.
         */
        private List<Bucket> buckets;

        /**
         * For datetime aggregations: frequency distribution.
         */
        private List<FrequencyDistribution> frequency;

        /**
         * For geometry aggregations: bounding box encompassing all geometries.
         */
        private double[] bounds;

        /**
         * For centroid aggregations: centroid point.
         */
        private Map<String, Object> centroid;

        /**
         * Overflow bucket for values outside the main buckets.
         */
        @JsonProperty("overflow")
        private Long overflow;
    }

    /**
     * Represents a bucket in a term aggregation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Bucket {

        /**
         * The key/value for this bucket.
         */
        private Object key;

        /**
         * The document count for this bucket.
         */
        @JsonProperty("data_type")
        private String dataType;

        /**
         * Frequency/count for this bucket.
         */
        private Long frequency;

        /**
         * Alias for frequency (some APIs use count).
         */
        private Long count;

        /**
         * Start of range for range buckets.
         */
        private Object from;

        /**
         * End of range for range buckets.
         */
        private Object to;
    }

    /**
     * Represents a frequency distribution entry for datetime aggregations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FrequencyDistribution {

        /**
         * The time key (ISO 8601 datetime string).
         */
        private String key;

        /**
         * The document count for this time period.
         */
        private Long frequency;

        /**
         * Start of the time interval.
         */
        private String from;

        /**
         * End of the time interval.
         */
        private String to;
    }

    /**
     * Creates an empty aggregation response.
     */
    public static AggregationResponse empty() {
        return AggregationResponse.builder()
                .aggregations(List.of())
                .build();
    }
}
