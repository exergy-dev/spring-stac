package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single queryable property in a JSON Schema format.
 * <p>
 * Used for the Filter Extension queryables endpoint to describe
 * which properties can be used in CQL2 filter expressions.
 *
 * @see <a href="https://github.com/stac-api-extensions/filter">STAC API Filter Extension</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryableProperty {

    /**
     * Title of the property for display purposes.
     */
    @JsonProperty("title")
    private String title;

    /**
     * Description of the property.
     */
    @JsonProperty("description")
    private String description;

    /**
     * JSON Schema type (string, number, integer, boolean, array, object).
     */
    @JsonProperty("type")
    private String type;

    /**
     * Format hint for string types (e.g., date-time, uri).
     */
    @JsonProperty("format")
    private String format;

    /**
     * Reference to another schema definition.
     */
    @JsonProperty("$ref")
    private String ref;

    /**
     * Allowed values for enum properties.
     */
    @JsonProperty("enum")
    private List<Object> enumValues;

    /**
     * Minimum value for numeric properties.
     */
    @JsonProperty("minimum")
    private Number minimum;

    /**
     * Maximum value for numeric properties.
     */
    @JsonProperty("maximum")
    private Number maximum;

    /**
     * Item type for array properties.
     */
    @JsonProperty("items")
    private QueryableProperty items;

    /**
     * Creates a string property.
     */
    public static QueryableProperty stringProperty(String title, String description) {
        return QueryableProperty.builder()
                .title(title)
                .description(description)
                .type("string")
                .build();
    }

    /**
     * Creates a datetime property.
     */
    public static QueryableProperty datetimeProperty(String title, String description) {
        return QueryableProperty.builder()
                .title(title)
                .description(description)
                .type("string")
                .format("date-time")
                .build();
    }

    /**
     * Creates a number property.
     */
    public static QueryableProperty numberProperty(String title, String description) {
        return QueryableProperty.builder()
                .title(title)
                .description(description)
                .type("number")
                .build();
    }

    /**
     * Creates a geometry reference property.
     */
    public static QueryableProperty geometryProperty(String title, String description) {
        return QueryableProperty.builder()
                .title(title)
                .description(description)
                .ref("https://geojson.org/schema/Geometry.json")
                .build();
    }

    /**
     * Creates a bbox array property.
     */
    public static QueryableProperty bboxProperty() {
        return QueryableProperty.builder()
                .title("Bounding Box")
                .description("Bounding box of the item")
                .type("array")
                .items(QueryableProperty.builder().type("number").build())
                .build();
    }
}
