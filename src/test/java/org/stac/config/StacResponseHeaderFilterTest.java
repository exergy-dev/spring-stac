package org.stac.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for StacResponseHeaderFilter.
 */
@ExtendWith(MockitoExtension.class)
class StacResponseHeaderFilterTest {

    private StacResponseHeaderFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new StacResponseHeaderFilter();
        ReflectionTestUtils.setField(filter, "stacVersion", "1.0.0");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("should add STAC-Version header to all API responses")
    void shouldAddStacVersionHeader() throws ServletException, IOException {
        // Given
        request.setRequestURI("/collections");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("STAC-Version")).isEqualTo("1.0.0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should add Content-Crs header for geo+json responses")
    void shouldAddContentCrsHeaderForGeoJson() throws ServletException, IOException {
        // Given
        request.setRequestURI("/search");
        response.setContentType("application/geo+json");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("STAC-Version")).isEqualTo("1.0.0");
        assertThat(response.getHeader("Content-Crs"))
                .isEqualTo("<http://www.opengis.net/def/crs/OGC/1.3/CRS84>");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should not add Content-Crs header for non-geo+json responses")
    void shouldNotAddContentCrsHeaderForNonGeoJson() throws ServletException, IOException {
        // Given
        request.setRequestURI("/");
        response.setContentType("application/json");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("STAC-Version")).isEqualTo("1.0.0");
        assertThat(response.getHeader("Content-Crs")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should use configurable STAC version")
    void shouldUseConfigurableStacVersion() throws ServletException, IOException {
        // Given
        ReflectionTestUtils.setField(filter, "stacVersion", "1.1.0");
        request.setRequestURI("/collections");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("STAC-Version")).isEqualTo("1.1.0");
    }

    @Test
    @DisplayName("should not filter actuator endpoints")
    void shouldNotFilterActuatorEndpoints() {
        // Given
        request.setRequestURI("/actuator/health");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("should not filter swagger-ui endpoints")
    void shouldNotFilterSwaggerEndpoints() {
        // Given
        request.setRequestURI("/swagger-ui/index.html");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("should not filter OpenAPI documentation endpoints")
    void shouldNotFilterOpenApiEndpoints() {
        // Given
        request.setRequestURI("/v3/api-docs");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("should filter STAC API endpoints")
    void shouldFilterStacApiEndpoints() {
        // Given - various STAC endpoints
        String[] stacEndpoints = {"/", "/conformance", "/collections", "/collections/test", "/search"};

        for (String endpoint : stacEndpoints) {
            request.setRequestURI(endpoint);

            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldNotFilter)
                    .as("Should filter endpoint: %s", endpoint)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("should handle null content type gracefully")
    void shouldHandleNullContentType() throws ServletException, IOException {
        // Given
        request.setRequestURI("/search");
        // Content type is null by default

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("STAC-Version")).isEqualTo("1.0.0");
        assertThat(response.getHeader("Content-Crs")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should add Content-Crs header for geo+json with charset")
    void shouldAddContentCrsHeaderForGeoJsonWithCharset() throws ServletException, IOException {
        // Given
        request.setRequestURI("/search");
        response.setContentType("application/geo+json;charset=UTF-8");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader("Content-Crs"))
                .isEqualTo("<http://www.opengis.net/def/crs/OGC/1.3/CRS84>");
    }
}
