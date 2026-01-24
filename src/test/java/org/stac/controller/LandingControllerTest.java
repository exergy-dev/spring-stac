package org.stac.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.stac.model.Conformance;
import org.stac.model.LandingPage;
import org.stac.model.Link;
import org.stac.service.StacService;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LandingController.
 */
@WebMvcTest(LandingController.class)
@AutoConfigureMockMvc(addFilters = false)
class LandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StacService stacService;

    @Test
    @DisplayName("should return landing page with 200 OK and expected JSON structure")
    void shouldReturnLandingPage() throws Exception {
        // Given
        List<Link> links = List.of(
                Link.builder()
                        .href("http://localhost:8080")
                        .rel("self")
                        .type("application/json")
                        .title("This catalog")
                        .build(),
                Link.builder()
                        .href("http://localhost:8080")
                        .rel("root")
                        .type("application/json")
                        .title("Root catalog")
                        .build(),
                Link.builder()
                        .href("http://localhost:8080/conformance")
                        .rel("conformance")
                        .type("application/json")
                        .title("Conformance classes")
                        .build(),
                Link.builder()
                        .href("http://localhost:8080/collections")
                        .rel("data")
                        .type("application/json")
                        .title("Collections")
                        .build(),
                Link.builder()
                        .href("http://localhost:8080/search")
                        .rel("search")
                        .type("application/geo+json")
                        .title("Search")
                        .method("GET")
                        .build()
        );

        List<String> conformsTo = List.of(
                "https://api.stacspec.org/v1.0.0/core",
                "https://api.stacspec.org/v1.0.0/item-search",
                "https://api.stacspec.org/v1.0.0/item-search#filter"
        );

        LandingPage landingPage = LandingPage.builder()
                .id("stac-api")
                .type("Catalog")
                .stacVersion("1.0.0")
                .title("STAC API")
                .description("A STAC API implementation")
                .conformsTo(conformsTo)
                .links(links)
                .build();

        when(stacService.getLandingPage()).thenReturn(landingPage);

        // When & Then
        mockMvc.perform(get("/")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("Catalog")))
                .andExpect(jsonPath("$.stac_version", is("1.0.0")))
                .andExpect(jsonPath("$.id", is("stac-api")))
                .andExpect(jsonPath("$.title", is("STAC API")))
                .andExpect(jsonPath("$.description", is("A STAC API implementation")))
                .andExpect(jsonPath("$.conformsTo", hasSize(3)))
                .andExpect(jsonPath("$.conformsTo[0]", is("https://api.stacspec.org/v1.0.0/core")))
                .andExpect(jsonPath("$.links", hasSize(5)))
                .andExpect(jsonPath("$.links[?(@.rel=='self')].href", hasItem("http://localhost:8080")))
                .andExpect(jsonPath("$.links[?(@.rel=='root')].href", hasItem("http://localhost:8080")))
                .andExpect(jsonPath("$.links[?(@.rel=='conformance')].href", hasItem("http://localhost:8080/conformance")))
                .andExpect(jsonPath("$.links[?(@.rel=='data')].href", hasItem("http://localhost:8080/collections")))
                .andExpect(jsonPath("$.links[?(@.rel=='search')].href", hasItem("http://localhost:8080/search")));

        verify(stacService).getLandingPage();
    }

    @Test
    @DisplayName("should return conformance with 200 OK and conformance classes")
    void shouldReturnConformance() throws Exception {
        // Given
        List<String> conformsTo = List.of(
                "https://api.stacspec.org/v1.0.0/core",
                "https://api.stacspec.org/v1.0.0/item-search",
                "https://api.stacspec.org/v1.0.0/item-search#filter",
                "https://api.stacspec.org/v1.0.0/item-search#query",
                "https://api.stacspec.org/v1.0.0/item-search#sort",
                "https://api.stacspec.org/v1.0.0/ogcapi-features",
                "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
                "http://www.opengis.net/spec/cql2/1.0/conf/cql2-text",
                "http://www.opengis.net/spec/cql2/1.0/conf/cql2-json"
        );

        Conformance conformance = Conformance.builder()
                .conformsTo(conformsTo)
                .build();

        when(stacService.getConformance()).thenReturn(conformance);

        // When & Then
        mockMvc.perform(get("/conformance")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.conformsTo", hasSize(9)))
                .andExpect(jsonPath("$.conformsTo", hasItem("https://api.stacspec.org/v1.0.0/core")))
                .andExpect(jsonPath("$.conformsTo", hasItem("https://api.stacspec.org/v1.0.0/item-search")))
                .andExpect(jsonPath("$.conformsTo", hasItem("https://api.stacspec.org/v1.0.0/item-search#filter")))
                .andExpect(jsonPath("$.conformsTo", hasItem("http://www.opengis.net/spec/cql2/1.0/conf/cql2-text")))
                .andExpect(jsonPath("$.conformsTo", hasItem("http://www.opengis.net/spec/cql2/1.0/conf/cql2-json")));

        verify(stacService).getConformance();
    }

    @Test
    @DisplayName("should return landing page with correct content type")
    void shouldReturnLandingPageWithCorrectContentType() throws Exception {
        // Given
        LandingPage landingPage = LandingPage.builder()
                .id("stac-api")
                .type("Catalog")
                .stacVersion("1.0.0")
                .title("STAC API")
                .description("Test")
                .links(List.of())
                .build();

        when(stacService.getLandingPage()).thenReturn(landingPage);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("should return conformance with correct content type")
    void shouldReturnConformanceWithCorrectContentType() throws Exception {
        // Given
        Conformance conformance = Conformance.builder()
                .conformsTo(List.of("https://api.stacspec.org/v1.0.0/core"))
                .build();

        when(stacService.getConformance()).thenReturn(conformance);

        // When & Then
        mockMvc.perform(get("/conformance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
