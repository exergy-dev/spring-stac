package org.stac.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds STAC-specific HTTP headers to API responses.
 * <p>
 * Adds the following headers:
 * <ul>
 *   <li>{@code STAC-Version}: The STAC API version (configurable via stac.api.version)</li>
 *   <li>{@code Content-Crs}: The CRS for geo+json responses (OGC CRS84)</li>
 * </ul>
 * <p>
 * This filter excludes actuator, swagger, and OpenAPI documentation endpoints
 * to avoid interfering with their standard responses.
 */
@Component
public class StacResponseHeaderFilter extends OncePerRequestFilter {

    private static final String STAC_VERSION_HEADER = "STAC-Version";
    private static final String CONTENT_CRS_HEADER = "Content-Crs";
    private static final String CRS84_URI = "<http://www.opengis.net/def/crs/OGC/1.3/CRS84>";
    private static final String GEO_JSON_MEDIA_TYPE = "application/geo+json";

    @Value("${stac.api.version:1.0.0}")
    private String stacVersion;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Add STAC-Version header to all API responses
        response.setHeader(STAC_VERSION_HEADER, stacVersion);

        // Process the request
        filterChain.doFilter(request, response);

        // Add Content-Crs header for geo+json responses
        String contentType = response.getContentType();
        if (contentType != null && contentType.contains(GEO_JSON_MEDIA_TYPE)) {
            response.setHeader(CONTENT_CRS_HEADER, CRS84_URI);
        }
    }

    /**
     * Determines if this filter should be skipped for the given request.
     * <p>
     * Excludes actuator endpoints, swagger UI, and OpenAPI documentation
     * from STAC header processing.
     *
     * @param request the HTTP request
     * @return true if the filter should be skipped, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
