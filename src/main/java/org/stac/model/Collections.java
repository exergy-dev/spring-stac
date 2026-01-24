package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAC Collections List Response DTO.
 * Represents the response from the /collections endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collections {

    /**
     * Array of STAC Collection objects.
     */
    private List<Collection> collections;

    /**
     * Links for pagination and related resources.
     */
    private List<Link> links;
}
