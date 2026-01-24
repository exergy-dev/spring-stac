package org.stac.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.stac.model.BulkOperationResult;
import org.stac.model.Collection;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.SearchCriteria;
import org.stac.util.TestFixtures;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for PgStacRepositoryImpl using the official pgSTAC Docker container.
 * Tests STAC repository operations against a real PostgreSQL/PostGIS database with pgSTAC installed.
 *
 * @see <a href="https://github.com/stac-utils/pgstac">pgSTAC GitHub Repository</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("pgstac")
class PgStacRepositoryIntegrationTest {

    /**
     * Official pgSTAC container from GitHub Container Registry.
     * This container includes PostgreSQL, PostGIS, and all pgSTAC functions pre-installed.
     */
    @Container
    static GenericContainer<?> pgstac = new GenericContainer<>(
            DockerImageName.parse("ghcr.io/stac-utils/pgstac:v0.9.9"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_USER", "username")
            .withEnv("POSTGRES_PASSWORD", "password")
            .withEnv("POSTGRES_DB", "postgis")
            .withEnv("PGUSER", "username")
            .withEnv("PGPASSWORD", "password")
            .withEnv("PGDATABASE", "postgis")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
            String.format("jdbc:postgresql://%s:%d/postgis",
                pgstac.getHost(), pgstac.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "username");
        registry.add("spring.datasource.password", () -> "password");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("stac.repository.type", () -> "pgstac");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.security.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () ->
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration");
    }

    @Autowired
    private StacRepository stacRepository;

    private static final String TEST_COLLECTION_ID = "test-collection";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        cleanupTestData();
    }

    private void cleanupTestData() {
        // Clean up test collections (items are deleted via CASCADE)
        for (String collId : List.of(TEST_COLLECTION_ID, "collection-1", "collection-2", "bulk-test-collection")) {
            try {
                stacRepository.deleteCollection(collId);
            } catch (Exception ignored) {
                // Ignore if not found
            }
        }
    }

