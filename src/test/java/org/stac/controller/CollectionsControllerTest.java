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
import org.stac.exception.ConflictException;
import org.stac.exception.NotFoundException;
import org.stac.model.*;
import org.stac.service.StacService;
import org.stac.util.TestFixtures;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CollectionsController.
 */
@WebMvcTest(CollectionsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfiguration.class)
class CollectionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StacService stacService;

    @Nested
    @DisplayName("GET /collections Tests")
    class GetCollectionsTests {

        @Test
        @DisplayName("should return collections with 200 OK")
        void shouldReturnCollections() throws Exception {
            // Given
            Collection collection1 = TestFixtures.createTestCollection("collection-1");
            Collection collection2 = TestFixtures.createTestCollection("collection-2");

            List<Link> links = List.of(
                    Link.builder()
                            .href("http://localhost:8080/collections")
                            .rel("self")
                            .type("application/json")
                            .build()
            );

            Collections collections = Collections.builder()
                    .collections(List.of(collection1, collection2))
                    .links(links)
                    .build();

            when(stacService.getCollections(any())).thenReturn(collections);

            // When & Then
            mockMvc.perform(get("/collections")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.collections", hasSize(2)))
                    .andExpect(jsonPath("$.collections[0].id", is("collection-1")))
                    .andExpect(jsonPath("$.collections[1].id", is("collection-2")))
                    .andExpect(jsonPath("$.links", hasSize(1)))
                    .andExpect(jsonPath("$.links[0].rel", is("self")));

            verify(stacService).getCollections(any());
        }

        @Test
        @DisplayName("should return collections with limit parameter")
        void shouldReturnCollectionsWithLimit() throws Exception {
            // Given
            Collection collection = TestFixtures.createTestCollection("collection-1");

            Collections collections = Collections.builder()
                    .collections(List.of(collection))
                    .links(List.of())
                    .build();

            when(stacService.getCollections(5)).thenReturn(collections);

            // When & Then
            mockMvc.perform(get("/collections")
                            .param("limit", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.collections", hasSize(1)));

            verify(stacService).getCollections(5);
        }
    }

    @Nested
    @DisplayName("GET /collections/{collectionId} Tests")
    class GetCollectionTests {

        @Test
        @DisplayName("should return collection by ID with 200 OK")
        void shouldReturnCollection() throws Exception {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacService.getCollection(collectionId)).thenReturn(collection);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}", collectionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(collectionId)))
                    .andExpect(jsonPath("$.type", is("Collection")))
                    .andExpect(jsonPath("$.stac_version", is("1.0.0")))
                    .andExpect(jsonPath("$.description", notNullValue()))
                    .andExpect(jsonPath("$.license", is("proprietary")))
                    .andExpect(jsonPath("$.extent", notNullValue()))
                    .andExpect(jsonPath("$.links", notNullValue()));

            verify(stacService).getCollection(collectionId);
        }

        @Test
        @DisplayName("should return 404 for missing collection")
        void shouldReturn404ForMissingCollection() throws Exception {
            // Given
            String collectionId = "non-existent";

            when(stacService.getCollection(collectionId))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}", collectionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(stacService).getCollection(collectionId);
        }
    }

    @Nested
    @DisplayName("POST /collections Tests")
    class CreateCollectionTests {

        @Test
        @DisplayName("should create collection and return 201 Created")
        void createCollection_shouldReturn201() throws Exception {
            // Given
            Collection collection = TestFixtures.createTestCollection("new-collection");

            when(stacService.createCollection(any(Collection.class))).thenReturn(collection);

            // When & Then
            mockMvc.perform(post("/collections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(collection))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is("new-collection")))
                    .andExpect(jsonPath("$.type", is("Collection")));

            verify(stacService).createCollection(any(Collection.class));
        }

        @Test
        @DisplayName("should return 409 when collection already exists")
        void createCollection_shouldReturn409_whenExists() throws Exception {
            // Given
            Collection collection = TestFixtures.createTestCollection("existing-collection");

            when(stacService.createCollection(any(Collection.class)))
                    .thenThrow(ConflictException.collection("existing-collection"));

            // When & Then
            mockMvc.perform(post("/collections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(collection))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());

            verify(stacService).createCollection(any(Collection.class));
        }
    }

    @Nested
    @DisplayName("PUT /collections/{collectionId} Tests")
    class UpdateCollectionTests {

        @Test
        @DisplayName("should update collection and return 200 OK")
        void updateCollection_shouldReturn200() throws Exception {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacService.updateCollection(eq(collectionId), any(Collection.class)))
                    .thenReturn(collection);

            // When & Then
            mockMvc.perform(put("/collections/{collectionId}", collectionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(collection))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(collectionId)));

            verify(stacService).updateCollection(eq(collectionId), any(Collection.class));
        }

        @Test
        @DisplayName("should return 404 when collection not found")
        void updateCollection_shouldReturn404_whenNotFound() throws Exception {
            // Given
            String collectionId = "non-existent";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacService.updateCollection(eq(collectionId), any(Collection.class)))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(put("/collections/{collectionId}", collectionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(collection))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /collections/{collectionId} Tests")
    class DeleteCollectionTests {

        @Test
        @DisplayName("should delete collection and return 204 No Content")
        void deleteCollection_shouldReturn204() throws Exception {
            // Given
            String collectionId = "test-collection";

            doNothing().when(stacService).deleteCollection(collectionId);

            // When & Then
            mockMvc.perform(delete("/collections/{collectionId}", collectionId))
                    .andExpect(status().isNoContent());

            verify(stacService).deleteCollection(collectionId);
        }

        @Test
        @DisplayName("should return 404 when collection not found")
        void deleteCollection_shouldReturn404_whenNotFound() throws Exception {
            // Given
            String collectionId = "non-existent";

            doThrow(NotFoundException.collection(collectionId))
                    .when(stacService).deleteCollection(collectionId);

            // When & Then
            mockMvc.perform(delete("/collections/{collectionId}", collectionId))
                    .andExpect(status().isNotFound());

            verify(stacService).deleteCollection(collectionId);
        }
    }

    @Nested
    @DisplayName("PATCH /collections/{collectionId} Tests")
    class PatchCollectionTests {

        @Test
        @DisplayName("should patch collection and return 200 OK")
        void patchCollection_shouldReturn200() throws Exception {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            collection.setTitle("Patched Title");

            String patch = "{\"title\": \"Patched Title\"}";

            when(stacService.patchCollection(eq(collectionId), any()))
                    .thenReturn(collection);

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}", collectionId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(collectionId)))
                    .andExpect(jsonPath("$.title", is("Patched Title")));

            verify(stacService).patchCollection(eq(collectionId), any());
        }

        @Test
        @DisplayName("should return 404 when collection not found")
        void patchCollection_shouldReturn404_whenNotFound() throws Exception {
            // Given
            String collectionId = "non-existent";
            String patch = "{\"title\": \"Patched Title\"}";

            when(stacService.patchCollection(eq(collectionId), any()))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}", collectionId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should accept merge-patch+json content type")
        void patchCollection_shouldAcceptMergePatchContentType() throws Exception {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            String patch = "{\"title\": \"Patched\"}";

            when(stacService.patchCollection(eq(collectionId), any()))
                    .thenReturn(collection);

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}", collectionId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
