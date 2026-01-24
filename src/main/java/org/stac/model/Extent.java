package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STAC Extent model representing the spatial and temporal extents of a Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#extent-object">STAC Extent Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extent {

    /**
     * Potential spatial extents covered by the Collection.
     */
    @JsonProperty("spatial")
    private SpatialExtent spatial;

    /**
     * Potential temporal extents covered by the Collection.
     */
    @JsonProperty("temporal")
    private TemporalExtent temporal;
}
