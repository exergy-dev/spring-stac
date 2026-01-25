package org.stac.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;

/**
 * Jackson module for JTS Geometry serialization/deserialization.
 * Uses JTS's built-in GeoJsonReader/GeoJsonWriter for GeoJSON support.
 */
public class JtsModule extends SimpleModule {

    public JtsModule() {
        super("JtsModule");
        addSerializer(Geometry.class, new JtsGeometrySerializer());
        addDeserializer(Geometry.class, new JtsGeometryDeserializer());
    }

    /**
     * Serializer that uses JTS GeoJsonWriter to convert Geometry to GeoJSON.
     */
    static class JtsGeometrySerializer extends JsonSerializer<Geometry> {
        private final GeoJsonWriter writer = new GeoJsonWriter();

        @Override
        public void serialize(Geometry geometry, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            if (geometry == null) {
                gen.writeNull();
            } else {
                gen.writeRawValue(writer.write(geometry));
            }
        }
    }

    /**
     * Deserializer that uses JTS GeoJsonReader to parse GeoJSON to Geometry.
     */
    static class JtsGeometryDeserializer extends JsonDeserializer<Geometry> {
        private final GeoJsonReader reader = new GeoJsonReader();

        @Override
        public Geometry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String json = p.readValueAsTree().toString();
            try {
                return reader.read(json);
            } catch (ParseException e) {
                throw new IOException("Failed to parse GeoJSON geometry", e);
            }
        }
    }
}
