package org.stac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stac.model.*;
import org.stac.service.StacService;

import java.util.List;

/**
 * REST controller for STAC Collection and Item endpoints.
 * <p>
 * Provides CRUD operations for collections and items as defined
 * by the STAC API and OGC API - Features specifications.
 */
@Slf4j
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "STAC Collection and Item endpoints")
public class CollectionsController {

    private final StacService stacService;

    // ========================================================================
    // Collection Endpoints
    // ========================================================================

    /**
     * Returns all available collections.
     *
     * @param limit maximum number of collections to return (optional)
     * @return list of collections with pagination links
     */
    @Operation(
            summary = "Get all collections",
            description = "Returns a list of all available STAC collections. " +
                    "Supports pagination through the limit parameter."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of collections",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collections.class)
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collections> getCollections(
            @Parameter(description = "Maximum number of collections to return")
            @RequestParam(required = false) Integer limit) {
        log.debug("Received request for collections with limit: {}", limit);
        Collections collections = stacService.getCollections(limit);
        return ResponseEntity.ok(collections);
    }

    /**
     * Returns a specific collection by ID.
     *
     * @param collectionId the collection identifier
     * @return the requested collection
     */
    @Operation(
            summary = "Get a collection",
            description = "Returns a single STAC collection identified by the collection ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The requested collection",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @GetMapping(value = "/{collectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection> getCollection(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId) {
        log.debug("Received request for collection: {}", collectionId);
        Collection collection = stacService.getCollection(collectionId);
        return ResponseEntity.ok(collection);
    }

    /**
     * Creates a new collection.
     *
     * @param collection the collection to create
     * @return the created collection with generated links
     */
    @Operation(
            summary = "Create a collection",
            description = "Creates a new STAC collection. " +
                    "The collection ID must be unique within the catalog."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Collection created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid collection data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Collection already exists",
                    content = @Content
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Collection> createCollection(
            @Valid @RequestBody Collection collection) {
        log.debug("Received request to create collection: {}", collection.getId());
        Collection createdCollection = stacService.createCollection(collection);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCollection);
    }

    /**
     * Updates an existing collection.
     *
     * @param collectionId the collection identifier
     * @param collection   the updated collection data
     * @return the updated collection
     */
    @Operation(
            summary = "Update a collection",
            description = "Replaces an existing STAC collection with the provided data. " +
                    "The collection ID in the path must match the ID in the request body."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Collection updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid collection data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @PutMapping(
            value = "/{collectionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Collection> updateCollection(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Valid @RequestBody Collection collection) {
        log.debug("Received request to update collection: {}", collectionId);
        Collection updatedCollection = stacService.updateCollection(collectionId, collection);
        return ResponseEntity.ok(updatedCollection);
    }

    /**
     * Partially updates a collection using JSON Merge Patch.
     *
     * @param collectionId the collection identifier
     * @param patch        the JSON Merge Patch to apply
     * @return the patched collection
     */
    @Operation(
            summary = "Patch a collection",
            description = "Partially updates a STAC collection using JSON Merge Patch (RFC 7386). " +
                    "Only the properties included in the patch will be modified."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Collection patched successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid patch data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @PatchMapping(
            value = "/{collectionId}",
            consumes = "application/merge-patch+json",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Collection> patchCollection(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @RequestBody JsonNode patch) {
        log.debug("Received request to patch collection: {}", collectionId);
        Collection patchedCollection = stacService.patchCollection(collectionId, patch);
        return ResponseEntity.ok(patchedCollection);
    }

    /**
     * Deletes a collection.
     *
     * @param collectionId the collection identifier
     * @return 204 No Content on successful deletion
     */
    @Operation(
            summary = "Delete a collection",
            description = "Deletes a STAC collection and optionally all its items."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Collection deleted successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @DeleteMapping(value = "/{collectionId}")
    public ResponseEntity<Void> deleteCollection(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId) {
        log.debug("Received request to delete collection: {}", collectionId);
        stacService.deleteCollection(collectionId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Item Endpoints
    // ========================================================================

    /**
     * Returns items within a collection with optional filtering.
     *
     * @param collectionId the collection identifier
     * @param limit        maximum number of items to return (optional)
     * @param bbox         bounding box for spatial filtering (optional)
     * @param datetime     datetime filter (optional)
     * @param token        pagination cursor token (optional)
     * @return item collection containing matching items
     */
    @Operation(
            summary = "Get items in a collection",
            description = "Returns items within a specific collection. " +
                    "Supports spatial filtering with bbox, temporal filtering with datetime, " +
                    "and pagination with limit and token parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Items in the collection",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = ItemCollection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @GetMapping(
            value = "/{collectionId}/items",
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<ItemCollection> getItems(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "Maximum number of items to return")
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "Bounding box for spatial filtering (west,south,east,north)")
            @RequestParam(required = false) String bbox,
            @Parameter(description = "Datetime filter (ISO 8601 datetime or interval)")
            @RequestParam(required = false) String datetime,
            @Parameter(description = "Pagination cursor token")
            @RequestParam(required = false) String token) {
        log.debug("Received request for items in collection: {} with limit: {}, bbox: {}, datetime: {}, token: {}",
                collectionId, limit, bbox, datetime, token);

        ItemQueryCriteria criteria = ItemQueryCriteria.builder()
                .limit(limit)
                .bbox(bbox != null ? BoundingBox.fromString(bbox) : null)
                .datetime(datetime)
                .token(token)
                .build();

        ItemCollection items = stacService.getItems(collectionId, criteria);
        return ResponseEntity.ok(items);
    }

    /**
     * Returns a specific item by collection and item ID.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @return the requested item
     */
    @Operation(
            summary = "Get an item",
            description = "Returns a single STAC item identified by the collection and item IDs."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The requested item",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = Item.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item or collection not found",
                    content = @Content
            )
    })
    @GetMapping(
            value = "/{collectionId}/items/{itemId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<Item> getItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "The item identifier", required = true)
            @PathVariable String itemId) {
        log.debug("Received request for item: {} in collection: {}", itemId, collectionId);
        Item item = stacService.getItem(collectionId, itemId);
        return ResponseEntity.ok(item);
    }

    // ========================================================================
    // Aggregation Endpoints
    // ========================================================================

    /**
     * Returns aggregations for items within a specific collection using query parameters (GET method).
     *
     * @param collectionId the collection identifier
     * @param datetime     datetime filter (optional)
     * @param bbox         bounding box for spatial filtering (optional)
     * @param aggregations list of fields to aggregate (optional, defaults to datetime)
     * @return aggregation results for the collection
     */
    @Operation(
            summary = "Aggregate items in a collection (GET)",
            description = "Performs aggregation queries on STAC items within a specific collection. " +
                    "Supports term aggregations, numeric statistics, datetime histograms, and geometry bounds. " +
                    "Can be filtered by bbox and datetime."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Aggregation results",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AggregationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @GetMapping(
            value = "/{collectionId}/aggregate",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AggregationResponse> getCollectionAggregations(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "Datetime filter (ISO 8601 datetime or interval)")
            @RequestParam(required = false) String datetime,
            @Parameter(description = "Bounding box for spatial filtering (west,south,east,north)")
            @RequestParam(required = false) String bbox,
            @Parameter(description = "List of fields to aggregate")
            @RequestParam(required = false, defaultValue = "datetime") List<String> aggregations) {

        log.debug("Received GET aggregate request for collection: {}, datetime={}, bbox={}, aggregations={}",
                collectionId, datetime, bbox, aggregations);

        // Parse bbox string to double array if provided
        double[] bboxArray = null;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            bboxArray = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                bboxArray[i] = Double.parseDouble(parts[i].trim());
            }
        }

        AggregationRequest request = AggregationRequest.builder()
                .collections(List.of(collectionId))
                .datetime(datetime)
                .bbox(bboxArray)
                .aggregations(aggregations)
                .build();

        AggregationResponse response = stacService.aggregate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns aggregations for items within a specific collection using a JSON request body (POST method).
     *
     * @param collectionId the collection identifier
     * @param request      the aggregation request containing fields and optional filters
     * @return aggregation results for the collection
     */
    @Operation(
            summary = "Aggregate items in a collection (POST)",
            description = "Performs aggregation queries on STAC items within a specific collection. " +
                    "Supports term aggregations, numeric statistics, datetime histograms, and geometry bounds. " +
                    "Can be filtered by bbox, datetime, and CQL2 filters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Aggregation results",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AggregationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid aggregation request",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @PostMapping(
            value = "/{collectionId}/aggregate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AggregationResponse> postCollectionAggregations(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @RequestBody AggregationRequest request) {

        log.debug("Received POST aggregate request for collection: {}, request: {}", collectionId, request);

        // Ensure the collection is scoped to the specified collection ID
        // If the request already has collections, we override to scope to this collection only
        AggregationRequest scopedRequest = AggregationRequest.builder()
                .collections(List.of(collectionId))
                .datetime(request.getDatetime())
                .bbox(request.getBbox())
                .aggregations(request.getAggregations())
                .filter(request.getFilter())
                .build();

        AggregationResponse response = stacService.aggregate(scopedRequest);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Transaction Endpoints (CRUD operations)
    // ========================================================================

    /**
     * Creates a new item in a collection.
     *
     * @param collectionId the collection identifier
     * @param item         the item to create
     * @return the created item with generated links
     */
    @Operation(
            summary = "Create an item",
            description = "Creates a new STAC item in the specified collection. " +
                    "The item ID must be unique within the collection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Item created successfully",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = Item.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid item data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Item already exists",
                    content = @Content
            )
    })
    @PostMapping(
            value = "/{collectionId}/items",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"},
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<Item> createItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Valid @RequestBody Item item) {
        log.debug("Received request to create item in collection: {}", collectionId);
        Item createdItem = stacService.createItem(collectionId, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * Updates an existing item in a collection.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @param item         the updated item data
     * @return the updated item
     */
    @Operation(
            summary = "Update an item",
            description = "Replaces an existing STAC item with the provided data. " +
                    "The item ID in the path must match the ID in the request body."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item updated successfully",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = Item.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid item data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item or collection not found",
                    content = @Content
            )
    })
    @PutMapping(
            value = "/{collectionId}/items/{itemId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"},
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<Item> updateItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "The item identifier", required = true)
            @PathVariable String itemId,
            @Valid @RequestBody Item item) {
        log.debug("Received request to update item: {} in collection: {}", itemId, collectionId);

        // Ensure the item ID matches the path parameter
        item.setId(itemId);

        Item updatedItem = stacService.updateItem(collectionId, item);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * Partially updates an item using JSON Merge Patch.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @param patch        the JSON Merge Patch to apply
     * @return the patched item
     */
    @Operation(
            summary = "Patch an item",
            description = "Partially updates a STAC item using JSON Merge Patch (RFC 7386). " +
                    "Only the properties included in the patch will be modified. " +
                    "The item ID and collection reference are immutable."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item patched successfully",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = Item.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid patch data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item or collection not found",
                    content = @Content
            )
    })
    @PatchMapping(
            value = "/{collectionId}/items/{itemId}",
            consumes = "application/merge-patch+json",
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<Item> patchItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "The item identifier", required = true)
            @PathVariable String itemId,
            @RequestBody JsonNode patch) {
        log.debug("Received request to patch item: {} in collection: {}", itemId, collectionId);
        Item patchedItem = stacService.patchItem(collectionId, itemId, patch);
        return ResponseEntity.ok(patchedItem);
    }

