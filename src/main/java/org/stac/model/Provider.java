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
 * STAC Provider model representing an organization that captures, processes, or hosts STAC data.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/collection-spec/collection-spec.md#provider-object">STAC Provider Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {

    /**
     * The name of the organization or individual.
     */
    @NotNull
    @JsonProperty("name")
    private String name;

    /**
     * Multi-line description to add further information about the provider.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Roles of the provider. Valid values are: "producer", "licensor", "processor", "host".
     */
    @JsonProperty("roles")
    private List<String> roles;

    /**
     * Homepage URL of the provider.
     */
    @JsonProperty("url")
    private String url;
}
