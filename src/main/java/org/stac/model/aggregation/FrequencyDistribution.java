package org.stac.model.aggregation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a frequency distribution entry for datetime aggregations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrequencyDistribution(
        /**
         * The time key (ISO 8601 datetime string).
         */
        String key,

        /**
         * The document count for this time period.
         */
        Long frequency,

        /**
         * Start of the time interval.
         */
        String from,

        /**
         * End of the time interval.
         */
        String to
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String key;
        private Long frequency;
        private String from;
        private String to;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder frequency(Long frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public FrequencyDistribution build() {
            return new FrequencyDistribution(key, frequency, from, to);
        }
    }
}
