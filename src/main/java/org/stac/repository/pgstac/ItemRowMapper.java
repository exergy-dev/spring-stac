package org.stac.repository.pgstac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.stac.model.Asset;
import org.stac.model.Item;
import org.stac.model.Link;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RowMapper for converting pgSTAC item results to Item model objects.
 * <p>
 * pgSTAC returns items as JSONB in a "content" column.
 * This mapper parses the JSON and constructs an Item instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemRowMapper implements RowMapper<Item> {

    private final ObjectMapper objectMapper;

    /**
     * Maps a result set row to an Item.
     *
     * @param rs     the result set positioned at the current row
     * @param rowNum the row number (0-based)
     * @return the mapped Item
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
        String content = rs.getString("content");
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            return parseItemFromJson(content);
        } catch (Exception e) {
            log.error("Failed to parse item JSON from row {}: {}", rowNum, e.getMessage());
            throw new SQLException("Failed to parse item JSON", e);
        }
    }

    /**
     * Parses an Item from a JSON string.
     *
     * @param json the JSON string representing the item
     * @return the parsed Item
     * @throws Exception if parsing fails
     */
    public Item parseItemFromJson(String json) throws Exception {
        JsonNode rootNode = objectMapper.readTree(json);

        Item.ItemBuilder builder = Item.builder();

        // Basic fields
        if (rootNode.has("type")) {
            builder.type(rootNode.get("type").asText());
        }

        if (rootNode.has("stac_version")) {
            builder.stacVersion(rootNode.get("stac_version").asText());
        }

        if (rootNode.has("stac_extensions")) {
            List<String> extensions = objectMapper.convertValue(
                    rootNode.get("stac_extensions"),
                    new TypeReference<List<String>>() {}
            );
            builder.stacExtensions(extensions);
        }

        if (rootNode.has("id")) {
            builder.id(rootNode.get("id").asText());
        }

        // Geometry
        if (rootNode.has("geometry") && !rootNode.get("geometry").isNull()) {
            Geometry geometry = parseGeometry(rootNode.get("geometry"));
            builder.geometry(geometry);
        }

        // Bounding box
        if (rootNode.has("bbox") && !rootNode.get("bbox").isNull()) {
            JsonNode bboxNode = rootNode.get("bbox");
            double[] bbox = new double[bboxNode.size()];
            for (int i = 0; i < bboxNode.size(); i++) {
                bbox[i] = bboxNode.get(i).asDouble();
            }
            builder.bbox(bbox);
        }

        // Properties
        if (rootNode.has("properties")) {
            Map<String, Object> properties = objectMapper.convertValue(
                    rootNode.get("properties"),
                    new TypeReference<Map<String, Object>>() {}
            );
            builder.properties(properties);
        }

        // Links
        if (rootNode.has("links")) {
            List<Link> links = objectMapper.convertValue(
                    rootNode.get("links"),
                    new TypeReference<List<Link>>() {}
            );
            builder.links(links);
        }

        // Assets
        if (rootNode.has("assets")) {
            Map<String, Asset> assets = objectMapper.convertValue(
                    rootNode.get("assets"),
                    new TypeReference<Map<String, Asset>>() {}
            );
            builder.assets(assets);
        }

        // Collection reference
        if (rootNode.has("collection")) {
            builder.collection(rootNode.get("collection").asText());
        }

        return builder.build();
    }

    /**
     * Parses a JTS Geometry from a GeoJSON node.
     *
     * @param geometryNode the GeoJSON geometry node
     * @return the parsed JTS Geometry
     * @throws Exception if parsing fails
     */
    private Geometry parseGeometry(JsonNode geometryNode) throws Exception {
        // Use the ObjectMapper which should have GeoJSON deserialization configured
        String geoJson = objectMapper.writeValueAsString(geometryNode);
        return objectMapper.readValue(geoJson, Geometry.class);
    }

    /**
     * Parses multiple items from a JSON array string.
     *
     * @param jsonArray the JSON array string
     * @return list of parsed items
     * @throws Exception if parsing fails
     */
    public List<Item> parseItemsFromJsonArray(String jsonArray) throws Exception {
        JsonNode arrayNode = objectMapper.readTree(jsonArray);
        List<Item> items = new ArrayList<>();

        if (arrayNode.isArray()) {
            for (JsonNode itemNode : arrayNode) {
                items.add(parseItemFromJson(itemNode.toString()));
            }
        }

        return items;
    }
}
