package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * STAC Collection model representing a group of related Items.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md">STAC Collection Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collection {

    /**
     * Type of the GeoJSON Object. Must be set to "Collection".
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "Collection";

    /**
     * The STAC version the Collection implements.
     */
    @JsonProperty("stac_version")
    @Builder.Default
    private String stacVersion = "1.0.0";

    /**
     * A list of extension identifiers the Collection implements.
     */
    @JsonProperty("stac_extensions")
    private List<String> stacExtensions;

    /**
     * Identifier for the Collection that is unique across the provider.
     */
    @NotNull
    @JsonProperty("id")
    private String id;

    /**
     * A short descriptive one-line title for the Collection.
     */
    @JsonProperty("title")
    private String title;

    /**
     * Detailed multi-line description to fully explain the Collection.
     */
    @NotNull
    @JsonProperty("description")
    private String description;

    /**
     * List of keywords describing the Collection.
     */
    @JsonProperty("keywords")
    private List<String> keywords;

    /**
     * Collection's license(s), either a SPDX License identifier or "proprietary" or "various".
     */
    @NotNull
    @JsonProperty("license")
    private String license;

    /**
     * A list of providers that capture, process, or host the data.
     */
    @JsonProperty("providers")
    private List<Provider> providers;

    /**
     * Spatial and temporal extents covered by the Collection.
     */
    @NotNull
    @JsonProperty("extent")
    private Extent extent;

    /**
     * A map of property summaries, providing statistics or value sets for the properties.
     */
    @JsonProperty("summaries")
    private Map<String, Object> summaries;

    /**
     * A list of references to other documents.
     */
    @JsonProperty("links")
    private List<Link> links;

    /**
     * Dictionary of asset objects that are associated with the Collection.
     */
    @JsonProperty("assets")
    private Map<String, Asset> assets;
}
