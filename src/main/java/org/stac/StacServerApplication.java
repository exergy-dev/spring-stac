package org.stac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the STAC API Server.
 * <p>
 * This Spring Boot application implements the STAC (SpatioTemporal Asset Catalog) API v1.0.0
 * with support for multiple backend data stores including pgSTAC (PostgreSQL) and Elasticsearch.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class StacServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StacServerApplication.class, args);
    }
}
