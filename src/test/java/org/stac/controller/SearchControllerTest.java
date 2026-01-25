package org.stac.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.stac.config.JacksonConfiguration;
import org.stac.model.*;
import org.stac.service.StacService;
import org.stac.util.TestFixtures;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SearchController.
 */
@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfiguration.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StacService stacService;

    @Nested
    @DisplayName("GET /search Tests")
    class GetSearchTests {

        @Test
        @DisplayName("should search with GET and return 200 OK")
        void shouldSearchWithGet() throws Exception {
            // Given
            Item item1 = TestFixtures.createTestItem("item-1", "collection-1");
            Item item2 = TestFixtures.createTestItem("item-2", "collection-1");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item1, item2))
                    .context(Context.builder().returned(2).matched(2).limit(10).build())
                    .links(List.of(
                            Link.builder()
                                    .href("http://localhost:8080/search")
                                    .rel("self")
                                    .type("application/geo+json")
                                    .build()
                    ))
                    .numberReturned(2)
                    .numberMatched(2)
                    .timeStamp("2023-01-01T00:00:00Z")
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.type", is("FeatureCollection")))
                    .andExpect(jsonPath("$.features", hasSize(2)))
                    .andExpect(jsonPath("$.features[0].id", is("item-1")))
                    .andExpect(jsonPath("$.features[1].id", is("item-2")))
                    .andExpect(jsonPath("$.context.returned", is(2)))
                    .andExpect(jsonPath("$.context.matched", is(2)))
                    .andExpect(jsonPath("$.numberReturned", is(2)))
                    .andExpect(jsonPath("$.numberMatched", is(2)))
                    .andExpect(jsonPath("$.links", hasSize(1)))
                    .andExpect(jsonPath("$.links[0].rel", is("self")));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with collections parameter")
        void shouldSearchWithCollections() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("collections", "collection-1,collection-2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("FeatureCollection")));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with bbox parameter")
        void shouldSearchWithBbox() throws Exception {
            // Given
            Item item = TestFixtures.createTestItem("item-1", "collection-1", 0.5, 0.5);

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item))
                    .context(Context.builder().returned(1).matched(1).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("bbox", "-180,-90,180,90")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(1)))
                    .andExpect(jsonPath("$.features[0].id", is("item-1")));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with datetime parameter")
        void shouldSearchWithDatetime() throws Exception {
            // Given
            Item item = TestFixtures.createTestItemWithDatetime("item-1", "collection-1", "2023-06-15T12:00:00Z");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item))
                    .context(Context.builder().returned(1).matched(1).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("datetime", "2023-01-01T00:00:00Z/2023-12-31T23:59:59Z")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(1)));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with single datetime")
        void shouldSearchWithSingleDatetime() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("datetime", "2023-06-15T12:00:00Z")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with limit parameter")
        void shouldSearchWithLimit() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(5).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("limit", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.context.limit", is(5)));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with ids parameter")
        void shouldSearchWithIds() throws Exception {
            // Given
            Item item1 = TestFixtures.createTestItem("item-1", "collection-1");
            Item item2 = TestFixtures.createTestItem("item-2", "collection-1");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item1, item2))
                    .context(Context.builder().returned(2).matched(2).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("ids", "item-1,item-2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(2)));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with multiple parameters")
        void shouldSearchWithMultipleParameters() throws Exception {
            // Given
            Item item = TestFixtures.createTestItem("item-1", "collection-1");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item))
                    .context(Context.builder().returned(1).matched(1).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .param("collections", "collection-1")
                            .param("bbox", "-10,-10,10,10")
                            .param("datetime", "2023-01-01T00:00:00Z/..")
                            .param("limit", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(1)));

            verify(stacService).search(any(SearchRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /search Tests")
    class PostSearchTests {

        @Test
        @DisplayName("should search with POST and return 200 OK")
        void shouldSearchWithPost() throws Exception {
            // Given
            Item item1 = TestFixtures.createTestItem("item-1", "collection-1");
            Item item2 = TestFixtures.createTestItem("item-2", "collection-2");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item1, item2))
                    .context(Context.builder().returned(2).matched(2).limit(10).build())
                    .links(List.of())
                    .numberReturned(2)
                    .numberMatched(2)
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .collections(List.of("collection-1", "collection-2"))
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("FeatureCollection")))
                    .andExpect(jsonPath("$.features", hasSize(2)))
                    .andExpect(jsonPath("$.context.returned", is(2)));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and bbox")
        void shouldSearchWithPostAndBbox() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .bbox("-180,-90,180,90")
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("FeatureCollection")));

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and datetime interval")
        void shouldSearchWithPostAndDatetime() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .datetime("2020-01-01T00:00:00Z/2020-12-31T23:59:59Z")
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and intersects geometry")
        void shouldSearchWithPostAndIntersects() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            String geoJson = """
                {
                    "type": "Polygon",
                    "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]]
                }
                """;

            SearchRequest request = SearchRequest.builder()
                    .intersects(geoJson)
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and CQL2 filter")
        void shouldSearchWithPostAndCql2Filter() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            String requestJson = """
                {
                    "filter": {
                        "op": "=",
                        "args": [{"property": "platform"}, "sentinel-2"]
                    },
                    "filter-lang": "cql2-json",
                    "limit": 10
                }
                """;

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and sortby")
        void shouldSearchWithPostAndSortby() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .sortby(List.of("-datetime", "+id"))
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and fields selection")
        void shouldSearchWithPostAndFields() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .fields("id,properties.datetime,-properties.eo:cloud_cover")
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(stacService).search(any(SearchRequest.class));
        }

        @Test
        @DisplayName("should search with POST and all parameters")
        void shouldSearchWithPostAndAllParameters() throws Exception {
            // Given
            Item item = TestFixtures.createTestItem("item-1", "collection-1");

            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item))
                    .context(Context.builder().returned(1).matched(1).limit(10).build())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder()
                    .collections(List.of("collection-1"))
                    .ids(List.of("item-1"))
                    .bbox("-10,-10,10,10")
                    .datetime("2020-01-01T00:00:00Z/2023-12-31T23:59:59Z")
                    .sortby(List.of("-datetime"))
                    .limit(10)
                    .build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(1)));

            verify(stacService).search(any(SearchRequest.class));
        }
    }

    @Nested
    @DisplayName("Content Type Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("should accept application/json for GET")
        void shouldAcceptApplicationJsonForGet() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept application/geo+json for GET")
        void shouldAcceptGeoJsonForGet() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/search")
                            .accept("application/geo+json"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept application/json for POST")
        void shouldAcceptApplicationJsonForPost() throws Exception {
            // Given
            ItemCollection result = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .links(List.of())
                    .build();

            when(stacService.search(any(SearchRequest.class))).thenReturn(result);

            SearchRequest request = SearchRequest.builder().build();

            // When & Then
            mockMvc.perform(post("/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
