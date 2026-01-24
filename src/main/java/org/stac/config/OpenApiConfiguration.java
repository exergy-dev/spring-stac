package org.stac.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for the STAC API server.
 * <p>
 * Provides metadata for the API documentation including title, version,
 * description, and server URLs.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "STAC API",
                version = "1.0.0",
                description = """
                        SpatioTemporal Asset Catalog (STAC) API Server.

                        This API provides access to geospatial data following the STAC specification v1.0.0.
                        It enables discovery and access to spatiotemporal assets through a standardized interface.

                        ## Features
                        - Browse and search collections of spatiotemporal data
                        - Filter items using CQL2 (Common Query Language)
                        - Support for spatial, temporal, and property-based queries
                        - Transaction support for creating, updating, and deleting items

                        ## Conformance Classes
                        - Core
                        - Item Search
                        - Filter (CQL2-JSON, CQL2-Text)
                        - Query, Sort, Fields, Context
                        - Transaction
                        """,
                contact = @Contact(
                        name = "STAC API Support"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        servers = {
                @Server(url = "${stac.server.url:http://localhost:8080}", description = "STAC API Server")
        }
)
public class OpenApiConfiguration {
    // Configuration is defined via annotations
}
