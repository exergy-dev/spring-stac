package org.stac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the STAC API server.
 * <p>
 * Registers interceptors, CORS mappings, and other MVC customizations.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${stac.cors.enabled:true}")
    private boolean corsEnabled;

    @Value("${stac.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${stac.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${stac.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${stac.cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsEnabled) {
            return;
        }

        registry.addMapping("/**")
                .allowedOrigins(parseCommaSeparated(allowedOrigins))
                .allowedMethods(parseCommaSeparated(allowedMethods))
                .allowedHeaders(parseCommaSeparated(allowedHeaders))
                .maxAge(maxAge);
    }

    private String[] parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return value.split(",");
    }
}
