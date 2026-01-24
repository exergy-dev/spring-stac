package org.stac.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

/**
 * Jackson serializer for JTS Geometry objects to GeoJSON format.
 * Converts JTS geometries to RFC 7946 compliant GeoJSON.
 */
public class GeometrySerializer extends JsonSerializer<Geometry> {

    @Override
    public void serialize(Geometry geometry, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (geometry == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        gen.writeStringField("type", geometry.getGeometryType());

        if (geometry instanceof GeometryCollection && !(geometry instanceof MultiPoint)
                && !(geometry instanceof MultiLineString) && !(geometry instanceof MultiPolygon)) {
            gen.writeFieldName("geometries");
            writeGeometryCollectionGeometries((GeometryCollection) geometry, gen);
        } else {
            gen.writeFieldName("coordinates");
            writeCoordinates(geometry, gen);
        }
        gen.writeEndObject();
    }

    private void writeCoordinates(Geometry geometry, JsonGenerator gen) throws IOException {
        if (geometry instanceof Point) {
            writePointCoordinates((Point) geometry, gen);
        } else if (geometry instanceof LineString) {
            writeLineStringCoordinates((LineString) geometry, gen);
        } else if (geometry instanceof Polygon) {
            writePolygonCoordinates((Polygon) geometry, gen);
        } else if (geometry instanceof MultiPoint) {
            writeMultiPointCoordinates((MultiPoint) geometry, gen);
        } else if (geometry instanceof MultiLineString) {
            writeMultiLineStringCoordinates((MultiLineString) geometry, gen);
        } else if (geometry instanceof MultiPolygon) {
            writeMultiPolygonCoordinates((MultiPolygon) geometry, gen);
        } else if (geometry instanceof GeometryCollection) {
            writeGeometryCollectionCoordinates((GeometryCollection) geometry, gen);
        }
    }

    private void writePointCoordinates(Point point, JsonGenerator gen) throws IOException {
        writeCoordinate(point.getCoordinate(), gen);
    }

    private void writeLineStringCoordinates(LineString lineString, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        for (Coordinate coord : lineString.getCoordinates()) {
            writeCoordinate(coord, gen);
        }
        gen.writeEndArray();
    }

    private void writePolygonCoordinates(Polygon polygon, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        // Exterior ring
        writeLineStringCoordinates(polygon.getExteriorRing(), gen);
        // Interior rings (holes)
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            writeLineStringCoordinates(polygon.getInteriorRingN(i), gen);
        }
        gen.writeEndArray();
    }

    private void writeMultiPointCoordinates(MultiPoint multiPoint, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
            writeCoordinate(multiPoint.getGeometryN(i).getCoordinate(), gen);
        }
        gen.writeEndArray();
    }

    private void writeMultiLineStringCoordinates(MultiLineString multiLineString, JsonGenerator gen)
            throws IOException {
        gen.writeStartArray();
        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
            writeLineStringCoordinates((LineString) multiLineString.getGeometryN(i), gen);
        }
        gen.writeEndArray();
    }

    private void writeMultiPolygonCoordinates(MultiPolygon multiPolygon, JsonGenerator gen)
            throws IOException {
        gen.writeStartArray();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            writePolygonCoordinates((Polygon) multiPolygon.getGeometryN(i), gen);
        }
        gen.writeEndArray();
    }

    private void writeGeometryCollectionCoordinates(GeometryCollection collection, JsonGenerator gen)
            throws IOException {
        // This should not be called for pure GeometryCollection, only for Multi* types
        gen.writeNull();
    }

    private void writeGeometryCollectionGeometries(GeometryCollection collection, JsonGenerator gen)
            throws IOException {
        gen.writeStartArray();
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            Geometry geom = collection.getGeometryN(i);
            serialize(geom, gen, null);
        }
        gen.writeEndArray();
    }

    private void writeCoordinate(Coordinate coord, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        gen.writeNumber(coord.getX());
        gen.writeNumber(coord.getY());
        if (!Double.isNaN(coord.getZ())) {
            gen.writeNumber(coord.getZ());
        }
        gen.writeEndArray();
    }

    @Override
    public Class<Geometry> handledType() {
        return Geometry.class;
    }
}
