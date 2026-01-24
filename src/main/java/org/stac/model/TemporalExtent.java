package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAC Temporal Extent model representing the temporal extents of a Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#temporal-extent-object">STAC Temporal Extent Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemporalExtent {

    /**
     * Potential temporal extents covered by the Collection.
     * Each extent is an array of two timestamps in RFC 3339 format (ISO 8601 datetime).
     * The first extent describes the overall temporal extent of the data.
     * Open date ranges are supported by setting the start or end time to null.
     */
    @JsonProperty("interval")
    private List<List<String>> interval;
}
