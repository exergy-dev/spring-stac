package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAC Landing Page Response DTO.
 * Represents the root catalog response of a STAC API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingPage {

    /**
     * Type of STAC object, always "Catalog" for landing page.
     */
    @Builder.Default
    private String type = "Catalog";

    /**
     * STAC specification version.
     */
    @JsonProperty("stac_version")
    @Builder.Default
    private String stacVersion = "1.0.0";

    /**
     * List of STAC extension identifiers used by this catalog.
     */
    @JsonProperty("stac_extensions")
    private List<String> stacExtensions;

    /**
     * Unique identifier for this catalog.
     */
    @NotNull
    private String id;

    /**
     * Human-readable title for this catalog.
     */
    private String title;

    /**
     * Detailed description of this catalog.
     */
    @NotNull
    private String description;

    /**
     * List of conformance classes this API implements.
     */
    private List<String> conformsTo;

    /**
     * Links to related resources (self, root, collections, search, etc.).
     */
    @NotNull
    private List<Link> links;
}
