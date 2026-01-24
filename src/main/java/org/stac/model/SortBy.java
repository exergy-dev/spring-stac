package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sort specification for STAC search results.
 * <p>
 * Defines a field and direction for sorting search results.
 * Supports the STAC Sort Extension format using +/- prefixes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortBy {

    /**
     * The field name to sort by.
     * <p>
     * Common STAC fields: "properties.datetime", "id", "collection"
     */
    private String field;

    /**
     * The sort direction (ascending or descending).
     */
    private SortDirection direction;

    /**
     * Parses a sort specification from a string.
     * <p>
     * Supports the STAC Sort Extension format:
     * - "+field" or "field" for ascending order
     * - "-field" for descending order
     *
     * @param sortSpec the sort specification string
     * @return parsed SortBy instance
     * @throws IllegalArgumentException if the string is null or empty
     */
    public static SortBy fromString(String sortSpec) {
        if (sortSpec == null || sortSpec.isBlank()) {
            throw new IllegalArgumentException("Sort specification cannot be null or empty");
        }

        String trimmed = sortSpec.trim();

        if (trimmed.startsWith("-")) {
            return SortBy.builder()
                    .field(trimmed.substring(1))
                    .direction(SortDirection.DESC)
                    .build();
        } else if (trimmed.startsWith("+")) {
            return SortBy.builder()
                    .field(trimmed.substring(1))
                    .direction(SortDirection.ASC)
                    .build();
        } else {
            // Default to ascending if no prefix
            return SortBy.builder()
                    .field(trimmed)
                    .direction(SortDirection.ASC)
                    .build();
        }
    }

    /**
     * Creates an ascending sort specification.
     *
     * @param field the field to sort by
     * @return SortBy with ascending direction
     */
    public static SortBy asc(String field) {
        return SortBy.builder()
                .field(field)
                .direction(SortDirection.ASC)
                .build();
    }

    /**
     * Creates a descending sort specification.
     *
     * @param field the field to sort by
     * @return SortBy with descending direction
     */
    public static SortBy desc(String field) {
        return SortBy.builder()
                .field(field)
                .direction(SortDirection.DESC)
                .build();
    }

    /**
     * Converts this SortBy to its string representation.
     *
     * @return string in "+field" or "-field" format
     */
    public String toSpecString() {
        String prefix = direction == SortDirection.DESC ? "-" : "+";
        return prefix + field;
    }
}
