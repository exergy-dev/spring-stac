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

/**
 * REST controller for STAC Item endpoints.
 * <p>
 * Provides CRUD operations for items within collections as defined
 * by the STAC API and OGC API - Features specifications.
 */
@Slf4j
@RestController
@RequestMapping("/collections/{collectionId}/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "STAC Item endpoints")
public class ItemsController {

    private final StacService stacService;

    // ========================================================================
    // Item Query Endpoints
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
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"})
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
            value = "/{itemId}",
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
            value = "/{itemId}",
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
            value = "/{itemId}",
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
    @DeleteMapping(value = "/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId,
            @Parameter(description = "The item identifier", required = true)
            @PathVariable String itemId) {
        log.debug("Received request to delete item: {} from collection: {}", itemId, collectionId);
        stacService.deleteItem(collectionId, itemId);
        return ResponseEntity.noContent().build();
    }
}
