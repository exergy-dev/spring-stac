package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * STAC Link model representing a hyperlink to related resources.
 *
 * @see <a href="https://github.com/radiantearth/stac-spec/blob/master/item-spec/item-spec.md#link-object">STAC Link Object</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Link {

    /**
     * The actual link in the format of an URL.
     */
    @NotNull
    @JsonProperty("href")
    private String href;

    /**
     * Relationship between the current document and the linked document.
     */
    @NotNull
    @JsonProperty("rel")
    private String rel;

    /**
     * Media type of the referenced entity.
     */
    @JsonProperty("type")
    private String type;

    /**
     * A human readable title to be used in rendered displays of the link.
     */
    @JsonProperty("title")
    private String title;

    /**
     * The HTTP method to use when following the link (for POST/PUT/DELETE operations).
     */
    @JsonProperty("method")
    private String method;

    /**
     * HTTP headers to include when following the link.
     */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Request body to include when following the link.
     */
    @JsonProperty("body")
    private Object body;

    /**
     * Whether the body should be merged with existing request body.
     */
    @JsonProperty("merge")
    private Boolean merge;
}
