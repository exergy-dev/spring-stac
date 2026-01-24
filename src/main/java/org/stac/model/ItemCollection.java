package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAC ItemCollection Response DTO.
 * Represents a GeoJSON FeatureCollection containing STAC Items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemCollection {

    /**
     * GeoJSON type, always "FeatureCollection".
     */
    @Builder.Default
    private String type = "FeatureCollection";

    /**
     * Array of STAC Items (GeoJSON Features).
     */
    private List<Item> features;

    /**
     * Links for pagination and related resources.
     */
    private List<Link> links;

    /**
     * STAC Context extension object providing result metadata.
     */
    private Context context;

    /**
     * Timestamp indicating when the response was generated (ISO 8601).
     */
    @JsonProperty("timestamp")
    private String timeStamp;

    /**
     * Total number of items matching the query (OGC API - Features).
     */
    @JsonProperty("numberMatched")
    private Integer numberMatched;

    /**
     * Number of items returned in this response (OGC API - Features).
     */
    @JsonProperty("numberReturned")
    private Integer numberReturned;
}
