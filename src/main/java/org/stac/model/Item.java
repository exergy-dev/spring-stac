package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.stac.util.GeometryDeserializer;
import org.stac.util.GeometrySerializer;

import java.util.List;
import java.util.Map;

/**
 * STAC Item model representing a GeoJSON Feature enhanced with STAC properties.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/item-spec/item-spec.md">STAC Item Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {

    /**
     * Type of the GeoJSON Object. Must be set to "Feature".
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "Feature";

    /**
     * The STAC version the Item implements.
     */
    @JsonProperty("stac_version")
    @Builder.Default
    private String stacVersion = "1.0.0";

    /**
     * A list of extension identifiers the Item implements.
     */
    @JsonProperty("stac_extensions")
    private List<String> stacExtensions;

    /**
     * Provider identifier. Must be unique within the Collection.
     */
    @NotNull
    @JsonProperty("id")
    private String id;

    /**
     * Defines the full footprint of the asset represented by this item.
     * GeoJSON Geometry object as defined in RFC 7946.
     */
    @JsonProperty("geometry")
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Geometry geometry;

    /**
     * Bounding Box of the asset represented by this Item.
     * Array of 4 or 6 numbers: [west, south, east, north] or [west, south, min-elev, east, north, max-elev].
     */
    @JsonProperty("bbox")
    private double[] bbox;

    /**
     * A dictionary of additional metadata for the Item.
     * Requires at minimum the "datetime" property.
     */
    @NotNull
    @JsonProperty("properties")
    private Map<String, Object> properties;

    /**
     * A list of references to other documents.
     */
    @JsonProperty("links")
    private List<Link> links;

    /**
     * Dictionary of asset objects keyed by their role or path.
     */
    @JsonProperty("assets")
    private Map<String, Asset> assets;

    /**
     * The id of the STAC Collection this Item references.
     */
    @JsonProperty("collection")
    private String collection;
}
