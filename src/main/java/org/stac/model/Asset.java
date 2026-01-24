package org.stac.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STAC Asset model representing a link to data associated with an Item or Collection.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/item-spec/item-spec.md#asset-object">STAC Asset Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Asset {

    /**
     * URI to the asset object.
     */
    @NotNull
    @JsonProperty("href")
    private String href;

    /**
     * The displayed title for clients and users.
     */
    @JsonProperty("title")
    private String title;

    /**
     * A description of the Asset providing additional details.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Media type of the asset.
     */
    @JsonProperty("type")
    private String type;

    /**
     * The semantic roles of the asset (e.g., "thumbnail", "overview", "data", "metadata").
     */
    @JsonProperty("roles")
    private List<String> roles;

    /**
     * Additional properties that are not part of the core Asset specification.
     */
    @JsonIgnore
    @Builder.Default
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * Returns additional properties for JSON serialization.
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Sets additional properties during JSON deserialization.
     */
    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(key, value);
    }
}
