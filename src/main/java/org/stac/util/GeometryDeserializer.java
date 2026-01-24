package org.stac.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson deserializer for GeoJSON to JTS Geometry objects.
 * Converts RFC 7946 compliant GeoJSON to JTS geometries.
 */
public class GeometryDeserializer extends JsonDeserializer<Geometry> {

    private static final int SRID_WGS84 = 4326;
    private final GeometryFactory geometryFactory;

    public GeometryDeserializer() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    @Override
    public Geometry deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return parseGeometry(node);
    }

    private Geometry parseGeometry(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String type = node.get("type").asText();
        JsonNode coordinates = node.get("coordinates");

        return switch (type) {
            case "Point" -> parsePoint(coordinates);
            case "LineString" -> parseLineString(coordinates);
            case "Polygon" -> parsePolygon(coordinates);
            case "MultiPoint" -> parseMultiPoint(coordinates);
            case "MultiLineString" -> parseMultiLineString(coordinates);
            case "MultiPolygon" -> parseMultiPolygon(coordinates);
            case "GeometryCollection" -> parseGeometryCollection(node.get("geometries"));
            default -> throw new IllegalArgumentException("Unsupported geometry type: " + type);
        };
    }

    private Geometry parsePoint(JsonNode coordinates) {
        Coordinate coord = parseCoordinate(coordinates);
        return geometryFactory.createPoint(coord);
    }

    private Geometry parseLineString(JsonNode coordinates) {
        Coordinate[] coords = parseCoordinateArray(coordinates);
        return geometryFactory.createLineString(coords);
    }

    private Geometry parsePolygon(JsonNode coordinates) {
        if (coordinates.size() == 0) {
            return geometryFactory.createPolygon();
        }

        LinearRing shell = geometryFactory.createLinearRing(parseCoordinateArray(coordinates.get(0)));

        if (coordinates.size() == 1) {
            return geometryFactory.createPolygon(shell);
        }

        LinearRing[] holes = new LinearRing[coordinates.size() - 1];
        for (int i = 1; i < coordinates.size(); i++) {
            holes[i - 1] = geometryFactory.createLinearRing(parseCoordinateArray(coordinates.get(i)));
        }

        return geometryFactory.createPolygon(shell, holes);
    }

    private Geometry parseMultiPoint(JsonNode coordinates) {
        Coordinate[] coords = parseCoordinateArray(coordinates);
        return geometryFactory.createMultiPointFromCoords(coords);
    }

    private Geometry parseMultiLineString(JsonNode coordinates) {
        org.locationtech.jts.geom.LineString[] lineStrings =
                new org.locationtech.jts.geom.LineString[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            lineStrings[i] = geometryFactory.createLineString(parseCoordinateArray(coordinates.get(i)));
        }
        return geometryFactory.createMultiLineString(lineStrings);
    }

    private Geometry parseMultiPolygon(JsonNode coordinates) {
        org.locationtech.jts.geom.Polygon[] polygons =
                new org.locationtech.jts.geom.Polygon[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            polygons[i] = (org.locationtech.jts.geom.Polygon) parsePolygon(coordinates.get(i));
        }
        return geometryFactory.createMultiPolygon(polygons);
    }

    private Geometry parseGeometryCollection(JsonNode geometries) {
        if (geometries == null || geometries.size() == 0) {
            return geometryFactory.createGeometryCollection();
        }

        Geometry[] geoms = new Geometry[geometries.size()];
        for (int i = 0; i < geometries.size(); i++) {
            geoms[i] = parseGeometry(geometries.get(i));
        }
        return geometryFactory.createGeometryCollection(geoms);
    }

    private Coordinate parseCoordinate(JsonNode node) {
        double x = node.get(0).asDouble();
        double y = node.get(1).asDouble();

        if (node.size() > 2) {
            double z = node.get(2).asDouble();
            return new Coordinate(x, y, z);
        }

        return new Coordinate(x, y);
    }

    private Coordinate[] parseCoordinateArray(JsonNode node) {
        List<Coordinate> coords = new ArrayList<>();
        for (JsonNode coordNode : node) {
            coords.add(parseCoordinate(coordNode));
        }
        return coords.toArray(new Coordinate[0]);
    }

    @Override
    public Class<?> handledType() {
        return Geometry.class;
    }
}
