package org.stac.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.stac.exception.ConflictException;
import org.stac.exception.NotFoundException;
import org.stac.model.*;
import org.stac.model.AggregationResponse;
import org.stac.repository.StacRepository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main service layer for STAC API operations.
 * <p>
 * Provides business logic for all STAC API endpoints including
 * landing page, conformance, collections, items, and search operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StacService {

    private final StacRepository stacRepository;
    private final Cql2FilterParser cql2FilterParser;
    private final ValidationService validationService;
    private final JsonMergePatchService jsonMergePatchService;

    @Value("${stac.api.root-url:http://localhost:8080}")
    private String rootUrl;

    @Value("${stac.api.default-limit:10}")
    private int defaultLimit;

    @Value("${stac.api.max-limit:10000}")
    private int maxLimit;

    @Value("${stac.api.title:STAC API}")
    private String apiTitle;

    @Value("${stac.api.description:A STAC API implementation}")
    private String apiDescription;

    @Value("${stac.api.id:stac-api}")
    private String apiId;

    /**
     * Standard STAC API conformance classes.
     */
    private static final List<String> CONFORMANCE_CLASSES = List.of(
            // STAC API
            "https://api.stacspec.org/v1.0.0/core",
            "https://api.stacspec.org/v1.0.0/item-search",
            "https://api.stacspec.org/v1.0.0/item-search#filter",
            "https://api.stacspec.org/v1.0.0/item-search#query",
            "https://api.stacspec.org/v1.0.0/item-search#sort",
            "https://api.stacspec.org/v1.0.0/item-search#fields",
            "https://api.stacspec.org/v1.0.0/item-search#context",
            "https://api.stacspec.org/v1.0.0/ogcapi-features",
            "https://api.stacspec.org/v1.0.0/ogcapi-features#transaction",
            // Aggregation Extension
            "https://api.stacspec.org/v0.3.0/aggregation",
            // OGC API - Features
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30",
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson",
            // CQL2
            "http://www.opengis.net/spec/cql2/1.0/conf/cql2-text",
            "http://www.opengis.net/spec/cql2/1.0/conf/cql2-json",
            "http://www.opengis.net/spec/cql2/1.0/conf/basic-cql2",
            "http://www.opengis.net/spec/cql2/1.0/conf/advanced-comparison-operators",
            "http://www.opengis.net/spec/cql2/1.0/conf/basic-spatial-operators",
            "http://www.opengis.net/spec/cql2/1.0/conf/temporal-operators"
    );

    // ========================================================================
    // Landing Page and Conformance
    // ========================================================================

    /**
     * Returns the STAC API landing page.
     *
     * @return the landing page with links to API resources
     */
    public LandingPage getLandingPage() {
        log.debug("Getting landing page");

        List<Link> links = new ArrayList<>();

        // Self link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("self")
                .type("application/json")
                .title("This catalog")
                .build());

        // Root link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Service description (OpenAPI)
        links.add(Link.builder()
                .href(rootUrl + "/api")
                .rel("service-desc")
                .type("application/vnd.oai.openapi+json;version=3.0")
                .title("OpenAPI definition")
                .build());

        // Service documentation
        links.add(Link.builder()
                .href(rootUrl + "/api.html")
                .rel("service-doc")
                .type("text/html")
                .title("API documentation")
                .build());

        // Conformance
        links.add(Link.builder()
                .href(rootUrl + "/conformance")
                .rel("conformance")
                .type("application/json")
                .title("Conformance classes")
                .build());

        // Collections (data)
        links.add(Link.builder()
                .href(rootUrl + "/collections")
                .rel("data")
                .type("application/json")
                .title("Collections")
                .build());

        // Search
        links.add(Link.builder()
                .href(rootUrl + "/search")
                .rel("search")
                .type("application/geo+json")
                .title("Search")
                .method("GET")
                .build());

        links.add(Link.builder()
                .href(rootUrl + "/search")
                .rel("search")
                .type("application/geo+json")
                .title("Search")
                .method("POST")
                .build());

        // Aggregate (STAC Aggregation Extension)
        links.add(Link.builder()
                .href(rootUrl + "/aggregate")
                .rel("aggregate")
                .type("application/json")
                .title("Aggregate")
                .method("GET")
                .build());

        links.add(Link.builder()
                .href(rootUrl + "/aggregate")
                .rel("aggregate")
                .type("application/json")
                .title("Aggregate")
                .method("POST")
                .build());

        return LandingPage.builder()
                .id(apiId)
                .type("Catalog")
                .stacVersion("1.0.0")
                .title(apiTitle)
                .description(apiDescription)
                .conformsTo(CONFORMANCE_CLASSES)
                .links(links)
                .build();
    }

    /**
     * Returns the conformance declaration.
     *
     * @return conformance with all implemented conformance class URIs
     */
    public Conformance getConformance() {
        log.debug("Getting conformance");
        return Conformance.builder()
                .conformsTo(CONFORMANCE_CLASSES)
                .build();
    }

    // ========================================================================
    // Collection Operations
    // ========================================================================

    /**
     * Returns all collections with pagination.
     *
     * @param limit maximum number of collections to return
     * @return Collections response with collection list and links
     */
    public Collections getCollections(Integer limit) {
        log.debug("Getting collections with limit: {}", limit);

        int effectiveLimit = limit != null ? Math.min(limit, maxLimit) : defaultLimit;
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        Page<Collection> page = stacRepository.findAllCollections(pageable);

        List<Collection> collections = page.getContent().stream()
                .map(this::addCollectionLinks)
                .collect(Collectors.toList());

        List<Link> links = new ArrayList<>();
        links.add(Link.builder()
                .href(rootUrl + "/collections")
                .rel("self")
                .type("application/json")
                .title("Collections")
                .build());

        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Pagination links
        if (page.hasNext()) {
            links.add(Link.builder()
                    .href(rootUrl + "/collections?limit=" + effectiveLimit + "&offset=" + (page.getNumber() + 1) * effectiveLimit)
                    .rel("next")
                    .type("application/json")
                    .title("Next page")
                    .build());
        }

        return Collections.builder()
                .collections(collections)
                .links(links)
                .build();
    }

    /**
     * Returns a specific collection by ID.
     *
     * @param id the collection identifier
     * @return the Collection with links
     * @throws NotFoundException if the collection does not exist
     */
    @Cacheable(value = "collections", key = "#id")
    public Collection getCollection(String id) {
        log.debug("Getting collection: {}", id);

        return stacRepository.findCollectionById(id)
                .map(this::addCollectionLinks)
                .orElseThrow(() -> NotFoundException.collection(id));
    }

    /**
     * Creates a new collection.
     *
     * @param collection the collection to create
     * @return the created Collection with links
     * @throws ConflictException if a collection with the same ID already exists
     */
    public Collection createCollection(Collection collection) {
        log.debug("Creating collection: {}", collection.getId());

        // Check if collection already exists
        if (stacRepository.findCollectionById(collection.getId()).isPresent()) {
            throw ConflictException.collection(collection.getId());
        }

        // Validate collection
        validationService.validateCollection(collection);

        // Save and return with links
        Collection savedCollection = stacRepository.saveCollection(collection);
        return addCollectionLinks(savedCollection);
    }

    /**
     * Updates an existing collection.
     *
     * @param id         the collection identifier
     * @param collection the updated collection data
     * @return the updated Collection with links
     * @throws NotFoundException if the collection does not exist
     */
    public Collection updateCollection(String id, Collection collection) {
        log.debug("Updating collection: {}", id);

        // Verify collection exists
        if (stacRepository.findCollectionById(id).isEmpty()) {
            throw NotFoundException.collection(id);
        }

        // Validate collection
        validationService.validateCollection(collection);

        // Ensure ID matches
        collection.setId(id);

        // Save and return with links
        Collection savedCollection = stacRepository.saveCollection(collection);
        return addCollectionLinks(savedCollection);
    }

    /**
     * Deletes a collection.
     *
     * @param id the collection identifier
     * @throws NotFoundException if the collection does not exist
     */
    public void deleteCollection(String id) {
        log.debug("Deleting collection: {}", id);

        // Verify collection exists
        if (stacRepository.findCollectionById(id).isEmpty()) {
            throw NotFoundException.collection(id);
        }

        stacRepository.deleteCollection(id);
    }

    /**
     * Applies a JSON Merge Patch to a collection.
     *
     * @param id    the collection identifier
     * @param patch the JSON Merge Patch to apply
     * @return the patched Collection with links
     * @throws NotFoundException if the collection does not exist
     */
    public Collection patchCollection(String id, JsonNode patch) {
        log.debug("Patching collection: {}", id);

        // Get existing collection
        Collection existing = stacRepository.findCollectionById(id)
                .orElseThrow(() -> NotFoundException.collection(id));

        // Apply patch
        Collection patched = jsonMergePatchService.applyCollectionPatch(existing, patch);

        // Validate patched collection
        validationService.validateCollection(patched);

        // Save and return with links
        Collection savedCollection = stacRepository.saveCollection(patched);
        return addCollectionLinks(savedCollection);
    }

    // ========================================================================
    // Item Operations
    // ========================================================================

    /**
     * Returns items within a collection with optional filtering.
     *
     * @param collectionId the collection identifier
     * @param criteria     optional query criteria for filtering
     * @return ItemCollection containing matching items
     * @throws NotFoundException if the collection does not exist
     */
    public ItemCollection getItems(String collectionId, ItemQueryCriteria criteria) {
        log.debug("Getting items for collection: {} with criteria: {}", collectionId, criteria);

        // Verify collection exists
        if (stacRepository.findCollectionById(collectionId).isEmpty()) {
            throw NotFoundException.collection(collectionId);
        }

        int effectiveLimit = criteria != null && criteria.getLimit() != null
                ? Math.min(criteria.getLimit(), maxLimit)
                : defaultLimit;

        Pageable pageable = PageRequest.of(0, effectiveLimit);
        Page<Item> page = stacRepository.findItemsByCollection(collectionId, criteria, pageable);

        List<Item> items = page.getContent().stream()
                .map(item -> addItemLinks(item, collectionId))
                .collect(Collectors.toList());

        List<Link> links = buildItemCollectionLinks(collectionId, page, effectiveLimit);

        return ItemCollection.builder()
                .type("FeatureCollection")
                .features(items)
                .links(links)
                .context(Context.builder()
                        .returned(items.size())
                        .limit(effectiveLimit)
                        .matched((int) page.getTotalElements())
                        .build())
                .numberMatched((int) page.getTotalElements())
                .numberReturned(items.size())
                .timeStamp(Instant.now().toString())
                .build();
    }

    /**
     * Returns a specific item by collection and item ID.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @return the Item with links
     * @throws NotFoundException if the item or collection does not exist
     */
    public Item getItem(String collectionId, String itemId) {
        log.debug("Getting item: {} from collection: {}", itemId, collectionId);

        return stacRepository.findItemById(collectionId, itemId)
                .map(item -> addItemLinks(item, collectionId))
                .orElseThrow(() -> NotFoundException.item(collectionId, itemId));
    }

    // ========================================================================
    // Search Operations
    // ========================================================================

    /**
     * Performs a cross-collection search.
     *
     * @param request the search request parameters
     * @return ItemCollection containing matching items
     */
    public ItemCollection search(SearchRequest request) {
        log.debug("Executing search with request: {}", request);

        // Validate the search request
        validationService.validateSearchRequest(request);
        validationService.validateLimit(request.getLimit(), maxLimit);

        // Build internal search criteria
        SearchCriteria criteria = buildSearchCriteria(request);

        // Execute search
        ItemCollection result = stacRepository.search(criteria);

        // Add links to items
        if (result.getFeatures() != null) {
            List<Item> itemsWithLinks = result.getFeatures().stream()
                    .map(item -> addItemLinks(item, item.getCollection()))
                    .collect(Collectors.toList());
            result.setFeatures(itemsWithLinks);
        }

        // Add search result links
        result.setLinks(buildSearchResultLinks(request, result));
        result.setTimeStamp(Instant.now().toString());

        return result;
    }

    // ========================================================================
    // Aggregation Operations
    // ========================================================================

    /**
     * Performs aggregation queries on STAC items.
     *
     * @param request the aggregation request
     * @return AggregationResponse containing aggregation results
     */
    public AggregationResponse aggregate(AggregationRequest request) {
        log.debug("Executing aggregation with request: {}", request);

        if (request == null || request.getAggregationCount() == 0) {
            return AggregationResponse.empty();
        }

        // Get raw aggregation results from repository
        Map<String, Object> rawResults = stacRepository.aggregate(request);

        // Convert to AggregationResponse format
        List<AggregationResponse.Aggregation> aggregations = new ArrayList<>();

        for (String field : request.getAggregations()) {
            Object result = rawResults.get(field);
            AggregationResponse.Aggregation agg = buildAggregation(field, result);
            if (agg != null) {
                aggregations.add(agg);
            }
        }

        // Build links
        List<Link> links = new ArrayList<>();
        links.add(Link.builder()
                .href(rootUrl + "/aggregate")
                .rel("self")
                .type("application/json")
                .title("Aggregation results")
                .build());
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        return AggregationResponse.builder()
                .aggregations(aggregations)
                .links(links)
                .build();
    }

    /**
     * Builds an Aggregation object from raw repository results.
     */
    @SuppressWarnings("unchecked")
    private AggregationResponse.Aggregation buildAggregation(String field, Object result) {
        if (result == null) {
            return null;
        }

        AggregationResponse.Aggregation.AggregationBuilder builder =
                AggregationResponse.Aggregation.builder().name(field);

        if (result instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) result;

            // Handle different aggregation result types
            if (resultMap.containsKey("buckets")) {
                // Term aggregation
                builder.dataType("string");
                List<Map<String, Object>> rawBuckets = (List<Map<String, Object>>) resultMap.get("buckets");
                List<AggregationResponse.Bucket> buckets = rawBuckets.stream()
                        .map(b -> AggregationResponse.Bucket.builder()
                                .key(b.get("key"))
                                .frequency(((Number) b.getOrDefault("count", b.getOrDefault("doc_count", 0))).longValue())
                                .count(((Number) b.getOrDefault("count", b.getOrDefault("doc_count", 0))).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.buckets(buckets);
            } else if (resultMap.containsKey("min") || resultMap.containsKey("max")) {
                // Numeric statistics
                builder.dataType("numeric");
                if (resultMap.containsKey("min")) {
                    builder.minimum((Number) resultMap.get("min"));
                }
                if (resultMap.containsKey("max")) {
                    builder.maximum((Number) resultMap.get("max"));
                }
                if (resultMap.containsKey("avg")) {
                    builder.average((Number) resultMap.get("avg"));
                }
                if (resultMap.containsKey("sum")) {
                    builder.sum((Number) resultMap.get("sum"));
                }
                if (resultMap.containsKey("count")) {
                    builder.count(((Number) resultMap.get("count")).longValue());
                }
            } else if (resultMap.containsKey("bounds")) {
                // Geometry bounds
                builder.dataType("geometry");
                Object boundsObj = resultMap.get("bounds");
                if (boundsObj instanceof double[]) {
                    builder.bounds((double[]) boundsObj);
                } else if (boundsObj instanceof List) {
                    List<Number> boundsList = (List<Number>) boundsObj;
                    builder.bounds(boundsList.stream().mapToDouble(Number::doubleValue).toArray());
                }
            }
        } else if (result instanceof List) {
            // Bucket list directly
            builder.dataType("string");
            List<Map<String, Object>> rawBuckets = (List<Map<String, Object>>) result;
            List<AggregationResponse.Bucket> buckets = rawBuckets.stream()
                    .map(b -> AggregationResponse.Bucket.builder()
                            .key(b.get("key"))
                            .frequency(((Number) b.getOrDefault("count", 0)).longValue())
                            .count(((Number) b.getOrDefault("count", 0)).longValue())
                            .build())
                    .collect(Collectors.toList());
            builder.buckets(buckets);
        } else if (result instanceof Number) {
            // Simple count
            builder.dataType("numeric");
            builder.count(((Number) result).longValue());
        }

        return builder.build();
    }

    // ========================================================================
    // Transaction Operations
    // ========================================================================

    /**
     * Creates a new item in a collection.
     *
     * @param collectionId the collection identifier
     * @param item         the item to create
     * @return the created Item with links
     * @throws NotFoundException if the collection does not exist
     */
    public Item createItem(String collectionId, Item item) {
        log.debug("Creating item in collection: {}", collectionId);

        // Verify collection exists
        if (stacRepository.findCollectionById(collectionId).isEmpty()) {
            throw NotFoundException.collection(collectionId);
        }

        // Validate item
        validationService.validateItem(item);

        // Set collection reference
        item.setCollection(collectionId);

        // Save and return with links
        Item savedItem = stacRepository.saveItem(collectionId, item);
        return addItemLinks(savedItem, collectionId);
    }

    /**
     * Updates an existing item in a collection.
     *
     * @param collectionId the collection identifier
     * @param item         the item to update
     * @return the updated Item with links
     * @throws NotFoundException if the item or collection does not exist
     */
    public Item updateItem(String collectionId, Item item) {
        log.debug("Updating item: {} in collection: {}", item.getId(), collectionId);

        // Verify collection exists
        if (stacRepository.findCollectionById(collectionId).isEmpty()) {
            throw NotFoundException.collection(collectionId);
        }

        // Verify item exists
        if (stacRepository.findItemById(collectionId, item.getId()).isEmpty()) {
            throw NotFoundException.item(collectionId, item.getId());
        }

        // Validate item
        validationService.validateItem(item);

        // Set collection reference
        item.setCollection(collectionId);

        // Save and return with links
        Item savedItem = stacRepository.saveItem(collectionId, item);
        return addItemLinks(savedItem, collectionId);
    }

    /**
     * Deletes an item from a collection.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @throws NotFoundException if the item or collection does not exist
     */
    public void deleteItem(String collectionId, String itemId) {
        log.debug("Deleting item: {} from collection: {}", itemId, collectionId);

        // Verify item exists (will throw if not found)
        stacRepository.findItemById(collectionId, itemId)
                .orElseThrow(() -> NotFoundException.item(collectionId, itemId));

        stacRepository.deleteItem(collectionId, itemId);
    }

    /**
     * Applies a JSON Merge Patch to an item.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @param patch        the JSON Merge Patch to apply
     * @return the patched Item with links
     * @throws NotFoundException if the item or collection does not exist
     */
    public Item patchItem(String collectionId, String itemId, JsonNode patch) {
        log.debug("Patching item: {} in collection: {}", itemId, collectionId);

        // Verify collection exists
        if (stacRepository.findCollectionById(collectionId).isEmpty()) {
            throw NotFoundException.collection(collectionId);
        }

        // Get existing item
        Item existing = stacRepository.findItemById(collectionId, itemId)
                .orElseThrow(() -> NotFoundException.item(collectionId, itemId));

        // Apply patch
        Item patched = jsonMergePatchService.applyItemPatch(existing, patch);

        // Validate patched item
        validationService.validateItem(patched);

        // Save and return with links
        Item savedItem = stacRepository.saveItem(collectionId, patched);
        return addItemLinks(savedItem, collectionId);
    }

    /**
     * Performs bulk operations on items in a collection.
     * <p>
     * Supports three operation methods:
     * <ul>
     *   <li><b>insert</b> - Insert new items; fails for items that already exist</li>
     *   <li><b>upsert</b> - Insert new items or update existing ones</li>
     *   <li><b>delete</b> - Delete items by their IDs</li>
     * </ul>
     *
     * @param collectionId the collection identifier
     * @param items        the list of items to process
     * @param method       the operation method ("insert", "upsert", or "delete")
     * @return BulkOperationResult containing success/failure counts and messages
     * @throws NotFoundException if the collection does not exist
     */
    public BulkOperationResult bulkItems(String collectionId, List<Item> items, String method) {
        log.debug("Performing bulk {} operation on {} items in collection: {}",
                method, items != null ? items.size() : 0, collectionId);

        // Verify collection exists
        if (stacRepository.findCollectionById(collectionId).isEmpty()) {
            throw NotFoundException.collection(collectionId);
        }

        if (items == null || items.isEmpty()) {
            return BulkOperationResult.empty();
        }

        // Set collection reference on all items
        items.forEach(item -> item.setCollection(collectionId));

        // Validate items (skip validation for delete operations which only need IDs)
        if (!"delete".equalsIgnoreCase(method)) {
            for (Item item : items) {
                validationService.validateItem(item);
            }
        }

        // Delegate to appropriate repository method based on operation type
        String effectiveMethod = method != null ? method.toLowerCase() : "upsert";

        return switch (effectiveMethod) {
            case "insert" -> stacRepository.bulkInsert(collectionId, items);
            case "delete" -> {
                List<String> itemIds = items.stream()
                        .map(Item::getId)
                        .collect(Collectors.toList());
                yield stacRepository.bulkDelete(collectionId, itemIds);
            }
            default -> stacRepository.bulkUpdate(collectionId, items); // "upsert" or default
        };
    }

    // ========================================================================
    // Queryables Operations
    // ========================================================================

    /**
     * Returns the global queryables for all collections.
     *
     * @return Queryables schema with standard properties
     */
    public Queryables getQueryables() {
        log.debug("Getting global queryables");

        return Queryables.withStandardProperties(
                rootUrl + "/queryables",
                "STAC API Queryables"
        );
    }

    /**
     * Returns the queryables for a specific collection.
     *
     * @param collectionId the collection identifier
     * @return Queryables schema for the collection
     * @throws NotFoundException if the collection does not exist
     */
    public Queryables getQueryables(String collectionId) {
        log.debug("Getting queryables for collection: {}", collectionId);

        // Verify collection exists
        Collection collection = stacRepository.findCollectionById(collectionId)
                .orElseThrow(() -> NotFoundException.collection(collectionId));

        Queryables queryables = Queryables.withStandardProperties(
                rootUrl + "/collections/" + collectionId + "/queryables",
                collection.getTitle() != null ? collection.getTitle() + " Queryables" : collectionId + " Queryables"
        );

        // Add collection-specific queryables from summaries if available
        if (collection.getSummaries() != null) {
            for (String key : collection.getSummaries().keySet()) {
                if (!queryables.getProperties().containsKey(key)) {
                    queryables.addProperty(key, QueryableProperty.builder()
                            .title(key)
                            .description("Property from collection summaries")
                            .build());
                }
            }
        }

        return queryables;
    }

    // ========================================================================
    // Internal Helper Methods
    // ========================================================================

    /**
     * Builds SearchCriteria from a SearchRequest DTO.
     *
     * @param request the incoming search request
     * @return internal SearchCriteria for repository operations
     */
    SearchCriteria buildSearchCriteria(SearchRequest request) {
        SearchCriteria.SearchCriteriaBuilder builder = SearchCriteria.builder();

        // Collections
        if (request.getCollections() != null && !request.getCollections().isEmpty()) {
            builder.collections(request.getCollections());
        }

        // Item IDs
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            builder.ids(request.getIds());
        }

        // Bbox
        if (request.getBbox() != null && !request.getBbox().isBlank()) {
            builder.bbox(BoundingBox.fromString(request.getBbox()));
        }

        // Datetime
        if (request.getDatetime() != null && !request.getDatetime().isBlank()) {
            builder.datetime(parseDatetime(request.getDatetime()));
        }

        // Intersects geometry
        if (request.getIntersects() != null && !request.getIntersects().isBlank()) {
            try {
                org.wololo.geojson.GeoJSON geoJson = org.wololo.geojson.GeoJSONFactory.create(request.getIntersects());
                org.wololo.jts2geojson.GeoJSONReader reader = new org.wololo.jts2geojson.GeoJSONReader();
                builder.intersects(reader.read(geoJson));
            } catch (Exception e) {
                log.warn("Failed to parse intersects geometry: {}", request.getIntersects(), e);
            }
        }

        // Limit
        int effectiveLimit = request.getLimit() != null
                ? Math.min(request.getLimit(), maxLimit)
                : defaultLimit;
        builder.limit(effectiveLimit);

        // Pagination token
        if (request.getToken() != null && !request.getToken().isBlank()) {
            builder.token(request.getToken());
        }

        // CQL2 Filter
        if (request.getFilter() != null) {
            String filterLang = request.getFilterLang() != null ? request.getFilterLang() : "cql2-json";
            FilterCriteria filterCriteria = cql2FilterParser.parseCql2Json(request.getFilter());
            builder.filter(filterCriteria);
        }

        // Sort
        if (request.getSortby() != null && !request.getSortby().isEmpty()) {
            List<SortBy> sortBy = request.getSortby().stream()
                    .map(SortBy::fromString)
                    .collect(Collectors.toList());
            builder.sortBy(sortBy);
        }

        // Fields
        if (request.getFields() != null && !request.getFields().isBlank()) {
            builder.fields(FieldSelector.fromString(request.getFields()));
        }

        // Free-text search
        if (request.getQ() != null && !request.getQ().isBlank()) {
            builder.q(request.getQ());
        }

        return builder.build();
    }

    private TemporalExtent parseDatetime(String datetime) {
        String[] parts = datetime.split("/");
        List<String> interval;

        if (parts.length == 1) {
            // Single instant
            interval = Arrays.asList(parts[0], parts[0]);
        } else if (parts.length == 2) {
            // Interval
            String start = "..".equals(parts[0]) ? null : parts[0];
            String end = "..".equals(parts[1]) ? null : parts[1];
            interval = Arrays.asList(start, end);
        } else {
            throw new IllegalArgumentException("Invalid datetime format: " + datetime);
        }

        return TemporalExtent.builder()
                .interval(List.of(interval))
                .build();
    }

    /**
     * Adds standard STAC links to a Collection.
     */
    private Collection addCollectionLinks(Collection collection) {
        List<Link> links = new ArrayList<>();

        if (collection.getLinks() != null) {
            links.addAll(collection.getLinks());
        }

        // Self link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + collection.getId())
                .rel("self")
                .type("application/json")
                .title("This collection")
                .build());

        // Root link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Parent link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("parent")
                .type("application/json")
                .title("Parent catalog")
                .build());

        // Items link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + collection.getId() + "/items")
                .rel("items")
                .type("application/geo+json")
                .title("Items in this collection")
                .build());

        collection.setLinks(links);
        return collection;
    }

    /**
     * Adds standard STAC links to an Item.
     */
    private Item addItemLinks(Item item, String collectionId) {
        List<Link> links = new ArrayList<>();

        if (item.getLinks() != null) {
            links.addAll(item.getLinks());
        }

        String effectiveCollectionId = collectionId != null ? collectionId : item.getCollection();

        // Self link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + effectiveCollectionId + "/items/" + item.getId())
                .rel("self")
                .type("application/geo+json")
                .title("This item")
                .build());

        // Root link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Parent/collection link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + effectiveCollectionId)
                .rel("parent")
                .type("application/json")
                .title("Parent collection")
                .build());

        links.add(Link.builder()
                .href(rootUrl + "/collections/" + effectiveCollectionId)
                .rel("collection")
                .type("application/json")
                .title("Collection")
                .build());

        item.setLinks(links);
        return item;
    }

    private List<Link> buildItemCollectionLinks(String collectionId, Page<Item> page, int limit) {
        List<Link> links = new ArrayList<>();

        // Self link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + collectionId + "/items")
                .rel("self")
                .type("application/geo+json")
                .title("Items in this collection")
                .build());

        // Root link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Collection link
        links.add(Link.builder()
                .href(rootUrl + "/collections/" + collectionId)
                .rel("collection")
                .type("application/json")
                .title("Collection")
                .build());

        // Pagination
        if (page.hasNext()) {
            links.add(Link.builder()
                    .href(rootUrl + "/collections/" + collectionId + "/items?limit=" + limit +
                          "&offset=" + (page.getNumber() + 1) * limit)
                    .rel("next")
                    .type("application/geo+json")
                    .title("Next page")
                    .build());
        }

        if (page.hasPrevious()) {
            links.add(Link.builder()
                    .href(rootUrl + "/collections/" + collectionId + "/items?limit=" + limit +
                          "&offset=" + (page.getNumber() - 1) * limit)
                    .rel("prev")
                    .type("application/geo+json")
                    .title("Previous page")
                    .build());
        }

        return links;
    }

    private List<Link> buildSearchResultLinks(SearchRequest request, ItemCollection result) {
        List<Link> links = new ArrayList<>();

        // Self link
        links.add(Link.builder()
                .href(rootUrl + "/search")
                .rel("self")
                .type("application/geo+json")
                .title("This search")
                .build());

        // Root link
        links.add(Link.builder()
                .href(rootUrl)
                .rel("root")
                .type("application/json")
                .title("Root catalog")
                .build());

        // Next page link (if context indicates more results)
        if (result.getContext() != null &&
            result.getContext().getMatched() != null &&
            result.getContext().getReturned() != null &&
            result.getContext().getMatched() > result.getContext().getReturned()) {

            // Build next link with POST body
            links.add(Link.builder()
                    .href(rootUrl + "/search")
                    .rel("next")
                    .type("application/geo+json")
                    .title("Next page")
                    .method("POST")
                    .build());
        }

        return links;
    }
}
