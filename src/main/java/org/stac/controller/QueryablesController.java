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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.stac.model.Queryables;
import org.stac.service.StacService;

/**
 * REST controller for STAC Filter Extension Queryables endpoints.
 * <p>
 * Provides JSON Schema descriptions of queryable properties that can
 * be used in CQL2 filter expressions.
 *
 * @see <a href="https://github.com/stac-api-extensions/filter">STAC API Filter Extension</a>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Filter Extension", description = "STAC API Filter Extension endpoints")
public class QueryablesController {

    private static final String SCHEMA_CONTENT_TYPE = "application/schema+json";

    private final StacService stacService;

    /**
     * Returns the global queryables for all collections.
     *
     * @return JSON Schema describing queryable properties
     */
    @Operation(
            summary = "Get global queryables",
            description = "Returns a JSON Schema describing which properties can be used " +
                    "in CQL2 filter expressions across all collections."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Queryables schema",
                    content = @Content(
                            mediaType = SCHEMA_CONTENT_TYPE,
                            schema = @Schema(implementation = Queryables.class)
                    )
            )
    })
    @GetMapping(value = "/queryables", produces = SCHEMA_CONTENT_TYPE)
    public ResponseEntity<Queryables> getQueryables() {
        log.debug("Received request for global queryables");
        Queryables queryables = stacService.getQueryables();
        return ResponseEntity.ok(queryables);
    }

    /**
     * Returns the queryables for a specific collection.
     *
     * @param collectionId the collection identifier
     * @return JSON Schema describing queryable properties for the collection
     */
    @Operation(
            summary = "Get collection queryables",
            description = "Returns a JSON Schema describing which properties can be used " +
                    "in CQL2 filter expressions for items in this collection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Queryables schema for the collection",
                    content = @Content(
                            mediaType = SCHEMA_CONTENT_TYPE,
                            schema = @Schema(implementation = Queryables.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Collection not found",
                    content = @Content
            )
    })
    @GetMapping(value = "/collections/{collectionId}/queryables", produces = SCHEMA_CONTENT_TYPE)
    public ResponseEntity<Queryables> getCollectionQueryables(
            @Parameter(description = "The collection identifier", required = true)
            @PathVariable String collectionId) {
        log.debug("Received request for queryables of collection: {}", collectionId);
        Queryables queryables = stacService.getQueryables(collectionId);
        return ResponseEntity.ok(queryables);
    }
}
