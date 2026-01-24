package org.stac.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stac.exception.NotFoundException;
import org.stac.model.Queryables;
import org.stac.service.StacService;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QueryablesController.
 */
@WebMvcTest(QueryablesController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueryablesControllerTest {

    private static final String SCHEMA_CONTENT_TYPE = "application/schema+json";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StacService stacService;

    @Nested
    @DisplayName("GET /queryables Tests")
    class GetQueryablesTests {

        @Test
        @DisplayName("should return queryables with 200 OK")
        void getQueryables_shouldReturn200() throws Exception {
            // Given
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/queryables",
                    "STAC API Queryables"
            );

            when(stacService.getQueryables()).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/queryables")
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(SCHEMA_CONTENT_TYPE))
                    .andExpect(jsonPath("$.$schema", is("https://json-schema.org/draft/2019-09/schema")))
                    .andExpect(jsonPath("$.$id", is("http://localhost:8080/queryables")))
                    .andExpect(jsonPath("$.type", is("object")))
                    .andExpect(jsonPath("$.title", is("STAC API Queryables")))
                    .andExpect(jsonPath("$.properties", notNullValue()));

            verify(stacService).getQueryables();
        }

        @Test
        @DisplayName("should return schema content type")
        void getQueryables_shouldReturnSchemaContentType() throws Exception {
            // Given
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/queryables",
                    "STAC API Queryables"
            );

            when(stacService.getQueryables()).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/queryables")
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(SCHEMA_CONTENT_TYPE));
        }

        @Test
        @DisplayName("should include id property in queryables")
        void getQueryables_shouldIncludeIdProperty() throws Exception {
            // Given
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/queryables",
                    "STAC API Queryables"
            );

            when(stacService.getQueryables()).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/queryables")
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.id", notNullValue()))
                    .andExpect(jsonPath("$.properties.id.type", is("string")));
        }

        @Test
        @DisplayName("should include datetime property in queryables")
        void getQueryables_shouldIncludeDatetimeProperty() throws Exception {
            // Given
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/queryables",
                    "STAC API Queryables"
            );

            when(stacService.getQueryables()).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/queryables")
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.datetime", notNullValue()))
                    .andExpect(jsonPath("$.properties.datetime.type", is("string")))
                    .andExpect(jsonPath("$.properties.datetime.format", is("date-time")));
        }

        @Test
        @DisplayName("should include geometry property in queryables")
        void getQueryables_shouldIncludeGeometryProperty() throws Exception {
            // Given
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/queryables",
                    "STAC API Queryables"
            );

            when(stacService.getQueryables()).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/queryables")
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.geometry", notNullValue()))
                    .andExpect(jsonPath("$.properties.geometry.$ref", is("https://geojson.org/schema/Geometry.json")));
        }
    }

    @Nested
    @DisplayName("GET /collections/{id}/queryables Tests")
    class GetCollectionQueryablesTests {

        @Test
        @DisplayName("should return collection queryables with 200 OK")
        void getCollectionQueryables_shouldReturn200() throws Exception {
            // Given
            String collectionId = "test-collection";
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/collections/" + collectionId + "/queryables",
                    "Test Collection Queryables"
            );

            when(stacService.getQueryables(collectionId)).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/queryables", collectionId)
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(SCHEMA_CONTENT_TYPE))
                    .andExpect(jsonPath("$.title", is("Test Collection Queryables")))
                    .andExpect(jsonPath("$.properties", notNullValue()));

            verify(stacService).getQueryables(collectionId);
        }

        @Test
        @DisplayName("should return 404 for non-existent collection")
        void getCollectionQueryables_shouldReturn404_whenNotFound() throws Exception {
            // Given
            String collectionId = "non-existent";

            when(stacService.getQueryables(collectionId))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/queryables", collectionId)
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isNotFound());

            verify(stacService).getQueryables(collectionId);
        }

        @Test
        @DisplayName("should include collection property in collection queryables")
        void getCollectionQueryables_shouldIncludeCollectionProperty() throws Exception {
            // Given
            String collectionId = "test-collection";
            Queryables queryables = Queryables.withStandardProperties(
                    "http://localhost:8080/collections/" + collectionId + "/queryables",
                    "Test Collection Queryables"
            );

            when(stacService.getQueryables(collectionId)).thenReturn(queryables);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/queryables", collectionId)
                            .accept(SCHEMA_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.collection", notNullValue()))
                    .andExpect(jsonPath("$.properties.collection.type", is("string")));
        }
    }
}
