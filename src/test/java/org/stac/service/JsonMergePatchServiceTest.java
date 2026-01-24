package org.stac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stac.model.Collection;
import org.stac.model.Item;
import org.stac.util.TestFixtures;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonMergePatchService.
 * Tests RFC 7386 JSON Merge Patch compliance.
 */
class JsonMergePatchServiceTest {

    private JsonMergePatchService jsonMergePatchService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonMergePatchService = new JsonMergePatchService(objectMapper);
    }

    @Nested
    @DisplayName("RFC 7386 Compliance Tests")
    class Rfc7386ComplianceTests {

        @Test
        @DisplayName("should replace value when patch contains same property")
        void applyPatch_shouldReplaceValue() throws Exception {
            // Given
            TestObject original = new TestObject("original", 100);
            JsonNode patch = objectMapper.readTree("{\"name\": \"updated\"}");

            // When
            TestObject result = jsonMergePatchService.applyPatch(original, patch, TestObject.class);

            // Then
            assertEquals("updated", result.getName());
            assertEquals(100, result.getValue()); // Unchanged
        }

        @Test
        @DisplayName("should delete property when patch contains null value")
        void applyPatch_shouldDeleteProperty_whenNull() throws Exception {
            // Given
            TestObject original = new TestObject("original", 100);
            JsonNode patch = objectMapper.readTree("{\"name\": null}");

            // When
            TestObject result = jsonMergePatchService.applyPatch(original, patch, TestObject.class);

            // Then
            assertNull(result.getName());
            assertEquals(100, result.getValue()); // Unchanged
        }

        @Test
        @DisplayName("should add new property when patch contains new field")
        void applyPatch_shouldAddNewProperty() throws Exception {
            // Given
            Map<String, Object> original = new HashMap<>();
            original.put("name", "original");
            JsonNode patch = objectMapper.readTree("{\"newField\": \"newValue\"}");

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = jsonMergePatchService.applyPatch(original, patch, Map.class);

            // Then
            assertEquals("original", result.get("name"));
            assertEquals("newValue", result.get("newField"));
        }

        @Test
        @DisplayName("should merge nested objects recursively")
        void applyPatch_shouldMergeNestedObjects() throws Exception {
            // Given
            JsonNode original = objectMapper.readTree("{\"outer\": {\"inner1\": \"value1\", \"inner2\": \"value2\"}}");
            JsonNode patch = objectMapper.readTree("{\"outer\": {\"inner1\": \"updated\"}}");

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = jsonMergePatchService.applyPatch(
                    objectMapper.convertValue(original, Map.class), patch, Map.class);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> outer = (Map<String, Object>) result.get("outer");
            assertEquals("updated", outer.get("inner1"));
            assertEquals("value2", outer.get("inner2")); // Unchanged
        }

        @Test
        @DisplayName("should replace array entirely (not merge)")
        void applyPatch_shouldReplaceArrayEntirely() throws Exception {
            // Given
            JsonNode original = objectMapper.readTree("{\"items\": [1, 2, 3]}");
            JsonNode patch = objectMapper.readTree("{\"items\": [4, 5]}");

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = jsonMergePatchService.applyPatch(
                    objectMapper.convertValue(original, Map.class), patch, Map.class);

            // Then
            @SuppressWarnings("unchecked")
            java.util.List<Integer> items = (java.util.List<Integer>) result.get("items");
            assertEquals(2, items.size());
            assertEquals(4, items.get(0));
            assertEquals(5, items.get(1));
        }

        @Test
        @DisplayName("should handle empty patch")
        void applyPatch_shouldHandleEmptyPatch() throws Exception {
            // Given
            TestObject original = new TestObject("original", 100);
            JsonNode patch = objectMapper.readTree("{}");

            // When
            TestObject result = jsonMergePatchService.applyPatch(original, patch, TestObject.class);

            // Then
            assertEquals("original", result.getName());
            assertEquals(100, result.getValue());
        }

        @Test
        @DisplayName("should throw exception for null original")
        void applyPatch_shouldThrowForNullOriginal() throws Exception {
            // Given
            JsonNode patch = objectMapper.readTree("{\"name\": \"value\"}");

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> jsonMergePatchService.applyPatch(null, patch, TestObject.class));
        }
    }

    @Nested
    @DisplayName("Collection Patch Tests")
    class CollectionPatchTests {

        @Test
        @DisplayName("should update collection title")
        void applyCollectionPatch_shouldUpdateTitle() throws Exception {
            // Given
            Collection original = TestFixtures.createTestCollection("test-collection");
            JsonNode patch = objectMapper.readTree("{\"title\": \"Updated Title\"}");

            // When
            Collection result = jsonMergePatchService.applyCollectionPatch(original, patch);

            // Then
            assertEquals("Updated Title", result.getTitle());
            assertEquals("test-collection", result.getId()); // Preserved
            assertEquals("Collection", result.getType()); // Preserved
        }

        @Test
        @DisplayName("should update collection description")
        void applyCollectionPatch_shouldUpdateDescription() throws Exception {
            // Given
            Collection original = TestFixtures.createTestCollection("test-collection");
            JsonNode patch = objectMapper.readTree("{\"description\": \"New description\"}");

            // When
            Collection result = jsonMergePatchService.applyCollectionPatch(original, patch);

            // Then
            assertEquals("New description", result.getDescription());
        }

        @Test
        @DisplayName("should preserve collection ID even if patch tries to change it")
        void applyCollectionPatch_shouldPreserveId() throws Exception {
            // Given
            Collection original = TestFixtures.createTestCollection("original-id");
            JsonNode patch = objectMapper.readTree("{\"id\": \"new-id\"}");

            // When
            Collection result = jsonMergePatchService.applyCollectionPatch(original, patch);

            // Then
            assertEquals("original-id", result.getId()); // ID should be preserved
        }

        @Test
        @DisplayName("should throw exception for null collection")
        void applyCollectionPatch_shouldThrowForNullCollection() throws Exception {
            // Given
            JsonNode patch = objectMapper.readTree("{\"title\": \"value\"}");

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> jsonMergePatchService.applyCollectionPatch(null, patch));
        }
    }

    @Nested
    @DisplayName("Item Patch Tests")
    class ItemPatchTests {

        @Test
        @DisplayName("should update item properties")
        void applyItemPatch_shouldUpdateProperties() throws Exception {
            // Given
            Item original = TestFixtures.createTestItem("test-item", "test-collection");
            JsonNode patch = objectMapper.readTree("{\"properties\": {\"title\": \"Updated Title\"}}");

            // When
            Item result = jsonMergePatchService.applyItemPatch(original, patch);

            // Then
            assertEquals("Updated Title", result.getProperties().get("title"));
            assertEquals("test-item", result.getId()); // Preserved
        }

        @Test
        @DisplayName("should preserve item ID even if patch tries to change it")
        void applyItemPatch_shouldPreserveId() throws Exception {
            // Given
            Item original = TestFixtures.createTestItem("original-id", "test-collection");
            JsonNode patch = objectMapper.readTree("{\"id\": \"new-id\"}");

            // When
            Item result = jsonMergePatchService.applyItemPatch(original, patch);

            // Then
            assertEquals("original-id", result.getId()); // ID should be preserved
        }

        @Test
        @DisplayName("should preserve item collection reference even if patch tries to change it")
        void applyItemPatch_shouldPreserveCollection() throws Exception {
            // Given
            Item original = TestFixtures.createTestItem("test-item", "original-collection");
            JsonNode patch = objectMapper.readTree("{\"collection\": \"new-collection\"}");

            // When
            Item result = jsonMergePatchService.applyItemPatch(original, patch);

            // Then
            assertEquals("original-collection", result.getCollection()); // Collection should be preserved
        }

        @Test
        @DisplayName("should preserve item type as Feature")
        void applyItemPatch_shouldPreserveType() throws Exception {
            // Given
            Item original = TestFixtures.createTestItem("test-item", "test-collection");
            JsonNode patch = objectMapper.readTree("{\"type\": \"NotAFeature\"}");

            // When
            Item result = jsonMergePatchService.applyItemPatch(original, patch);

            // Then
            assertEquals("Feature", result.getType()); // Type should be preserved
        }

        @Test
        @DisplayName("should throw exception for null item")
        void applyItemPatch_shouldThrowForNullItem() throws Exception {
            // Given
            JsonNode patch = objectMapper.readTree("{\"properties\": {\"title\": \"value\"}}");

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> jsonMergePatchService.applyItemPatch(null, patch));
        }
    }

    /**
     * Simple test object for generic patch tests.
     */
    static class TestObject {
        private String name;
        private int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
