package org.stac.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

    @Nested
    @DisplayName("GET /collections/{collectionId}/items Tests")
    class GetItemsTests {

        @Test
        @DisplayName("should return items with 200 OK")
        void shouldReturnItems() throws Exception {
            // Given
            String collectionId = "test-collection";
            Item item1 = TestFixtures.createTestItem("item-1", collectionId);
            Item item2 = TestFixtures.createTestItem("item-2", collectionId);

            ItemCollection itemCollection = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item1, item2))
                    .context(Context.builder().returned(2).matched(2).limit(10).build())
                    .links(List.of())
                    .numberReturned(2)
                    .numberMatched(2)
                    .build();

            when(stacService.getItems(eq(collectionId), any(ItemQueryCriteria.class)))
                    .thenReturn(itemCollection);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/items", collectionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("FeatureCollection")))
                    .andExpect(jsonPath("$.features", hasSize(2)))
                    .andExpect(jsonPath("$.features[0].id", is("item-1")))
                    .andExpect(jsonPath("$.features[1].id", is("item-2")))
                    .andExpect(jsonPath("$.context.returned", is(2)))
                    .andExpect(jsonPath("$.numberReturned", is(2)))
                    .andExpect(jsonPath("$.numberMatched", is(2)));

            verify(stacService).getItems(eq(collectionId), any(ItemQueryCriteria.class));
        }

        @Test
        @DisplayName("should return items with query parameters")
        void shouldReturnItemsWithQueryParameters() throws Exception {
            // Given
            String collectionId = "test-collection";
            Item item = TestFixtures.createTestItem("item-1", collectionId);

            ItemCollection itemCollection = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(item))
                    .context(Context.builder().returned(1).matched(1).limit(5).build())
                    .links(List.of())
                    .build();

            when(stacService.getItems(eq(collectionId), any(ItemQueryCriteria.class)))
                    .thenReturn(itemCollection);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/items", collectionId)
                            .param("limit", "5")
                            .param("bbox", "-180,-90,180,90")
                            .param("datetime", "2020-01-01T00:00:00Z/2020-12-31T23:59:59Z")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.features", hasSize(1)));

            verify(stacService).getItems(eq(collectionId), any(ItemQueryCriteria.class));
        }

        @Test
        @DisplayName("should return 404 for items in missing collection")
        void shouldReturn404ForItemsInMissingCollection() throws Exception {
            // Given
            String collectionId = "non-existent";

            when(stacService.getItems(eq(collectionId), any(ItemQueryCriteria.class)))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/items", collectionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /collections/{collectionId}/items/{itemId} Tests")
    class GetItemTests {

        @Test
        @DisplayName("should return item by ID with 200 OK")
        void shouldReturnItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            when(stacService.getItem(collectionId, itemId)).thenReturn(item);

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(itemId)))
                    .andExpect(jsonPath("$.type", is("Feature")))
                    .andExpect(jsonPath("$.stac_version", is("1.0.0")))
                    .andExpect(jsonPath("$.collection", is(collectionId)))
                    .andExpect(jsonPath("$.geometry", notNullValue()))
                    .andExpect(jsonPath("$.bbox", notNullValue()))
                    .andExpect(jsonPath("$.properties", notNullValue()))
                    .andExpect(jsonPath("$.links", notNullValue()))
                    .andExpect(jsonPath("$.assets", notNullValue()));

            verify(stacService).getItem(collectionId, itemId);
        }

        @Test
        @DisplayName("should return 404 for missing item")
        void shouldReturn404ForMissingItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";

            when(stacService.getItem(collectionId, itemId))
                    .thenThrow(NotFoundException.item(collectionId, itemId));

            // When & Then
            mockMvc.perform(get("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(stacService).getItem(collectionId, itemId);
        }
    }

    @Nested
    @DisplayName("POST /collections/{collectionId}/items Tests")
    class CreateItemTests {

        @Test
        @DisplayName("should create item and return 201 Created")
        void shouldCreateItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            Item item = TestFixtures.createTestItem("new-item", collectionId);

            when(stacService.createItem(eq(collectionId), any(Item.class))).thenReturn(item);

            // When & Then
            mockMvc.perform(post("/collections/{collectionId}/items", collectionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is("new-item")))
                    .andExpect(jsonPath("$.type", is("Feature")))
                    .andExpect(jsonPath("$.collection", is(collectionId)));

            verify(stacService).createItem(eq(collectionId), any(Item.class));
        }

        @Test
        @DisplayName("should return 404 when creating item in non-existent collection")
        void shouldReturn404WhenCreatingItemInMissingCollection() throws Exception {
            // Given
            String collectionId = "non-existent";
            Item item = TestFixtures.createTestItem("new-item", collectionId);

            when(stacService.createItem(eq(collectionId), any(Item.class)))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(post("/collections/{collectionId}/items", collectionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /collections/{collectionId}/items/{itemId} Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("should update item and return 200 OK")
        void shouldUpdateItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            when(stacService.updateItem(eq(collectionId), any(Item.class))).thenReturn(item);

            // When & Then
            mockMvc.perform(put("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(itemId)))
                    .andExpect(jsonPath("$.collection", is(collectionId)));

            verify(stacService).updateItem(eq(collectionId), any(Item.class));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent item")
        void shouldReturn404WhenUpdatingMissingItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            when(stacService.updateItem(eq(collectionId), any(Item.class)))
                    .thenThrow(NotFoundException.item(collectionId, itemId));

            // When & Then
            mockMvc.perform(put("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /collections/{collectionId}/items/{itemId} Tests")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete item and return 204 No Content")
        void shouldDeleteItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";

            doNothing().when(stacService).deleteItem(collectionId, itemId);

            // When & Then
            mockMvc.perform(delete("/collections/{collectionId}/items/{itemId}", collectionId, itemId))
                    .andExpect(status().isNoContent());

            verify(stacService).deleteItem(collectionId, itemId);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent item")
        void shouldReturn404WhenDeletingMissingItem() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";

            doThrow(NotFoundException.item(collectionId, itemId))
                    .when(stacService).deleteItem(collectionId, itemId);

            // When & Then
            mockMvc.perform(delete("/collections/{collectionId}/items/{itemId}", collectionId, itemId))
                    .andExpect(status().isNotFound());

            verify(stacService).deleteItem(collectionId, itemId);
        }
    }

    @Nested
    @DisplayName("PATCH /collections/{collectionId}/items/{itemId} Tests")
    class PatchItemTests {

        @Test
        @DisplayName("should patch item and return 200 OK")
        void patchItem_shouldReturn200() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            String patch = "{\"properties\": {\"title\": \"Patched Title\"}}";

            when(stacService.patchItem(eq(collectionId), eq(itemId), any()))
                    .thenReturn(item);

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(itemId)))
                    .andExpect(jsonPath("$.collection", is(collectionId)));

            verify(stacService).patchItem(eq(collectionId), eq(itemId), any());
        }

        @Test
        @DisplayName("should return 404 when item not found")
        void patchItem_shouldReturn404_whenItemNotFound() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";
            String patch = "{\"properties\": {\"title\": \"Patched\"}}";

            when(stacService.patchItem(eq(collectionId), eq(itemId), any()))
                    .thenThrow(NotFoundException.item(collectionId, itemId));

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when collection not found")
        void patchItem_shouldReturn404_whenCollectionNotFound() throws Exception {
            // Given
            String collectionId = "non-existent";
            String itemId = "test-item";
            String patch = "{\"properties\": {\"title\": \"Patched\"}}";

            when(stacService.patchItem(eq(collectionId), eq(itemId), any()))
                    .thenThrow(NotFoundException.collection(collectionId));

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should update properties via patch")
        void patchItem_shouldUpdateProperties() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);
            item.getProperties().put("title", "Patched Title");

            String patch = "{\"properties\": {\"title\": \"Patched Title\"}}";

            when(stacService.patchItem(eq(collectionId), eq(itemId), any()))
                    .thenReturn(item);

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.title", is("Patched Title")));
        }

        @Test
        @DisplayName("should not change item ID via patch")
        void patchItem_shouldNotChangeId() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            String patch = "{\"id\": \"new-id\"}";

            when(stacService.patchItem(eq(collectionId), eq(itemId), any()))
                    .thenReturn(item);

            // When & Then
            mockMvc.perform(patch("/collections/{collectionId}/items/{itemId}", collectionId, itemId)
                            .contentType("application/merge-patch+json")
                            .content(patch)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(itemId))); // ID should remain unchanged
        }
    }
}
