package org.stac.repository.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;
import org.stac.model.Asset;
import org.stac.model.Item;
import org.stac.model.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between STAC Items and Elasticsearch documents.
 * <p>
 * This component handles the serialization and deserialization of STAC Items
 * to/from Elasticsearch document format, including proper handling of
 * geometries in GeoJSON format.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchItemMapper {

    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Converts a STAC Item to an Elasticsearch document (Map).
     *
     * @param item the Item to convert
     * @return a Map representing the Elasticsearch document
     */
    public Map<String, Object> toDocument(Item item) {
        Map<String, Object> doc = new HashMap<>();

        // Core fields
        doc.put("type", item.getType());
        doc.put("stac_version", item.getStacVersion());
        doc.put("id", item.getId());
        doc.put("collection", item.getCollection());

        // STAC extensions
        if (item.getStacExtensions() != null) {
            doc.put("stac_extensions", item.getStacExtensions());
        }

        // Geometry in GeoJSON format for geo_shape field
        if (item.getGeometry() != null) {
            doc.put("geometry", geometryToGeoJson(item.getGeometry()));
        }

        // Bounding box
        if (item.getBbox() != null) {
            doc.put("bbox", item.getBbox());
        }

        // Properties
        if (item.getProperties() != null) {
            doc.put("properties", item.getProperties());
        }

        // Links
        if (item.getLinks() != null) {
            List<Map<String, Object>> linkMaps = new ArrayList<>();
            for (Link link : item.getLinks()) {
                linkMaps.add(linkToMap(link));
            }
            doc.put("links", linkMaps);
        }

        // Assets
        if (item.getAssets() != null) {
            Map<String, Object> assetMaps = new HashMap<>();
            for (Map.Entry<String, Asset> entry : item.getAssets().entrySet()) {
                assetMaps.put(entry.getKey(), assetToMap(entry.getValue()));
            }
            doc.put("assets", assetMaps);
        }

        return doc;
    }

    /**
     * Converts an Elasticsearch document (Map) to a STAC Item.
     *
     * @param doc the Elasticsearch document
     * @return the converted Item
     */
    @SuppressWarnings("unchecked")
    public Item fromDocument(Map<String, Object> doc) {
        Item.ItemBuilder builder = Item.builder();

        // Core fields
        builder.type((String) doc.getOrDefault("type", "Feature"));
        builder.stacVersion((String) doc.getOrDefault("stac_version", "1.0.0"));
        builder.id((String) doc.get("id"));
        builder.collection((String) doc.get("collection"));

        // STAC extensions
        Object extensions = doc.get("stac_extensions");
        if (extensions instanceof List) {
            builder.stacExtensions((List<String>) extensions);
        }

        // Geometry
        Object geometry = doc.get("geometry");
        if (geometry instanceof Map) {
            builder.geometry(geoJsonToGeometry((Map<String, Object>) geometry));
        }

        // Bounding box
        Object bbox = doc.get("bbox");
        if (bbox instanceof List) {
            List<Number> bboxList = (List<Number>) bbox;
            double[] bboxArray = new double[bboxList.size()];
            for (int i = 0; i < bboxList.size(); i++) {
                bboxArray[i] = bboxList.get(i).doubleValue();
            }
            builder.bbox(bboxArray);
        } else if (bbox instanceof double[]) {
            builder.bbox((double[]) bbox);
        }

        // Properties
        Object properties = doc.get("properties");
        if (properties instanceof Map) {
            builder.properties((Map<String, Object>) properties);
        }

        // Links
        Object links = doc.get("links");
        if (links instanceof List) {
            List<Link> linkList = new ArrayList<>();
            for (Object linkObj : (List<?>) links) {
                if (linkObj instanceof Map) {
                    linkList.add(mapToLink((Map<String, Object>) linkObj));
                }
            }
            builder.links(linkList);
        }

        // Assets
        Object assets = doc.get("assets");
        if (assets instanceof Map) {
            Map<String, Asset> assetMap = new HashMap<>();
            Map<String, Object> assetsObj = (Map<String, Object>) assets;
            for (Map.Entry<String, Object> entry : assetsObj.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    assetMap.put(entry.getKey(), mapToAsset((Map<String, Object>) entry.getValue()));
                }
            }
            builder.assets(assetMap);
        }

        return builder.build();
    }

    /**
     * Converts an Elasticsearch search hit to a STAC Item.
     *
     * @param hit the Elasticsearch search hit containing an ObjectNode source
     * @return the converted Item
     */
    public Item toSearchHit(co.elastic.clients.elasticsearch.core.search.Hit<ObjectNode> hit) {
        ObjectNode source = hit.source();
        if (source == null) {
            return null;
        }

        return fromObjectNode(source);
    }

    /**
     * Converts a Jackson ObjectNode to a STAC Item.
     *
     * @param node the ObjectNode to convert
     * @return the converted Item
     */
    @SuppressWarnings("unchecked")
    public Item fromObjectNode(ObjectNode node) {
        // Convert ObjectNode to Map using ObjectMapper
        Map<String, Object> doc = objectMapper.convertValue(node, Map.class);
        return fromDocument(doc);
    }

    /**
     * Converts a JTS Geometry to a GeoJSON Map.
     */
    private Map<String, Object> geometryToGeoJson(Geometry geometry) {
        Map<String, Object> geoJson = new HashMap<>();
        String type = geometry.getGeometryType();
        geoJson.put("type", type);

        switch (type) {
            case "Point" -> {
                Coordinate coord = geometry.getCoordinate();
                geoJson.put("coordinates", List.of(coord.x, coord.y));
            }
            case "LineString" -> {
                geoJson.put("coordinates", extractLineCoordinates(geometry));
            }
            case "Polygon" -> {
                org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                List<List<List<Double>>> rings = new ArrayList<>();

                // Exterior ring
                rings.add(extractLineCoordinates(polygon.getExteriorRing()));

                // Interior rings (holes)
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    rings.add(extractLineCoordinates(polygon.getInteriorRingN(i)));
                }
                geoJson.put("coordinates", rings);
            }
            case "MultiPoint" -> {
                List<List<Double>> coords = new ArrayList<>();
                for (Coordinate coord : geometry.getCoordinates()) {
                    coords.add(List.of(coord.x, coord.y));
                }
                geoJson.put("coordinates", coords);
            }
            case "MultiLineString" -> {
                org.locationtech.jts.geom.MultiLineString mls = (org.locationtech.jts.geom.MultiLineString) geometry;
                List<List<List<Double>>> lines = new ArrayList<>();
                for (int i = 0; i < mls.getNumGeometries(); i++) {
                    lines.add(extractLineCoordinates(mls.getGeometryN(i)));
                }
                geoJson.put("coordinates", lines);
            }
            case "MultiPolygon" -> {
                org.locationtech.jts.geom.MultiPolygon mp = (org.locationtech.jts.geom.MultiPolygon) geometry;
                List<List<List<List<Double>>>> polygons = new ArrayList<>();
                for (int i = 0; i < mp.getNumGeometries(); i++) {
                    org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) mp.getGeometryN(i);
                    List<List<List<Double>>> rings = new ArrayList<>();
                    rings.add(extractLineCoordinates(polygon.getExteriorRing()));
                    for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
                        rings.add(extractLineCoordinates(polygon.getInteriorRingN(j)));
                    }
                    polygons.add(rings);
                }
                geoJson.put("coordinates", polygons);
            }
            case "GeometryCollection" -> {
                org.locationtech.jts.geom.GeometryCollection gc = (org.locationtech.jts.geom.GeometryCollection) geometry;
                List<Map<String, Object>> geometries = new ArrayList<>();
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    geometries.add(geometryToGeoJson(gc.getGeometryN(i)));
                }
                geoJson.put("geometries", geometries);
            }
        }

        return geoJson;
    }

    /**
     * Extracts coordinates from a linear geometry.
     */
    private List<List<Double>> extractLineCoordinates(Geometry geometry) {
        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coord : geometry.getCoordinates()) {
            coords.add(List.of(coord.x, coord.y));
        }
        return coords;
    }

    /**
     * Converts a GeoJSON Map to a JTS Geometry.
     */
    @SuppressWarnings("unchecked")
    private Geometry geoJsonToGeometry(Map<String, Object> geoJson) {
        String type = (String) geoJson.get("type");
        Object coords = geoJson.get("coordinates");

        return switch (type) {
            case "Point" -> {
                List<Number> coord = (List<Number>) coords;
                yield geometryFactory.createPoint(
                        new Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue()));
            }
            case "LineString" -> {
                List<List<Number>> lineCoords = (List<List<Number>>) coords;
                yield geometryFactory.createLineString(toCoordinateArray(lineCoords));
            }
            case "Polygon" -> {
                List<List<List<Number>>> rings = (List<List<List<Number>>>) coords;
                LinearRing shell = geometryFactory.createLinearRing(toCoordinateArray(rings.get(0)));
                LinearRing[] holes = new LinearRing[rings.size() - 1];
                for (int i = 1; i < rings.size(); i++) {
                    holes[i - 1] = geometryFactory.createLinearRing(toCoordinateArray(rings.get(i)));
                }
                yield geometryFactory.createPolygon(shell, holes);
            }
            case "MultiPoint" -> {
                List<List<Number>> points = (List<List<Number>>) coords;
                Coordinate[] coordinates = toCoordinateArray(points);
                yield geometryFactory.createMultiPointFromCoords(coordinates);
            }
            case "MultiLineString" -> {
                List<List<List<Number>>> lines = (List<List<List<Number>>>) coords;
                org.locationtech.jts.geom.LineString[] lineStrings =
                        new org.locationtech.jts.geom.LineString[lines.size()];
                for (int i = 0; i < lines.size(); i++) {
                    lineStrings[i] = geometryFactory.createLineString(toCoordinateArray(lines.get(i)));
                }
                yield geometryFactory.createMultiLineString(lineStrings);
            }
            case "MultiPolygon" -> {
                List<List<List<List<Number>>>> polygons = (List<List<List<List<Number>>>>) coords;
                org.locationtech.jts.geom.Polygon[] polygonArray =
                        new org.locationtech.jts.geom.Polygon[polygons.size()];
                for (int i = 0; i < polygons.size(); i++) {
                    List<List<List<Number>>> rings = polygons.get(i);
                    LinearRing shell = geometryFactory.createLinearRing(toCoordinateArray(rings.get(0)));
                    LinearRing[] holes = new LinearRing[rings.size() - 1];
                    for (int j = 1; j < rings.size(); j++) {
                        holes[j - 1] = geometryFactory.createLinearRing(toCoordinateArray(rings.get(j)));
                    }
                    polygonArray[i] = geometryFactory.createPolygon(shell, holes);
                }
                yield geometryFactory.createMultiPolygon(polygonArray);
            }
            case "GeometryCollection" -> {
                List<Map<String, Object>> geometries = (List<Map<String, Object>>) geoJson.get("geometries");
                Geometry[] geomArray = new Geometry[geometries.size()];
                for (int i = 0; i < geometries.size(); i++) {
                    geomArray[i] = geoJsonToGeometry(geometries.get(i));
                }
                yield geometryFactory.createGeometryCollection(geomArray);
            }
            default -> throw new IllegalArgumentException("Unsupported geometry type: " + type);
        };
    }

    /**
     * Converts a list of coordinate pairs to a Coordinate array.
     */
    private Coordinate[] toCoordinateArray(List<List<Number>> coords) {
        Coordinate[] result = new Coordinate[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            List<Number> coord = coords.get(i);
            result[i] = new Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue());
        }
        return result;
    }

    /**
     * Converts a Link to a Map.
     */
    private Map<String, Object> linkToMap(Link link) {
        Map<String, Object> map = new HashMap<>();
        map.put("href", link.getHref());
        map.put("rel", link.getRel());
        if (link.getType() != null) {
            map.put("type", link.getType());
        }
        if (link.getTitle() != null) {
            map.put("title", link.getTitle());
        }
        if (link.getMethod() != null) {
            map.put("method", link.getMethod());
        }
        if (link.getHeaders() != null) {
            map.put("headers", link.getHeaders());
        }
        if (link.getBody() != null) {
            map.put("body", link.getBody());
        }
        if (link.getMerge() != null) {
            map.put("merge", link.getMerge());
        }
        return map;
    }

    /**
     * Converts a Map to a Link.
     */
    @SuppressWarnings("unchecked")
    private Link mapToLink(Map<String, Object> map) {
        return Link.builder()
                .href((String) map.get("href"))
                .rel((String) map.get("rel"))
                .type((String) map.get("type"))
                .title((String) map.get("title"))
                .method((String) map.get("method"))
                .headers((Map<String, String>) map.get("headers"))
                .body(map.get("body"))
                .merge((Boolean) map.get("merge"))
                .build();
    }

    /**
     * Converts an Asset to a Map.
     */
    private Map<String, Object> assetToMap(Asset asset) {
        Map<String, Object> map = new HashMap<>();
        map.put("href", asset.getHref());
        if (asset.getTitle() != null) {
            map.put("title", asset.getTitle());
        }
        if (asset.getDescription() != null) {
            map.put("description", asset.getDescription());
        }
        if (asset.getType() != null) {
            map.put("type", asset.getType());
        }
        if (asset.getRoles() != null) {
            map.put("roles", asset.getRoles());
        }
        if (asset.getAdditionalProperties() != null) {
            map.putAll(asset.getAdditionalProperties());
        }
        return map;
    }

    /**
     * Converts a Map to an Asset.
     */
    @SuppressWarnings("unchecked")
    private Asset mapToAsset(Map<String, Object> map) {
        Asset.AssetBuilder builder = Asset.builder()
                .href((String) map.get("href"))
                .title((String) map.get("title"))
                .description((String) map.get("description"))
                .type((String) map.get("type"));

        Object roles = map.get("roles");
        if (roles instanceof List) {
            builder.roles((List<String>) roles);
        }

        // Handle additional properties
        Map<String, Object> additionalProps = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!List.of("href", "title", "description", "type", "roles").contains(key)) {
                additionalProps.put(key, entry.getValue());
            }
        }
        if (!additionalProps.isEmpty()) {
            builder.additionalProperties(additionalProps);
        }

        return builder.build();
    }
}
