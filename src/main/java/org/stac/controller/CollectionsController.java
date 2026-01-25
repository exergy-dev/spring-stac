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
 * REST controller for STAC Collection endpoints.
 * <p>
 * Provides CRUD operations for collections as defined
 * by the STAC API and OGC API - Features specifications.
 */
@Slf4j
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "STAC Collection endpoints")
public class CollectionsController {

    private final StacService stacService;

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
}
