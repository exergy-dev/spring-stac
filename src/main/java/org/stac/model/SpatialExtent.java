package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * STAC Spatial Extent model representing the spatial extents of a Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#spatial-extent-object">STAC Spatial Extent Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpatialExtent(
        /**
         * Potential spatial extents covered by the Collection.
         * Each bounding box is an array of 4 or 6 numbers:
         * [west, south, east, north] or [west, south, min-elev, east, north, max-elev]
         * The first bounding box describes the overall spatial extent of the data.
         */
        @JsonProperty("bbox")
        List<double[]> bbox
) {
    /**
     * Builder pattern support for compatibility with existing code.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<double[]> bbox;

        public Builder bbox(List<double[]> bbox) {
            this.bbox = bbox;
            return this;
        }

        public SpatialExtent build() {
            return new SpatialExtent(bbox);
        }
    }

    // Accessor method for compatibility with code expecting getter
    public List<double[]> getBbox() {
        return bbox;
    }
}
