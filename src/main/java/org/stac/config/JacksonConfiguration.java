package org.stac.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.locationtech.jts.geom.Geometry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.stac.util.GeometryDeserializer;
import org.stac.util.GeometrySerializer;

/**
 * Jackson configuration for the STAC API server.
 * Registers custom serializers/deserializers for JTS geometries
 * and configures JSON processing settings.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register JTS Geometry serializers
        SimpleModule geometryModule = new SimpleModule("JTSGeometryModule");
        geometryModule.addSerializer(Geometry.class, new GeometrySerializer());
        geometryModule.addDeserializer(Geometry.class, new GeometryDeserializer());
        mapper.registerModule(geometryModule);

        // Configure serialization features
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
