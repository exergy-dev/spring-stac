package org.stac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.stac.exception.ConflictException;
import org.stac.exception.NotFoundException;
import org.stac.model.*;
import org.stac.repository.StacRepository;
import org.stac.util.TestFixtures;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for StacService.
 */
@ExtendWith(MockitoExtension.class)
class StacServiceTest {

    @Mock
    private StacRepository stacRepository;

    @Mock
    private Cql2FilterParser cql2FilterParser;

    @Mock
    private ValidationService validationService;

    @Mock
    private JsonMergePatchService jsonMergePatchService;

    @Mock
    private LinkBuilder linkBuilder;

    @InjectMocks
    private StacService stacService;

    private static final String ROOT_URL = "http://localhost:8080";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 10000;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stacService, "rootUrl", ROOT_URL);
        ReflectionTestUtils.setField(stacService, "defaultLimit", DEFAULT_LIMIT);
        ReflectionTestUtils.setField(stacService, "maxLimit", MAX_LIMIT);
        ReflectionTestUtils.setField(stacService, "apiTitle", "STAC API");
        ReflectionTestUtils.setField(stacService, "apiDescription", "A STAC API implementation");
        ReflectionTestUtils.setField(stacService, "apiId", "stac-api");

        // Setup LinkBuilder to return the same object with links added (lenient to avoid unnecessary stubbing errors)
        lenient().when(linkBuilder.addCollectionLinks(any(Collection.class))).thenAnswer(invocation -> {
            Collection col = invocation.getArgument(0);
            col.setLinks(List.of(
                    Link.builder().href(ROOT_URL + "/collections/" + col.getId()).rel("self").type("application/json").build(),
                    Link.builder().href(ROOT_URL).rel("root").type("application/json").build(),
                    Link.builder().href(ROOT_URL).rel("parent").type("application/json").build(),
                    Link.builder().href(ROOT_URL + "/collections/" + col.getId() + "/items").rel("items").type("application/geo+json").build()
            ));
            return col;
        });
        lenient().when(linkBuilder.addItemLinks(any(Item.class), anyString())).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            String collectionId = invocation.getArgument(1);
            item.setLinks(List.of(
                    Link.builder().href(ROOT_URL + "/collections/" + collectionId + "/items/" + item.getId()).rel("self").type("application/geo+json").build(),
                    Link.builder().href(ROOT_URL).rel("root").type("application/json").build(),
                    Link.builder().href(ROOT_URL + "/collections/" + collectionId).rel("parent").type("application/json").build(),
                    Link.builder().href(ROOT_URL + "/collections/" + collectionId).rel("collection").type("application/json").build()
            ));
            return item;
        });
        lenient().when(linkBuilder.buildCollectionsLinks(any(), anyInt())).thenReturn(List.of(
                Link.builder().href(ROOT_URL + "/collections").rel("self").type("application/json").build(),
                Link.builder().href(ROOT_URL).rel("root").type("application/json").build()
        ));
        lenient().when(linkBuilder.buildItemCollectionLinks(anyString(), any(), anyInt())).thenReturn(List.of(
                Link.builder().href(ROOT_URL + "/collections/test-collection/items").rel("self").type("application/geo+json").build(),
                Link.builder().href(ROOT_URL).rel("root").type("application/json").build()
        ));
        lenient().when(linkBuilder.buildSearchResultLinks(any(), any())).thenReturn(List.of(
                Link.builder().href(ROOT_URL + "/search").rel("self").type("application/geo+json").build(),
                Link.builder().href(ROOT_URL).rel("root").type("application/json").build()
        ));
        lenient().when(linkBuilder.buildAggregationLinks()).thenReturn(List.of(
                Link.builder().href(ROOT_URL + "/aggregate").rel("self").type("application/json").build(),
                Link.builder().href(ROOT_URL).rel("root").type("application/json").build()
        ));
    }

    @Nested
    @DisplayName("Landing Page Tests")
    class LandingPageTests {

        @Test
        @DisplayName("should return landing page with all required links")
        void shouldReturnLandingPage() {
            // When
            LandingPage landingPage = stacService.getLandingPage();

            // Then
            assertNotNull(landingPage);
            assertEquals("Catalog", landingPage.getType());
            assertEquals("1.0.0", landingPage.getStacVersion());
            assertEquals("stac-api", landingPage.getId());
            assertEquals("STAC API", landingPage.getTitle());
            assertEquals("A STAC API implementation", landingPage.getDescription());

            // Verify links
            assertNotNull(landingPage.getLinks());
            assertFalse(landingPage.getLinks().isEmpty());

            // Check for required links
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "self"));
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "root"));
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "service-desc"));
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "conformance"));
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "data"));
            assertTrue(hasLinkWithRel(landingPage.getLinks(), "search"));

            // Verify conformance classes
            assertNotNull(landingPage.getConformsTo());
            assertFalse(landingPage.getConformsTo().isEmpty());
            assertTrue(landingPage.getConformsTo().contains("https://api.stacspec.org/v1.0.0/core"));
        }

        @Test
        @DisplayName("should return conformance with all implemented classes")
        void shouldReturnConformance() {
            // When
            Conformance conformance = stacService.getConformance();

            // Then
            assertNotNull(conformance);
            assertNotNull(conformance.getConformsTo());
            assertFalse(conformance.getConformsTo().isEmpty());

            // Verify key conformance classes
            List<String> conformsTo = conformance.getConformsTo();
            assertTrue(conformsTo.contains("https://api.stacspec.org/v1.0.0/core"));
            assertTrue(conformsTo.contains("https://api.stacspec.org/v1.0.0/item-search"));
            assertTrue(conformsTo.contains("https://api.stacspec.org/v1.0.0/item-search#filter"));
            assertTrue(conformsTo.contains("http://www.opengis.net/spec/cql2/1.0/conf/cql2-text"));
            assertTrue(conformsTo.contains("http://www.opengis.net/spec/cql2/1.0/conf/cql2-json"));
        }
    }

    @Nested
    @DisplayName("Collection Tests")
    class CollectionTests {

        @Test
        @DisplayName("should get collections with pagination")
        void shouldGetCollections() {
            // Given
            Collection collection1 = TestFixtures.createTestCollection("collection-1");
            Collection collection2 = TestFixtures.createTestCollection("collection-2");
            Page<Collection> page = new PageImpl<>(List.of(collection1, collection2));

            when(stacRepository.findAllCollections(any(Pageable.class))).thenReturn(page);

            // When
            Collections result = stacService.getCollections(10);

            // Then
            assertNotNull(result);
            assertNotNull(result.getCollections());
            assertEquals(2, result.getCollections().size());
            assertNotNull(result.getLinks());
            assertTrue(hasLinkWithRel(result.getLinks(), "self"));

            verify(stacRepository).findAllCollections(any(Pageable.class));
        }

        @Test
        @DisplayName("should get collection by ID")
        void shouldGetCollectionById() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));

            // When
            Collection result = stacService.getCollection(collectionId);

            // Then
            assertNotNull(result);
            assertEquals(collectionId, result.getId());
            assertNotNull(result.getLinks());
            assertTrue(hasLinkWithRel(result.getLinks(), "self"));
            assertTrue(hasLinkWithRel(result.getLinks(), "items"));

            verify(stacRepository).findCollectionById(collectionId);
        }

        @Test
        @DisplayName("should throw NotFoundException for missing collection")
        void shouldThrowNotFoundForMissingCollection() {
            // Given
            String collectionId = "non-existent";

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.empty());

            // When & Then
            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> stacService.getCollection(collectionId));

            assertEquals("Collection", exception.getResourceType());
            verify(stacRepository).findCollectionById(collectionId);
        }
    }

    @Nested
    @DisplayName("Item Tests")
    class ItemTests {

        @Test
        @DisplayName("should get items from collection")
        void shouldGetItems() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            Item item1 = TestFixtures.createTestItem("item-1", collectionId);
            Item item2 = TestFixtures.createTestItem("item-2", collectionId);
            Page<Item> page = new PageImpl<>(List.of(item1, item2));

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));
            when(stacRepository.findItemsByCollection(eq(collectionId), any(), any(Pageable.class)))
                    .thenReturn(page);

            // When
            ItemCollection result = stacService.getItems(collectionId, null);

            // Then
            assertNotNull(result);
            assertEquals("FeatureCollection", result.getType());
            assertNotNull(result.getFeatures());
            assertEquals(2, result.getFeatures().size());
            assertNotNull(result.getContext());
            assertEquals(2, result.getContext().getReturned());

            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository).findItemsByCollection(eq(collectionId), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("should get item by ID")
        void shouldGetItemById() {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            when(stacRepository.findItemById(collectionId, itemId)).thenReturn(Optional.of(item));

            // When
            Item result = stacService.getItem(collectionId, itemId);

            // Then
            assertNotNull(result);
            assertEquals(itemId, result.getId());
            assertEquals(collectionId, result.getCollection());
            assertNotNull(result.getLinks());
            assertTrue(hasLinkWithRel(result.getLinks(), "self"));
            assertTrue(hasLinkWithRel(result.getLinks(), "collection"));

            verify(stacRepository).findItemById(collectionId, itemId);
        }

        @Test
        @DisplayName("should throw NotFoundException for missing item")
        void shouldThrowNotFoundForMissingItem() {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";

            when(stacRepository.findItemById(collectionId, itemId)).thenReturn(Optional.empty());

            // When & Then
            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> stacService.getItem(collectionId, itemId));

            assertEquals("Item", exception.getResourceType());
            verify(stacRepository).findItemById(collectionId, itemId);
        }
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("should search items")
        void shouldSearchItems() {
            // Given
            SearchRequest request = SearchRequest.builder()
                    .collections(List.of("collection-1"))
                    .limit(10)
                    .build();

            ItemCollection searchResult = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of(
                            TestFixtures.createTestItem("item-1", "collection-1"),
                            TestFixtures.createTestItem("item-2", "collection-1")
                    ))
                    .context(Context.builder().returned(2).matched(2).limit(10).build())
                    .build();

            when(stacRepository.search(any(SearchCriteria.class))).thenReturn(searchResult);

            // When
            ItemCollection result = stacService.search(request);

            // Then
            assertNotNull(result);
            assertEquals("FeatureCollection", result.getType());
            assertNotNull(result.getFeatures());
            assertEquals(2, result.getFeatures().size());
            assertNotNull(result.getLinks());
            assertNotNull(result.getTimeStamp());

            verify(validationService).validateSearchRequest(request);
            verify(validationService).validateLimit(request.getLimit(), MAX_LIMIT);
            verify(stacRepository).search(any(SearchCriteria.class));
        }

        @Test
        @DisplayName("should build search criteria from request")
        void shouldBuildSearchCriteriaFromRequest() {
            // Given
            SearchRequest request = SearchRequest.builder()
                    .collections(List.of("collection-1", "collection-2"))
                    .ids(List.of("item-1", "item-2"))
                    .bbox("-180,-90,180,90")
                    .datetime("2020-01-01T00:00:00Z/2020-12-31T23:59:59Z")
                    .limit(50)
                    .sortby(List.of("-datetime", "+id"))
                    .fields("id,properties.datetime")
                    .build();

            // When
            SearchCriteria criteria = stacService.buildSearchCriteria(request);

            // Then
            assertNotNull(criteria);
            assertEquals(List.of("collection-1", "collection-2"), criteria.getCollections());
            assertEquals(List.of("item-1", "item-2"), criteria.getIds());
            assertNotNull(criteria.getBbox());
            assertNotNull(criteria.getDatetime());
            assertEquals(50, criteria.getLimit());
            assertNotNull(criteria.getSortBy());
            assertEquals(2, criteria.getSortBy().size());
            assertNotNull(criteria.getFields());
        }

        @Test
        @DisplayName("should search with CQL2 filter")
        void shouldSearchWithCql2Filter() throws Exception {
            // Given
            ObjectMapper mapper = new ObjectMapper();
            JsonNode filter = mapper.readTree("{\"op\": \"=\", \"args\": [{\"property\": \"platform\"}, \"sentinel-2\"]}");

            SearchRequest request = SearchRequest.builder()
                    .filter(filter)
                    .filterLang("cql2-json")
                    .limit(10)
                    .build();

            FilterCriteria filterCriteria = FilterCriteria.builder()
                    .type(FilterType.COMPARISON)
                    .operator("=")
                    .propertyName("platform")
                    .value("sentinel-2")
                    .build();

            ItemCollection searchResult = ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(List.of())
                    .context(Context.builder().returned(0).matched(0).limit(10).build())
                    .build();

            when(cql2FilterParser.parseCql2Json(filter)).thenReturn(filterCriteria);
            when(stacRepository.search(any(SearchCriteria.class))).thenReturn(searchResult);

            // When
            ItemCollection result = stacService.search(request);

            // Then
            assertNotNull(result);
            verify(cql2FilterParser).parseCql2Json(filter);
            verify(stacRepository).search(any(SearchCriteria.class));
        }
    }

    @Nested
    @DisplayName("Transaction Tests")
    class TransactionTests {

        @Test
        @DisplayName("should create item")
        void shouldCreateItem() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            Item item = TestFixtures.createTestItem("new-item", collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));
            when(stacRepository.saveItem(eq(collectionId), any(Item.class))).thenReturn(item);

            // When
            Item result = stacService.createItem(collectionId, item);

            // Then
            assertNotNull(result);
            assertEquals("new-item", result.getId());
            assertEquals(collectionId, result.getCollection());
            assertNotNull(result.getLinks());
            assertTrue(hasLinkWithRel(result.getLinks(), "self"));

            verify(stacRepository).findCollectionById(collectionId);
            verify(validationService).validateItem(item);
            verify(stacRepository).saveItem(eq(collectionId), any(Item.class));
        }

        @Test
        @DisplayName("should validate item before create")
        void shouldValidateItemBeforeCreate() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            Item item = TestFixtures.createTestItem("new-item", collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));
            when(stacRepository.saveItem(eq(collectionId), any(Item.class))).thenReturn(item);

            // When
            stacService.createItem(collectionId, item);

            // Then
            verify(validationService).validateItem(item);
        }

        @Test
        @DisplayName("should throw NotFoundException when creating item in non-existent collection")
        void shouldThrowNotFoundWhenCreatingItemInMissingCollection() {
            // Given
            String collectionId = "non-existent";
            Item item = TestFixtures.createTestItem("new-item", collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> stacService.createItem(collectionId, item));

            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository, never()).saveItem(anyString(), any(Item.class));
        }

        @Test
        @DisplayName("should delete item")
        void shouldDeleteItem() {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Item item = TestFixtures.createTestItem(itemId, collectionId);

            when(stacRepository.findItemById(collectionId, itemId)).thenReturn(Optional.of(item));

            // When
            stacService.deleteItem(collectionId, itemId);

            // Then
            verify(stacRepository).findItemById(collectionId, itemId);
            verify(stacRepository).deleteItem(collectionId, itemId);
        }

        @Test
        @DisplayName("should throw NotFoundException when deleting non-existent item")
        void shouldThrowNotFoundWhenDeletingMissingItem() {
            // Given
            String collectionId = "test-collection";
            String itemId = "non-existent";

            when(stacRepository.findItemById(collectionId, itemId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> stacService.deleteItem(collectionId, itemId));

            verify(stacRepository).findItemById(collectionId, itemId);
            verify(stacRepository, never()).deleteItem(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Collection CRUD Tests")
    class CollectionCrudTests {

        @Test
        @DisplayName("should create collection")
        void createCollection_shouldValidateAndSave() {
            // Given
            Collection collection = TestFixtures.createTestCollection("new-collection");

            when(stacRepository.findCollectionById("new-collection")).thenReturn(Optional.empty());
            when(stacRepository.saveCollection(collection)).thenReturn(collection);

            // When
            Collection result = stacService.createCollection(collection);

            // Then
            assertNotNull(result);
            assertEquals("new-collection", result.getId());
            verify(stacRepository).findCollectionById("new-collection");
            verify(validationService).validateCollection(collection);
            verify(stacRepository).saveCollection(collection);
        }

        @Test
        @DisplayName("should throw ConflictException when collection already exists")
        void createCollection_shouldThrowConflict_whenExists() {
            // Given
            Collection collection = TestFixtures.createTestCollection("existing-collection");

            when(stacRepository.findCollectionById("existing-collection"))
                    .thenReturn(Optional.of(collection));

            // When & Then
            assertThrows(ConflictException.class,
                    () -> stacService.createCollection(collection));

            verify(stacRepository).findCollectionById("existing-collection");
            verify(stacRepository, never()).saveCollection(any());
        }

        @Test
        @DisplayName("should update collection")
        void updateCollection_shouldVerifyExistsThenSave() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));
            when(stacRepository.saveCollection(collection)).thenReturn(collection);

            // When
            Collection result = stacService.updateCollection(collectionId, collection);

            // Then
            assertNotNull(result);
            assertEquals(collectionId, result.getId());
            verify(stacRepository).findCollectionById(collectionId);
            verify(validationService).validateCollection(collection);
            verify(stacRepository).saveCollection(collection);
        }

        @Test
        @DisplayName("should throw NotFoundException when updating non-existent collection")
        void updateCollection_shouldThrowNotFound_whenMissing() {
            // Given
            String collectionId = "non-existent";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> stacService.updateCollection(collectionId, collection));

            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository, never()).saveCollection(any());
        }

        @Test
        @DisplayName("should delete collection")
        void deleteCollection_shouldVerifyExistsThenDelete() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));

            // When
            stacService.deleteCollection(collectionId);

            // Then
            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository).deleteCollection(collectionId);
        }

        @Test
        @DisplayName("should throw NotFoundException when deleting non-existent collection")
        void deleteCollection_shouldThrowNotFound_whenMissing() {
            // Given
            String collectionId = "non-existent";

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> stacService.deleteCollection(collectionId));

            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository, never()).deleteCollection(anyString());
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        @Test
        @DisplayName("should patch collection")
        void patchCollection_shouldApplyPatchAndValidate() throws Exception {
            // Given
            String collectionId = "test-collection";
            Collection original = TestFixtures.createTestCollection(collectionId);
            Collection patched = TestFixtures.createTestCollection(collectionId);
            patched.setTitle("Patched Title");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode patch = mapper.readTree("{\"title\": \"Patched Title\"}");

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(original));
            when(jsonMergePatchService.applyCollectionPatch(original, patch)).thenReturn(patched);
            when(stacRepository.saveCollection(patched)).thenReturn(patched);

            // When
            Collection result = stacService.patchCollection(collectionId, patch);

            // Then
            assertNotNull(result);
            assertEquals("Patched Title", result.getTitle());
            verify(stacRepository).findCollectionById(collectionId);
            verify(jsonMergePatchService).applyCollectionPatch(original, patch);
            verify(validationService).validateCollection(patched);
            verify(stacRepository).saveCollection(patched);
        }

        @Test
        @DisplayName("should patch item")
        void patchItem_shouldApplyPatchAndValidate() throws Exception {
            // Given
            String collectionId = "test-collection";
            String itemId = "test-item";
            Collection collection = TestFixtures.createTestCollection(collectionId);
            Item original = TestFixtures.createTestItem(itemId, collectionId);
            Item patched = TestFixtures.createTestItem(itemId, collectionId);
            patched.getProperties().put("title", "Patched Title");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode patch = mapper.readTree("{\"properties\": {\"title\": \"Patched Title\"}}");

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));
            when(stacRepository.findItemById(collectionId, itemId)).thenReturn(Optional.of(original));
            when(jsonMergePatchService.applyItemPatch(original, patch)).thenReturn(patched);
            when(stacRepository.saveItem(collectionId, patched)).thenReturn(patched);

            // When
            Item result = stacService.patchItem(collectionId, itemId, patch);

            // Then
            assertNotNull(result);
            assertEquals("Patched Title", result.getProperties().get("title"));
            verify(stacRepository).findCollectionById(collectionId);
            verify(stacRepository).findItemById(collectionId, itemId);
            verify(jsonMergePatchService).applyItemPatch(original, patch);
            verify(validationService).validateItem(patched);
            verify(stacRepository).saveItem(collectionId, patched);
        }
    }

    @Nested
    @DisplayName("Queryables Tests")
    class QueryablesTests {

        @Test
        @DisplayName("should return global queryables")
        void getQueryables_shouldReturnGlobalSchema() {
            // When
            Queryables result = stacService.getQueryables();

            // Then
            assertNotNull(result);
            assertEquals(ROOT_URL + "/queryables", result.getId());
            assertEquals("STAC API Queryables", result.getTitle());
            assertNotNull(result.getProperties());
            assertTrue(result.getProperties().containsKey("id"));
            assertTrue(result.getProperties().containsKey("datetime"));
            assertTrue(result.getProperties().containsKey("geometry"));
        }

        @Test
        @DisplayName("should return collection queryables")
        void getQueryables_shouldReturnCollectionSchema() {
            // Given
            String collectionId = "test-collection";
            Collection collection = TestFixtures.createTestCollection(collectionId);

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.of(collection));

            // When
            Queryables result = stacService.getQueryables(collectionId);

            // Then
            assertNotNull(result);
            assertEquals(ROOT_URL + "/collections/" + collectionId + "/queryables", result.getId());
            assertNotNull(result.getProperties());
            assertTrue(result.getProperties().containsKey("id"));

            verify(stacRepository).findCollectionById(collectionId);
        }

        @Test
        @DisplayName("should throw NotFoundException for missing collection")
        void getQueryables_shouldThrowNotFound_whenCollectionMissing() {
            // Given
            String collectionId = "non-existent";

            when(stacRepository.findCollectionById(collectionId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> stacService.getQueryables(collectionId));

            verify(stacRepository).findCollectionById(collectionId);
        }
    }

    private boolean hasLinkWithRel(List<Link> links, String rel) {
        return links != null && links.stream()
                .anyMatch(link -> rel.equals(link.getRel()));
    }
}
