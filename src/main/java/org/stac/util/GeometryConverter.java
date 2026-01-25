package org.stac.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility component for converting between JTS Geometry and GeoJSON Map representations.
 * <p>
 * This consolidates geometry conversion logic previously duplicated across
 * ElasticsearchItemMapper and Cql2ToElasticsearchConverter.
 */
@Component
public class GeometryConverter {

    private static final int SRID_WGS84 = 4326;
    private final GeometryFactory geometryFactory;

    public GeometryConverter() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    /**
     * Converts a JTS Geometry to a GeoJSON Map representation.
     *
     * @param geometry the JTS Geometry to convert
     * @return a Map representing the GeoJSON structure
     */
    public Map<String, Object> toGeoJson(Geometry geometry) {
        if (geometry == null) {
            return null;
        }

        Map<String, Object> geoJson = new HashMap<>();
        String type = geometry.getGeometryType();
        geoJson.put("type", type);

        if (geometry instanceof GeometryCollection gc && !(geometry instanceof MultiPoint)
                && !(geometry instanceof MultiLineString) && !(geometry instanceof MultiPolygon)) {
            List<Map<String, Object>> geometries = new ArrayList<>();
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                geometries.add(toGeoJson(gc.getGeometryN(i)));
            }
            geoJson.put("geometries", geometries);
        } else {
            geoJson.put("coordinates", extractCoordinates(geometry));
        }

        return geoJson;
    }

    /**
     * Converts a GeoJSON Map to a JTS Geometry.
     *
     * @param geoJson the GeoJSON Map to convert
     * @return the JTS Geometry
     * @throws IllegalArgumentException if the geometry type is unsupported
     */
    @SuppressWarnings("unchecked")
    public Geometry fromGeoJson(Map<String, Object> geoJson) {
        if (geoJson == null) {
            return null;
        }

        String type = (String) geoJson.get("type");
        Object coords = geoJson.get("coordinates");

        return switch (type) {
            case "Point" -> createPoint((List<Number>) coords);
            case "LineString" -> createLineString((List<List<Number>>) coords);
            case "Polygon" -> createPolygon((List<List<List<Number>>>) coords);
            case "MultiPoint" -> createMultiPoint((List<List<Number>>) coords);
            case "MultiLineString" -> createMultiLineString((List<List<List<Number>>>) coords);
            case "MultiPolygon" -> createMultiPolygon((List<List<List<List<Number>>>>) coords);
            case "GeometryCollection" -> createGeometryCollection(
                    (List<Map<String, Object>>) geoJson.get("geometries"));
            default -> throw new IllegalArgumentException("Unsupported geometry type: " + type);
        };
    }

    /**
     * Extracts coordinates from a JTS Geometry in GeoJSON coordinate format.
     */
    private Object extractCoordinates(Geometry geometry) {
        return switch (geometry) {
            case Point p -> List.of(p.getX(), p.getY());
            case LineString ls -> extractLineCoordinates(ls);
            case Polygon polygon -> extractPolygonCoordinates(polygon);
            case MultiPoint mp -> extractMultiPointCoordinates(mp);
            case MultiLineString mls -> extractMultiLineStringCoordinates(mls);
            case MultiPolygon mpoly -> extractMultiPolygonCoordinates(mpoly);
            default -> throw new IllegalArgumentException(
                    "Unsupported geometry type for coordinate extraction: " + geometry.getGeometryType());
        };
    }

    private List<List<Double>> extractLineCoordinates(Geometry geometry) {
        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coord : geometry.getCoordinates()) {
            coords.add(List.of(coord.x, coord.y));
        }
        return coords;
    }

    private List<List<List<Double>>> extractPolygonCoordinates(Polygon polygon) {
        List<List<List<Double>>> rings = new ArrayList<>();
        rings.add(extractLineCoordinates(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            rings.add(extractLineCoordinates(polygon.getInteriorRingN(i)));
        }
        return rings;
    }

    private List<List<Double>> extractMultiPointCoordinates(MultiPoint mp) {
        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coord : mp.getCoordinates()) {
            coords.add(List.of(coord.x, coord.y));
        }
        return coords;
    }

    private List<List<List<Double>>> extractMultiLineStringCoordinates(MultiLineString mls) {
        List<List<List<Double>>> lines = new ArrayList<>();
        for (int i = 0; i < mls.getNumGeometries(); i++) {
            lines.add(extractLineCoordinates(mls.getGeometryN(i)));
        }
        return lines;
    }

    private List<List<List<List<Double>>>> extractMultiPolygonCoordinates(MultiPolygon mp) {
        List<List<List<List<Double>>>> polygons = new ArrayList<>();
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            polygons.add(extractPolygonCoordinates((Polygon) mp.getGeometryN(i)));
        }
        return polygons;
    }

    private Point createPoint(List<Number> coords) {
        return geometryFactory.createPoint(
                new Coordinate(coords.get(0).doubleValue(), coords.get(1).doubleValue()));
    }

    private LineString createLineString(List<List<Number>> coords) {
        return geometryFactory.createLineString(toCoordinateArray(coords));
    }

    private Polygon createPolygon(List<List<List<Number>>> rings) {
        LinearRing shell = geometryFactory.createLinearRing(toCoordinateArray(rings.get(0)));
        LinearRing[] holes = new LinearRing[rings.size() - 1];
        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = geometryFactory.createLinearRing(toCoordinateArray(rings.get(i)));
        }
        return geometryFactory.createPolygon(shell, holes);
    }

    private MultiPoint createMultiPoint(List<List<Number>> coords) {
        return geometryFactory.createMultiPointFromCoords(toCoordinateArray(coords));
    }

    private MultiLineString createMultiLineString(List<List<List<Number>>> lines) {
        LineString[] lineStrings = new LineString[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lineStrings[i] = geometryFactory.createLineString(toCoordinateArray(lines.get(i)));
        }
        return geometryFactory.createMultiLineString(lineStrings);
    }

    private MultiPolygon createMultiPolygon(List<List<List<List<Number>>>> polygons) {
        Polygon[] polygonArray = new Polygon[polygons.size()];
        for (int i = 0; i < polygons.size(); i++) {
            polygonArray[i] = createPolygon(polygons.get(i));
        }
        return geometryFactory.createMultiPolygon(polygonArray);
    }

    private GeometryCollection createGeometryCollection(List<Map<String, Object>> geometries) {
        Geometry[] geomArray = new Geometry[geometries.size()];
        for (int i = 0; i < geometries.size(); i++) {
            geomArray[i] = fromGeoJson(geometries.get(i));
        }
        return geometryFactory.createGeometryCollection(geomArray);
    }

    private Coordinate[] toCoordinateArray(List<List<Number>> coords) {
        Coordinate[] result = new Coordinate[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            List<Number> coord = coords.get(i);
            result[i] = new Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue());
        }
        return result;
    }
}