    /**
     * Deletes an item from a collection.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @return 204 No Content on successful deletion
     */
    @Operation(
            summary = "Delete an item",
            description = "Deletes a STAC item from the specified collection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Item deleted successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item or collection not found",
                    content = @Content
            )
    })
    @DeleteMapping(value = "/{collectionId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "The item identifier", required = true)
            @PathVariable String itemId) {
        log.debug("Received request to delete item: {} from collection: {}", itemId, collectionId);
        stacService.deleteItem(collectionId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Bulk Operations
    // ========================================================================

    /**
     * Performs bulk operations on items in a collection.
     * <p>
     * This endpoint provides stac-fastapi-pgstac parity for bulk item operations.
     *
     * @param collectionId the collection identifier
     * @param request      the bulk items request containing items and operation method
     * @return the result of the bulk operation
     */
    @Operation(
            summary = "Bulk items operation",
            description = "Performs bulk operations on items in a collection. " +
                    "Supports three methods: 'insert' (fails if items exist), " +
                    "'upsert' (insert or update, default), and 'delete' (removes items by ID). " +
                    "This endpoint provides parity with stac-fastapi-pgstac."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bulk operation completed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BulkOperationResult.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @PostMapping(
            value = "/{collectionId}/bulk_items",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BulkOperationResult> bulkItems(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Valid @RequestBody BulkItemsRequest request) {
        log.debug("Received bulk items request for collection: {} with method: {} and {} items",
                collectionId, request.getMethod(), request.getItems() != null ? request.getItems().size() : 0);

        BulkOperationResult result = stacService.bulkItems(
                collectionId,
                request.getItems(),
                request.getMethod()
        );

        return ResponseEntity.ok(result);
    }
}
