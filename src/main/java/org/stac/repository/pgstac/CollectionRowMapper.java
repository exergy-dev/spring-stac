package org.stac.repository.pgstac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.stac.model.Asset;
import org.stac.model.Collection;
import org.stac.model.Extent;
import org.stac.model.Link;
import org.stac.model.Provider;
import org.stac.model.SpatialExtent;
import org.stac.model.TemporalExtent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RowMapper for converting pgSTAC collection results to Collection model objects.
 * <p>
 * pgSTAC returns collections as JSONB in a "content" column.
 * This mapper parses the JSON and constructs a Collection instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRowMapper implements RowMapper<Collection> {

    private final ObjectMapper objectMapper;

    /**
     * Maps a result set row to a Collection.
     *
     * @param rs     the result set positioned at the current row
     * @param rowNum the row number (0-based)
     * @return the mapped Collection
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Collection mapRow(ResultSet rs, int rowNum) throws SQLException {
        String content = rs.getString("content");
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            return parseCollectionFromJson(content);
        } catch (Exception e) {
            log.error("Failed to parse collection JSON from row {}: {}", rowNum, e.getMessage());
            throw new SQLException("Failed to parse collection JSON", e);
        }
    }

    /**
     * Parses a Collection from a JSON string.
     *
     * @param json the JSON string representing the collection
     * @return the parsed Collection
     * @throws Exception if parsing fails
     */
    public Collection parseCollectionFromJson(String json) throws Exception {
        JsonNode rootNode = objectMapper.readTree(json);

        Collection.CollectionBuilder builder = Collection.builder();

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

        if (rootNode.has("title")) {
            builder.title(rootNode.get("title").asText());
        }

        if (rootNode.has("description")) {
            builder.description(rootNode.get("description").asText());
        }

        if (rootNode.has("keywords")) {
            List<String> keywords = objectMapper.convertValue(
                    rootNode.get("keywords"),
                    new TypeReference<List<String>>() {}
            );
            builder.keywords(keywords);
        }

        if (rootNode.has("license")) {
            builder.license(rootNode.get("license").asText());
        }

        // Providers
        if (rootNode.has("providers")) {
            List<Provider> providers = objectMapper.convertValue(
                    rootNode.get("providers"),
                    new TypeReference<List<Provider>>() {}
            );
            builder.providers(providers);
        }

        // Extent
        if (rootNode.has("extent")) {
            Extent extent = parseExtent(rootNode.get("extent"));
            builder.extent(extent);
        }

        // Summaries
        if (rootNode.has("summaries")) {
            Map<String, Object> summaries = objectMapper.convertValue(
                    rootNode.get("summaries"),
                    new TypeReference<Map<String, Object>>() {}
            );
            builder.summaries(summaries);
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

        return builder.build();
    }

    /**
     * Parses an Extent from a JSON node.
     *
     * @param extentNode the JSON node containing extent data
     * @return the parsed Extent
     */
    private Extent parseExtent(JsonNode extentNode) {
        Extent.ExtentBuilder builder = Extent.builder();

        // Spatial extent
        if (extentNode.has("spatial")) {
            JsonNode spatialNode = extentNode.get("spatial");
            var spatialBuilder = SpatialExtent.builder();

            if (spatialNode.has("bbox")) {
                JsonNode bboxArrayNode = spatialNode.get("bbox");
                List<double[]> bboxList = new ArrayList<>();

                for (JsonNode bboxNode : bboxArrayNode) {
                    double[] bbox = new double[bboxNode.size()];
                    for (int i = 0; i < bboxNode.size(); i++) {
                        bbox[i] = bboxNode.get(i).asDouble();
                    }
                    bboxList.add(bbox);
                }
                spatialBuilder.bbox(bboxList);
            }

            builder.spatial(spatialBuilder.build());
        }

        // Temporal extent
        if (extentNode.has("temporal")) {
            JsonNode temporalNode = extentNode.get("temporal");
            var temporalBuilder = TemporalExtent.builder();

            if (temporalNode.has("interval")) {
                JsonNode intervalArrayNode = temporalNode.get("interval");
                List<List<String>> intervalList = new ArrayList<>();

                for (JsonNode intervalNode : intervalArrayNode) {
                    List<String> interval = new ArrayList<>();
                    for (JsonNode dateNode : intervalNode) {
                        interval.add(dateNode.isNull() ? null : dateNode.asText());
                    }
                    intervalList.add(interval);
                }
                temporalBuilder.interval(intervalList);
            }

            builder.temporal(temporalBuilder.build());
        }

        return builder.build();
    }

    /**
     * Parses multiple collections from a JSON array string.
     *
     * @param jsonArray the JSON array string
     * @return list of parsed collections
     * @throws Exception if parsing fails
     */
    public List<Collection> parseCollectionsFromJsonArray(String jsonArray) throws Exception {
        JsonNode arrayNode = objectMapper.readTree(jsonArray);
        List<Collection> collections = new ArrayList<>();

        if (arrayNode.isArray()) {
            for (JsonNode collectionNode : arrayNode) {
                collections.add(parseCollectionFromJson(collectionNode.toString()));
            }
        }

        return collections;
    }
}
