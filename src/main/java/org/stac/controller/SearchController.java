package org.stac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stac.model.ItemCollection;
import org.stac.model.SearchRequest;
import org.stac.service.StacService;

/**
 * REST controller for STAC API search endpoint.
 * <p>
 * Provides cross-collection search capabilities with support for
 * spatial, temporal, and property-based filtering as defined by
 * the STAC API Item Search specification.
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Item Search", description = "STAC API Item Search endpoints")
public class SearchController {

    private final StacService stacService;

    /**
     * Searches for items using query parameters (GET method).
     * <p>
     * This endpoint supports simple search queries via URL parameters.
     * For complex queries with CQL2 filters, use the POST method instead.
     *
     * @param request the search request parameters bound from query string
     * @return item collection containing matching items
     */
    @Operation(
            summary = "Search items (GET)",
            description = "Searches for STAC items across collections using query parameters. " +
                    "Supports filtering by collections, IDs, bbox, datetime, and pagination. " +
                    "For complex queries with CQL2 filters, use the POST method."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = ItemCollection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid search parameters",
                    content = @Content
            )
    })
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"})
    public ResponseEntity<ItemCollection> searchGet(@ModelAttribute SearchRequest request) {
        log.debug("Received GET search request: {}", request);
        ItemCollection results = stacService.search(request);
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for items using a JSON request body (POST method).
     * <p>
     * This endpoint supports the full range of search capabilities including
     * CQL2 filters, intersects geometry, and complex query expressions.
     *
     * @param request the search request containing all search parameters
     * @return item collection containing matching items
     */
    @Operation(
            summary = "Search items (POST)",
            description = "Searches for STAC items across collections using a JSON request body. " +
                    "Supports the full range of search capabilities including CQL2 filters, " +
                    "spatial intersection (intersects), sorting, and field selection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results",
                    content = @Content(
                            mediaType = "application/geo+json",
                            schema = @Schema(implementation = ItemCollection.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid search request",
                    content = @Content
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/geo+json"}
    )
    public ResponseEntity<ItemCollection> searchPost(@RequestBody SearchRequest request) {
        log.debug("Received POST search request: {}", request);
        ItemCollection results = stacService.search(request);
        return ResponseEntity.ok(results);
    }
}
