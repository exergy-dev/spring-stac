package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAC Spatial Extent model representing the spatial extents of a Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#spatial-extent-object">STAC Spatial Extent Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpatialExtent {

    /**
     * Potential spatial extents covered by the Collection.
     * Each bounding box is an array of 4 or 6 numbers:
     * [west, south, east, north] or [west, south, min-elev, east, north, max-elev]
     * The first bounding box describes the overall spatial extent of the data.
     */
    @JsonProperty("bbox")
    private List<double[]> bbox;
}