    // ========================================================================
    // Collection Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve a collection")
    void shouldSaveAndRetrieveCollection() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);

        // When
        Collection saved = stacRepository.saveCollection(collection);

        // Then
        assertNotNull(saved);
        assertEquals(TEST_COLLECTION_ID, saved.getId());

        // Verify retrieval
        Optional<Collection> retrieved = stacRepository.findCollectionById(TEST_COLLECTION_ID);
        assertTrue(retrieved.isPresent());
        assertEquals(TEST_COLLECTION_ID, retrieved.get().getId());
        assertEquals(collection.getTitle(), retrieved.get().getTitle());
        assertEquals(collection.getDescription(), retrieved.get().getDescription());
    }

    @Test
    @DisplayName("Should count collections correctly")
    void shouldCountCollections() {
        // Given
        long initialCount = stacRepository.countCollections();
        Collection collection1 = TestFixtures.createTestCollection("collection-1");
        Collection collection2 = TestFixtures.createTestCollection("collection-2");

        // When
        stacRepository.saveCollection(collection1);
        stacRepository.saveCollection(collection2);

        // Then
        long newCount = stacRepository.countCollections();
        assertEquals(initialCount + 2, newCount);

        // Cleanup
        stacRepository.deleteCollection("collection-1");
        stacRepository.deleteCollection("collection-2");
    }

    // ========================================================================
    // Item Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve an item")
    void shouldSaveAndRetrieveItem() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item = TestFixtures.createTestItem("item-1", TEST_COLLECTION_ID);

        // When
        Item saved = stacRepository.saveItem(TEST_COLLECTION_ID, item);

        // Then
        assertNotNull(saved);
        assertEquals("item-1", saved.getId());
        assertEquals(TEST_COLLECTION_ID, saved.getCollection());

        // Verify retrieval
        Optional<Item> retrieved = stacRepository.findItemById(TEST_COLLECTION_ID, "item-1");
        assertTrue(retrieved.isPresent());
        assertEquals("item-1", retrieved.get().getId());
        assertEquals(TEST_COLLECTION_ID, retrieved.get().getCollection());
        assertNotNull(retrieved.get().getGeometry());
        assertNotNull(retrieved.get().getProperties());
    }

    @Test
    @DisplayName("Should save and retrieve an item with polygon geometry")
    void shouldSaveAndRetrieveItemWithPolygonGeometry() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item = TestFixtures.createTestItemWithPolygon("polygon-item-1", TEST_COLLECTION_ID);

        // When
        Item saved = stacRepository.saveItem(TEST_COLLECTION_ID, item);

        // Then
        assertNotNull(saved);

        Optional<Item> retrieved = stacRepository.findItemById(TEST_COLLECTION_ID, "polygon-item-1");
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getGeometry());
        assertEquals("Polygon", retrieved.get().getGeometry().getGeometryType());
    }

    @Test
    @DisplayName("Should delete an item")
    void shouldDeleteItem() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item = TestFixtures.createTestItem("item-to-delete", TEST_COLLECTION_ID);
        stacRepository.saveItem(TEST_COLLECTION_ID, item);

        // Verify item exists
        assertTrue(stacRepository.findItemById(TEST_COLLECTION_ID, "item-to-delete").isPresent());

        // When
        stacRepository.deleteItem(TEST_COLLECTION_ID, "item-to-delete");

        // Then
        Optional<Item> deleted = stacRepository.findItemById(TEST_COLLECTION_ID, "item-to-delete");
        assertFalse(deleted.isPresent());
    }

    @Test
    @DisplayName("Should count items by collection")
    void shouldCountItemsByCollection() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item1 = TestFixtures.createTestItem("count-item-1", TEST_COLLECTION_ID);
        Item item2 = TestFixtures.createTestItem("count-item-2", TEST_COLLECTION_ID);
        Item item3 = TestFixtures.createTestItem("count-item-3", TEST_COLLECTION_ID);

        // When
        stacRepository.saveItem(TEST_COLLECTION_ID, item1);
        stacRepository.saveItem(TEST_COLLECTION_ID, item2);
        stacRepository.saveItem(TEST_COLLECTION_ID, item3);

        // Then
        long count = stacRepository.countItemsByCollection(TEST_COLLECTION_ID);
        assertEquals(3, count);
    }

    // ========================================================================
    // Search Tests
    // ========================================================================

    @Test
    @DisplayName("Should search items by bounding box")
    void shouldSearchItemsByBbox() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        // Create items at different locations
        Item itemInBbox = TestFixtures.createTestItem("item-in-bbox", TEST_COLLECTION_ID, -122.0, 37.5);
        Item itemOutsideBbox = TestFixtures.createTestItem("item-outside-bbox", TEST_COLLECTION_ID, -100.0, 30.0);

        stacRepository.saveItem(TEST_COLLECTION_ID, itemInBbox);
        stacRepository.saveItem(TEST_COLLECTION_ID, itemOutsideBbox);

        // When - search within San Francisco area
        SearchCriteria criteria = TestFixtures.createSearchCriteriaWithBbox(-123.0, 37.0, -121.0, 38.0);
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertEquals(1, results.getFeatures().size());
        assertEquals("item-in-bbox", results.getFeatures().get(0).getId());
    }

    @Test
    @DisplayName("Should search items by datetime range")
    void shouldSearchItemsByDatetime() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        // Create items with different datetimes
        String pastDatetime = Instant.now().minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String recentDatetime = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String futureDatetime = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();

        Item pastItem = TestFixtures.createTestItemWithDatetime("past-item", TEST_COLLECTION_ID, pastDatetime);
        Item recentItem = TestFixtures.createTestItemWithDatetime("recent-item", TEST_COLLECTION_ID, recentDatetime);
        Item futureItem = TestFixtures.createTestItemWithDatetime("future-item", TEST_COLLECTION_ID, futureDatetime);

        stacRepository.saveItem(TEST_COLLECTION_ID, pastItem);
        stacRepository.saveItem(TEST_COLLECTION_ID, recentItem);
        stacRepository.saveItem(TEST_COLLECTION_ID, futureItem);

        // When - search for items in the last 7 days
        String start = Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String end = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        SearchCriteria criteria = TestFixtures.createSearchCriteriaWithDatetime(start, end);
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertEquals(1, results.getFeatures().size());
        assertEquals("recent-item", results.getFeatures().get(0).getId());
    }

    @Test
    @DisplayName("Should search items by collection")
    void shouldSearchItemsByCollection() {
        // Given
        Collection collection1 = TestFixtures.createTestCollection("collection-1");
        Collection collection2 = TestFixtures.createTestCollection("collection-2");
        stacRepository.saveCollection(collection1);
        stacRepository.saveCollection(collection2);

        Item item1 = TestFixtures.createTestItem("item-in-col1", "collection-1");
        Item item2 = TestFixtures.createTestItem("item-in-col2", "collection-2");
        Item item3 = TestFixtures.createTestItem("another-item-in-col1", "collection-1");

        stacRepository.saveItem("collection-1", item1);
        stacRepository.saveItem("collection-2", item2);
        stacRepository.saveItem("collection-1", item3);

        // When - search only in collection-1
        SearchCriteria criteria = TestFixtures.createSearchCriteriaWithCollections("collection-1");
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertEquals(2, results.getFeatures().size());

        // Verify all results are from collection-1
        for (Item item : results.getFeatures()) {
            assertEquals("collection-1", item.getCollection());
        }
    }

    @Test
    @DisplayName("Should return empty results when no items match search criteria")
    void shouldReturnEmptyResultsWhenNoMatch() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item = TestFixtures.createTestItem("item-1", TEST_COLLECTION_ID, -122.0, 37.5);
        stacRepository.saveItem(TEST_COLLECTION_ID, item);

        // When - search in an area with no items
        SearchCriteria criteria = TestFixtures.createSearchCriteriaWithBbox(0.0, 0.0, 10.0, 10.0);
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertTrue(results.getFeatures().isEmpty());
    }

    @Test
    @DisplayName("Should search with default criteria and return all items")
    void shouldSearchWithDefaultCriteria() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        Item item1 = TestFixtures.createTestItem("search-item-1", TEST_COLLECTION_ID);
        Item item2 = TestFixtures.createTestItem("search-item-2", TEST_COLLECTION_ID);

        stacRepository.saveItem(TEST_COLLECTION_ID, item1);
        stacRepository.saveItem(TEST_COLLECTION_ID, item2);

        // When - search with default criteria (no filters)
        SearchCriteria criteria = TestFixtures.createTestSearchCriteria();
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertTrue(results.getFeatures().size() >= 2);
    }

    // ========================================================================
    // Bulk Operation Tests
    // ========================================================================

    @Test
    @DisplayName("Should bulk insert items successfully")
    void shouldBulkInsertItems() {
        // Given
        Collection collection = TestFixtures.createTestCollection("bulk-test-collection");
        stacRepository.saveCollection(collection);

        List<Item> items = List.of(
                TestFixtures.createTestItem("bulk-item-1", "bulk-test-collection"),
                TestFixtures.createTestItem("bulk-item-2", "bulk-test-collection"),
                TestFixtures.createTestItem("bulk-item-3", "bulk-test-collection"),
                TestFixtures.createTestItem("bulk-item-4", "bulk-test-collection"),
                TestFixtures.createTestItem("bulk-item-5", "bulk-test-collection")
        );

        // When
        BulkOperationResult result = stacRepository.bulkInsert("bulk-test-collection", items);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSuccess());
        assertEquals(0, result.getFailed());
        assertTrue(result.isFullySuccessful());
        assertEquals(5, result.getItems().size());

        // Verify items were actually inserted
        long count = stacRepository.countItemsByCollection("bulk-test-collection");
        assertEquals(5, count);
    }

    @Test
    @DisplayName("Should return empty result for bulk insert with empty list")
    void shouldReturnEmptyResultForEmptyBulkInsert() {
        // Given
        Collection collection = TestFixtures.createTestCollection("bulk-test-collection");
        stacRepository.saveCollection(collection);

        // When
        BulkOperationResult result = stacRepository.bulkInsert("bulk-test-collection", List.of());

        // Then
        assertNotNull(result);
        assertEquals(0, result.getSuccess());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getTotal());
    }

    // ========================================================================
    // Context and Pagination Tests
    // ========================================================================

    @Test
    @DisplayName("Should include context information in search results")
    void shouldIncludeContextInSearchResults() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        for (int i = 1; i <= 15; i++) {
            Item item = TestFixtures.createTestItem("context-item-" + i, TEST_COLLECTION_ID);
            stacRepository.saveItem(TEST_COLLECTION_ID, item);
        }

        // When
        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of(TEST_COLLECTION_ID))
                .limit(5)
                .build();
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getContext());
        assertEquals(5, results.getContext().getReturned());
        assertEquals(5, results.getContext().getLimit());
        assertEquals(15, results.getContext().getMatched());

        // Also verify OGC-style fields
        assertEquals(15, results.getNumberMatched());
        assertEquals(5, results.getNumberReturned());
    }

    @Test
    @DisplayName("Should respect limit in search criteria")
    void shouldRespectLimitInSearchCriteria() {
        // Given
        Collection collection = TestFixtures.createTestCollection(TEST_COLLECTION_ID);
        stacRepository.saveCollection(collection);

        for (int i = 1; i <= 10; i++) {
            Item item = TestFixtures.createTestItem("limit-item-" + i, TEST_COLLECTION_ID);
            stacRepository.saveItem(TEST_COLLECTION_ID, item);
        }

        // When
        SearchCriteria criteria = SearchCriteria.builder()
                .collections(List.of(TEST_COLLECTION_ID))
                .limit(3)
                .build();
        ItemCollection results = stacRepository.search(criteria);

        // Then
        assertNotNull(results);
        assertNotNull(results.getFeatures());
        assertEquals(3, results.getFeatures().size());
    }
}
