package org.stac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stac.model.BulkItemsRequest;
import org.stac.model.BulkOperationResult;
import org.stac.service.StacService;

/**
 * REST controller for STAC bulk item operations.
 * <p>
 * Provides bulk insert, upsert, and delete operations for items
 * within collections, providing parity with stac-fastapi-pgstac.
 */
@Slf4j
@RestController
@RequestMapping("/collections/{collectionId}/bulk_items")
@RequiredArgsConstructor
@Tag(name = "Bulk Operations", description = "STAC bulk item operation endpoints")
public class BulkItemsController {

    private final StacService stacService;

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
