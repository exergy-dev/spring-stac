package org.stac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.stac.model.AggregationRequest;
import org.stac.model.AggregationResponse;
import org.stac.model.BoundingBox;
import org.stac.service.StacService;

import java.util.List;

/**
 * REST controller for collection-scoped STAC Aggregation Extension endpoints.
 * <p>
 * Provides aggregation capabilities for STAC items within a specific collection.
 *
 * @see <a href="https://github.com/stac-api-extensions/aggregation">STAC Aggregation Extension</a>
 */
@Slf4j
@RestController
@RequestMapping("/collections/{collectionId}/aggregate")
@RequiredArgsConstructor
@Tag(name = "Aggregation", description = "STAC Aggregation Extension endpoints")
public class CollectionAggregateController {

    private final StacService stacService;

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
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
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

        AggregationRequest request = AggregationRequest.builder()
                .collections(List.of(collectionId))
                .datetime(datetime)
                .bbox(BoundingBox.parseToArray(bbox))
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
}
