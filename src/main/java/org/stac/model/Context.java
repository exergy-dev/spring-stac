package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STAC Context Extension Model.
 * Provides metadata about search results including pagination information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    /**
     * Number of items returned in this response.
     */
    private Integer returned;

    /**
     * Maximum number of items requested (the limit parameter).
     */
    private Integer limit;

    /**
     * Total number of items matching the query.
     * May be null if the total count is not available or too expensive to compute.
     */
    private Integer matched;
}
