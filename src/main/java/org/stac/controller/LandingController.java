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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stac.model.Conformance;
import org.stac.model.LandingPage;
import org.stac.service.StacService;

/**
 * REST controller for STAC API landing page and conformance endpoints.
 * <p>
 * Provides the root catalog entry point and conformance declaration
 * as required by the STAC API specification.
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Core", description = "STAC API Core endpoints")
public class LandingController {

    private final StacService stacService;

    /**
     * Returns the STAC API landing page (root catalog).
     * <p>
     * The landing page provides links to all available resources
     * including collections, search, and conformance endpoints.
     *
     * @return the landing page with links to API resources
     */
    @Operation(
            summary = "Landing page",
            description = "Returns the root catalog for this STAC API. The landing page provides " +
                    "links to the API definition, conformance declaration, and available collections."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The landing page",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LandingPage.class)
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LandingPage> getLandingPage() {
        log.debug("Received request for landing page");
        LandingPage landingPage = stacService.getLandingPage();
        return ResponseEntity.ok(landingPage);
    }

    /**
     * Returns the conformance declaration.
     * <p>
     * Lists all conformance classes that this API implements,
     * including STAC API, OGC API - Features, and CQL2 conformance classes.
     *
     * @return the conformance declaration with all implemented conformance class URIs
     */
    @Operation(
            summary = "Conformance declaration",
            description = "Returns the list of conformance classes that this API implements. " +
                    "Conformance classes are identified by URIs as specified in STAC API and OGC API standards."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The conformance classes implemented by this API",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Conformance.class)
                    )
            )
    })
    @GetMapping(value = "/conformance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Conformance> getConformance() {
        log.debug("Received request for conformance declaration");
        Conformance conformance = stacService.getConformance();
        return ResponseEntity.ok(conformance);
    }
}
