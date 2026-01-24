package org.stac.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.stac.model.BoundingBox;
import org.stac.model.BulkOperationResult;
import org.stac.model.Collection;
import org.stac.model.FilterCriteria;
import org.stac.model.FilterType;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.SearchCriteria;
import org.stac.model.TemporalExtent;
import org.stac.util.TestFixtures;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Elasticsearch repository implementation.
 * <p>
 * Uses TestContainers to spin up an Elasticsearch instance for testing.
 * Tests cover all major STAC repository operations including CRUD,
 * spatial queries, temporal filtering, and pagination.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("elasticsearch")
@Import(ElasticsearchTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ElasticsearchRepositoryIntegrationTest {

    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.11.4";

    @Container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
        registry.add("stac.repository.type", () -> "elasticsearch");
    }

    @Autowired
    private StacRepository stacRepository;

    @Autowired
    private ElasticsearchTestConfiguration testConfiguration;

    @BeforeEach
    void setUp() throws IOException {
        // Clear existing data and ensure indices have proper mappings
        testConfiguration.clearAllData();

        // Create the items index template
        testConfiguration.createItemsIndexTemplate();

        // Allow time for Elasticsearch to process
        waitForElasticsearch();
    }

    /**
     * Wait for Elasticsearch to be ready and indices to be refreshed.
     */
    private void waitForElasticsearch() {
        try {
            Thread.sleep(500);
            testConfiguration.refreshIndices();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Ignore refresh errors in setup
        }
    }

    /**
     * Refresh indices to ensure documents are searchable.
     */
    private void refreshAndWait() {
        try {
            testConfiguration.refreshIndices();
            Thread.sleep(100);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    // ========================================================================
    // Collection Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve a collection")
    void shouldSaveAndRetrieveCollection() {
        // Given
        Collection collection = TestFixtures.createTestCollection("test-collection-1");

        // When
        Collection savedCollection = stacRepository.saveCollection(collection);
        refreshAndWait();

        Optional<Collection> retrievedCollection = stacRepository.findCollectionById("test-collection-1");

        // Then
        assertNotNull(savedCollection);
        assertEquals("test-collection-1", savedCollection.getId());

        assertTrue(retrievedCollection.isPresent());
        assertEquals("test-collection-1", retrievedCollection.get().getId());
        assertEquals(collection.getTitle(), retrievedCollection.get().getTitle());
        assertEquals(collection.getDescription(), retrievedCollection.get().getDescription());
        assertEquals(collection.getLicense(), retrievedCollection.get().getLicense());
    }

    // ========================================================================
    // Item Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve an item")
    void shouldSaveAndRetrieveItem() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("collection-for-item");
        stacRepository.saveCollection(collection);

        // Create the items index for this collection
        testConfiguration.createItemsIndex("collection-for-item");
        refreshAndWait();

        Item item = TestFixtures.createTestItem("test-item-1", "collection-for-item");

        // When
        Item savedItem = stacRepository.saveItem("collection-for-item", item);
        refreshAndWait();

        Optional<Item> retrievedItem = stacRepository.findItemById("collection-for-item", "test-item-1");

        // Then
        assertNotNull(savedItem);
        assertEquals("test-item-1", savedItem.getId());
        assertEquals("collection-for-item", savedItem.getCollection());

        assertTrue(retrievedItem.isPresent());
        assertEquals("test-item-1", retrievedItem.get().getId());
        assertEquals("collection-for-item", retrievedItem.get().getCollection());
        assertNotNull(retrievedItem.get().getGeometry());
        assertNotNull(retrievedItem.get().getProperties());
    }

    // ========================================================================
    // Search Tests
    // ========================================================================

    @Test
    @DisplayName("Should search items by bounding box")
    void shouldSearchItemsByBbox() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("bbox-test-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("bbox-test-collection");
        refreshAndWait();

        // Create items at different locations
        Item itemInBbox = TestFixtures.createTestItem("item-in-bbox", "bbox-test-collection", -10.0, 40.0);
        Item itemOutsideBbox = TestFixtures.createTestItem("item-outside", "bbox-test-collection", -150.0, 60.0);

        stacRepository.saveItem("bbox-test-collection", itemInBbox);
        stacRepository.saveItem("bbox-test-collection", itemOutsideBbox);
        refreshAndWait();

        // When - search with bbox that includes the first item
        BoundingBox bbox = BoundingBox.builder()
                .west(-20.0)
                .south(30.0)
                .east(0.0)
                .north(50.0)
                .build();

        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of("bbox-test-collection"))
                .bbox(bbox)
                .limit(10)
                .build();

        ItemCollection result = stacRepository.search(criteria);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFeatures());
        assertEquals(1, result.getFeatures().size());
        assertEquals("item-in-bbox", result.getFeatures().get(0).getId());
    }

    @Test
    @DisplayName("Should search items by datetime")
    void shouldSearchItemsByDatetime() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("datetime-test-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("datetime-test-collection");
        refreshAndWait();

        // Create items with different datetimes
        Item item2020 = TestFixtures.createTestItemWithDatetime("item-2020", "datetime-test-collection",
                "2020-06-15T10:00:00Z");
        Item item2022 = TestFixtures.createTestItemWithDatetime("item-2022", "datetime-test-collection",
                "2022-06-15T10:00:00Z");
        Item item2024 = TestFixtures.createTestItemWithDatetime("item-2024", "datetime-test-collection",
                "2024-06-15T10:00:00Z");

        stacRepository.saveItem("datetime-test-collection", item2020);
        stacRepository.saveItem("datetime-test-collection", item2022);
        stacRepository.saveItem("datetime-test-collection", item2024);
        refreshAndWait();

        // When - search for items in 2021-2023
        TemporalExtent temporalExtent = TemporalExtent.builder()
                .interval(List.of(List.of("2021-01-01T00:00:00Z", "2023-12-31T23:59:59Z")))
                .build();

        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of("datetime-test-collection"))
                .datetime(temporalExtent)
                .limit(10)
                .build();

        ItemCollection result = stacRepository.search(criteria);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFeatures());
        assertEquals(1, result.getFeatures().size());
        assertEquals("item-2022", result.getFeatures().get(0).getId());
    }

    @Test
    @DisplayName("Should search items by collection")
    void shouldSearchItemsByCollection() throws IOException {
        // Given
        Collection collection1 = TestFixtures.createTestCollection("search-collection-1");
        Collection collection2 = TestFixtures.createTestCollection("search-collection-2");
        stacRepository.saveCollection(collection1);
        stacRepository.saveCollection(collection2);
        testConfiguration.createItemsIndex("search-collection-1");
        testConfiguration.createItemsIndex("search-collection-2");
        refreshAndWait();

        // Create items in different collections
        Item item1 = TestFixtures.createTestItem("item-in-coll-1", "search-collection-1");
        Item item2 = TestFixtures.createTestItem("item-in-coll-2", "search-collection-2");
        Item item3 = TestFixtures.createTestItem("another-item-in-coll-1", "search-collection-1");

        stacRepository.saveItem("search-collection-1", item1);
        stacRepository.saveItem("search-collection-2", item2);
        stacRepository.saveItem("search-collection-1", item3);
        refreshAndWait();

        // When - search only in collection 1
        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of("search-collection-1"))
                .limit(10)
                .build();

        ItemCollection result = stacRepository.search(criteria);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFeatures());
        assertEquals(2, result.getFeatures().size());
        assertTrue(result.getFeatures().stream()
                .allMatch(item -> "search-collection-1".equals(item.getCollection())));
    }

    @Test
    @DisplayName("Should search items with CQL2 filter")
    void shouldSearchWithCql2Filter() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("cql2-test-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("cql2-test-collection");
        refreshAndWait();

        // Create items with different platform values
        Item sentinelItem = TestFixtures.createTestItemWithProperty("sentinel-item", "cql2-test-collection",
                "platform", "sentinel-2");
        Item landsatItem = TestFixtures.createTestItemWithProperty("landsat-item", "cql2-test-collection",
                "platform", "landsat-8");

        stacRepository.saveItem("cql2-test-collection", sentinelItem);
        stacRepository.saveItem("cql2-test-collection", landsatItem);
        refreshAndWait();

        // When - search with CQL2 filter for sentinel platform
        FilterCriteria filter = FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator("=")
                .propertyName("properties.platform")
                .value("sentinel-2")
                .build();

        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of("cql2-test-collection"))
                .filter(filter)
                .limit(10)
                .build();

        ItemCollection result = stacRepository.search(criteria);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFeatures());
        assertEquals(1, result.getFeatures().size());
        assertEquals("sentinel-item", result.getFeatures().get(0).getId());
        assertEquals("sentinel-2", result.getFeatures().get(0).getProperties().get("platform"));
    }

    // ========================================================================
    // Delete Tests
    // ========================================================================

    @Test
    @DisplayName("Should delete an item")
    void shouldDeleteItem() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("delete-test-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("delete-test-collection");
        refreshAndWait();

        Item item = TestFixtures.createTestItem("item-to-delete", "delete-test-collection");
        stacRepository.saveItem("delete-test-collection", item);
        refreshAndWait();

        // Verify item exists
        Optional<Item> beforeDelete = stacRepository.findItemById("delete-test-collection", "item-to-delete");
        assertTrue(beforeDelete.isPresent());

        // When
        stacRepository.deleteItem("delete-test-collection", "item-to-delete");
        refreshAndWait();

        // Then
        Optional<Item> afterDelete = stacRepository.findItemById("delete-test-collection", "item-to-delete");
        assertFalse(afterDelete.isPresent());
    }

    // ========================================================================
    // Bulk Operations Tests
    // ========================================================================

    @Test
    @DisplayName("Should bulk insert items")
    void shouldBulkInsertItems() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("bulk-insert-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("bulk-insert-collection");
        refreshAndWait();

        List<Item> items = Arrays.asList(
                TestFixtures.createTestItem("bulk-item-1", "bulk-insert-collection"),
                TestFixtures.createTestItem("bulk-item-2", "bulk-insert-collection"),
                TestFixtures.createTestItem("bulk-item-3", "bulk-insert-collection"),
                TestFixtures.createTestItem("bulk-item-4", "bulk-insert-collection"),
                TestFixtures.createTestItem("bulk-item-5", "bulk-insert-collection")
        );

        // When
        BulkOperationResult result = stacRepository.bulkInsert("bulk-insert-collection", items);
        refreshAndWait();

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSuccess());
        assertEquals(0, result.getFailed());
        assertTrue(result.isFullySuccessful());

        // Verify all items were inserted
        long count = stacRepository.countItemsByCollection("bulk-insert-collection");
        assertEquals(5, count);
    }

    // ========================================================================
    // Pagination Tests
    // ========================================================================

    @Test
    @DisplayName("Should paginate results")
    void shouldPaginateResults() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("pagination-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("pagination-collection");
        refreshAndWait();

        // Create 15 items
        for (int i = 1; i <= 15; i++) {
            Item item = TestFixtures.createTestItem("paginated-item-" + i, "pagination-collection");
            stacRepository.saveItem("pagination-collection", item);
        }
        refreshAndWait();

        // When - first page
        SearchCriteria firstPageCriteria = SearchCriteria.builder()
                .collections(List.of("pagination-collection"))
                .limit(5)
                .build();

        ItemCollection firstPage = stacRepository.search(firstPageCriteria);

        // Then
        assertNotNull(firstPage);
        assertNotNull(firstPage.getFeatures());
        assertEquals(5, firstPage.getFeatures().size());

        // Verify context if available
        if (firstPage.getContext() != null) {
            assertTrue(firstPage.getContext().getMatched() >= 15);
        }

        // When - verify we can get more items with different criteria
        SearchCriteria allItemsCriteria = SearchCriteria.builder()
                .collections(List.of("pagination-collection"))
                .limit(20)
                .build();

        ItemCollection allItems = stacRepository.search(allItemsCriteria);

        // Then
        assertNotNull(allItems);
        assertNotNull(allItems.getFeatures());
        assertEquals(15, allItems.getFeatures().size());
    }

    @Test
    @DisplayName("Should paginate collection items using Page")
    void shouldPaginateCollectionItems() throws IOException {
        // Given
        Collection collection = TestFixtures.createTestCollection("page-pagination-collection");
        stacRepository.saveCollection(collection);
        testConfiguration.createItemsIndex("page-pagination-collection");
        refreshAndWait();

        // Create 10 items
        for (int i = 1; i <= 10; i++) {
            Item item = TestFixtures.createTestItem("page-item-" + i, "page-pagination-collection");
            stacRepository.saveItem("page-pagination-collection", item);
        }
        refreshAndWait();

        // When - get first page
        Pageable firstPageable = PageRequest.of(0, 3);
        Page<Item> firstPage = stacRepository.findItemsByCollection("page-pagination-collection", null, firstPageable);

        // Then
        assertNotNull(firstPage);
        assertEquals(3, firstPage.getContent().size());
        assertEquals(10, firstPage.getTotalElements());
        assertEquals(4, firstPage.getTotalPages());
        assertTrue(firstPage.hasNext());
        assertFalse(firstPage.hasPrevious());

        // When - get second page
        Pageable secondPageable = PageRequest.of(1, 3);
        Page<Item> secondPage = stacRepository.findItemsByCollection("page-pagination-collection", null, secondPageable);

        // Then
        assertNotNull(secondPage);
        assertEquals(3, secondPage.getContent().size());
        assertTrue(secondPage.hasNext());
        assertTrue(secondPage.hasPrevious());

        // When - get last page
        Pageable lastPageable = PageRequest.of(3, 3);
        Page<Item> lastPage = stacRepository.findItemsByCollection("page-pagination-collection", null, lastPageable);

        // Then
        assertNotNull(lastPage);
        assertEquals(1, lastPage.getContent().size()); // Only 1 item on last page (10 % 3 = 1)
        assertFalse(lastPage.hasNext());
        assertTrue(lastPage.hasPrevious());
    }
}
