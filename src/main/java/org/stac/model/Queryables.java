package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STAC API Queryables model following JSON Schema format.
 * <p>
 * Provides a JSON Schema describing which properties can be used
 * in CQL2 filter expressions for the Filter Extension.
 *
 * @see <a href="https://github.com/stac-api-extensions/filter">STAC API Filter Extension</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Queryables {

    /**
     * JSON Schema version.
     */
    @JsonProperty("$schema")
    @Builder.Default
    private String schema = "https://json-schema.org/draft/2019-09/schema";

    /**
     * Schema identifier (typically the queryables endpoint URL).
     */
    @JsonProperty("$id")
    private String id;

    /**
     * Schema type (always "object" for queryables).
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "object";

    /**
     * Human-readable title for the queryables schema.
     */
    @JsonProperty("title")
    private String title;

    /**
     * Human-readable description of the queryables.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Map of property names to their JSON Schema definitions.
     */
    @JsonProperty("properties")
    private Map<String, QueryableProperty> properties;

    /**
     * Creates a Queryables instance with standard STAC properties.
     *
     * @param id    the schema identifier
     * @param title the schema title
     * @return Queryables with standard properties
     */
    public static Queryables withStandardProperties(String id, String title) {
        Map<String, QueryableProperty> props = new LinkedHashMap<>();

        props.put("id", QueryableProperty.stringProperty(
                "Item ID",
                "Unique identifier for the STAC item"));

        props.put("collection", QueryableProperty.stringProperty(
                "Collection ID",
                "Identifier of the collection this item belongs to"));

        props.put("geometry", QueryableProperty.geometryProperty(
                "Item Geometry",
                "The footprint of the item as a GeoJSON geometry"));

        props.put("datetime", QueryableProperty.datetimeProperty(
                "Datetime",
                "The searchable datetime of the item"));

        props.put("bbox", QueryableProperty.bboxProperty());

        return Queryables.builder()
                .id(id)
                .title(title)
                .description("Queryable properties for STAC items")
                .properties(props)
                .build();
    }

    /**
     * Adds a property to the queryables.
     *
     * @param name     the property name
     * @param property the property definition
     * @return this instance for chaining
     */
    public Queryables addProperty(String name, QueryableProperty property) {
        if (this.properties == null) {
            this.properties = new LinkedHashMap<>();
        }
        this.properties.put(name, property);
        return this;
    }
}
