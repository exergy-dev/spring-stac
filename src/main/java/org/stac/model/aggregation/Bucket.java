package org.stac.model.aggregation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a bucket in a term aggregation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Bucket(
        /**
         * The key/value for this bucket.
         */
        Object key,

        /**
         * The data type of the bucket values.
         */
        @JsonProperty("data_type")
        String dataType,

        /**
         * Frequency/count for this bucket.
         */
        Long frequency,

        /**
         * Alias for frequency (some APIs use count).
         */
        Long count,

        /**
         * Start of range for range buckets.
         */
        Object from,

        /**
         * End of range for range buckets.
         */
        Object to
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object key;
        private String dataType;
        private Long frequency;
        private Long count;
        private Object from;
        private Object to;

        public Builder key(Object key) {
            this.key = key;
            return this;
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder frequency(Long frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder count(Long count) {
            this.count = count;
            return this;
        }

        public Builder from(Object from) {
            this.from = from;
            return this;
        }

        public Builder to(Object to) {
            this.to = to;
            return this;
        }

        public Bucket build() {
            return new Bucket(key, dataType, frequency, count, from, to);
        }
    }
}
