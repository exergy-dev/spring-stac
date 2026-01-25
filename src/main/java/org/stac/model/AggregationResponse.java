package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stac.model.aggregation.Aggregation;

import java.util.List;

/**
 * Response model for STAC Aggregation Extension.
 * <p>
 * Contains aggregated statistics and facet information for STAC items,
 * following the STAC Aggregation Extension specification.
 *
 * @see <a href="https://github.com/stac-api-extensions/aggregation">STAC Aggregation Extension</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AggregationResponse {

    /**
     * Type identifier for the aggregation response.
     */
    @Builder.Default
    private String type = "AggregationCollection";

    /**
     * List of aggregation results, one per requested aggregation field.
     */
    private List<Aggregation> aggregations;

    /**
     * Links related to this aggregation response.
     */
    private List<Link> links;

    /**
     * Creates an empty aggregation response.
     */
    public static AggregationResponse empty() {
        return AggregationResponse.builder()
                .aggregations(List.of())
                .build();
    }
}
