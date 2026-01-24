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
import org.stac.model.AggregationRequest;
import org.stac.model.AggregationResponse;
import org.stac.service.StacService;

import java.util.List;

/**
 * REST controller for STAC Aggregation Extension endpoints.
 * <p>
 * Provides aggregation capabilities for STAC items including:
 * - Term/bucket aggregations (faceted counts)
 * - Numeric statistics (min, max, avg, sum)
 * - Datetime histograms
 * - Geometry bounds
 *
 * @see <a href="https://github.com/stac-api-extensions/aggregation">STAC Aggregation Extension</a>
 */
@Slf4j
@RestController
@RequestMapping("/aggregate")
@RequiredArgsConstructor
@Tag(name = "Aggregation", description = "STAC Aggregation Extension endpoints")
public class AggregateController {

    private final StacService stacService;

    /**
     * Returns available aggregation fields.
     *
     * @return list of available aggregation field names
     */
    @Operation(
            summary = "Get available aggregations",
            description = "Returns the list of fields that can be aggregated across all collections."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of available aggregation fields",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AggregationResponse> getAggregations(
            @RequestParam(required = false) List<String> collections,
            @RequestParam(required = false) String datetime,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false, defaultValue = "collection,datetime") List<String> aggregations) {

        log.debug("Received GET aggregate request: collections={}, datetime={}, bbox={}, aggregations={}",
                collections, datetime, bbox, aggregations);

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
                .collections(collections)
                .datetime(datetime)
                .bbox(bboxArray)
                .aggregations(aggregations)
                .build();

        AggregationResponse response = stacService.aggregate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs aggregation using a JSON request body (POST method).
     *
     * @param request the aggregation request containing fields and optional filters
     * @return aggregation results
     */
    @Operation(
            summary = "Aggregate items (POST)",
            description = "Performs aggregation queries on STAC items. Supports term aggregations, " +
                    "numeric statistics, datetime histograms, and geometry bounds. " +
                    "Can be filtered by collections, bbox, datetime, and CQL2 filters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Aggregation results",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AggregationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid aggregation request",
                    content = @Content
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AggregationResponse> aggregatePost(@RequestBody AggregationRequest request) {
        log.debug("Received POST aggregate request: {}", request);
        AggregationResponse response = stacService.aggregate(request);
        return ResponseEntity.ok(response);
    }
}
