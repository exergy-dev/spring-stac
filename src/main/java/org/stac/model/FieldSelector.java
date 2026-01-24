package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fields extension support for STAC API.
 * <p>
 * Allows clients to specify which fields to include or exclude from responses.
 * Follows the STAC Fields Extension specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSelector {

    /**
     * Fields to include in the response.
     * <p>
     * If specified, only these fields will be returned.
     * Uses dotted notation for nested fields (e.g., "properties.datetime").
     */
    @Builder.Default
    private Set<String> include = new HashSet<>();

    /**
     * Fields to exclude from the response.
     * <p>
     * These fields will be omitted from the response.
     * Uses dotted notation for nested fields (e.g., "properties.eo:cloud_cover").
     */
    @Builder.Default
    private Set<String> exclude = new HashSet<>();

    /**
     * Parses field expressions from a string.
     * <p>
     * Format: Comma-separated field names with optional +/- prefix.
     * - "+field" or "field" indicates inclusion
     * - "-field" indicates exclusion
     * <p>
     * Example: "+id,+geometry,-properties.description"
     *
     * @param fieldsSpec comma-separated field expressions
     * @return parsed FieldSelector instance
     */
    public static FieldSelector fromString(String fieldsSpec) {
        if (fieldsSpec == null || fieldsSpec.isBlank()) {
            return FieldSelector.builder().build();
        }

        Set<String> include = new HashSet<>();
        Set<String> exclude = new HashSet<>();

        Arrays.stream(fieldsSpec.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(field -> {
                    if (field.startsWith("-")) {
                        exclude.add(field.substring(1));
                    } else if (field.startsWith("+")) {
                        include.add(field.substring(1));
                    } else {
                        include.add(field);
                    }
                });

        return FieldSelector.builder()
                .include(include)
                .exclude(exclude)
                .build();
    }

    /**
     * Parses field expressions from separate include and exclude sets.
     *
     * @param includeFields fields to include
     * @param excludeFields fields to exclude
     * @return FieldSelector with the specified fields
     */
    public static FieldSelector of(Set<String> includeFields, Set<String> excludeFields) {
        return FieldSelector.builder()
                .include(includeFields != null ? new HashSet<>(includeFields) : new HashSet<>())
                .exclude(excludeFields != null ? new HashSet<>(excludeFields) : new HashSet<>())
                .build();
    }

    /**
     * Creates a FieldSelector that includes only the specified fields.
     *
     * @param fields fields to include
     * @return FieldSelector with includes only
     */
    public static FieldSelector includeOnly(String... fields) {
        return FieldSelector.builder()
                .include(new HashSet<>(Arrays.asList(fields)))
                .build();
    }

    /**
     * Creates a FieldSelector that excludes the specified fields.
     *
     * @param fields fields to exclude
     * @return FieldSelector with excludes only
     */
    public static FieldSelector excludeOnly(String... fields) {
        return FieldSelector.builder()
                .exclude(new HashSet<>(Arrays.asList(fields)))
                .build();
    }

    /**
     * Checks if this selector has any field specifications.
     *
     * @return true if either include or exclude has entries
     */
    public boolean hasSelections() {
        return (include != null && !include.isEmpty()) ||
               (exclude != null && !exclude.isEmpty());
    }

    /**
     * Checks if a specific field should be included in the response.
     *
     * @param fieldPath the field path to check (dotted notation)
     * @return true if the field should be included
     */
    public boolean shouldInclude(String fieldPath) {
        // If exclude list contains this field, exclude it
        if (exclude != null && exclude.contains(fieldPath)) {
            return false;
        }
        // If include list is empty, include everything not excluded
        if (include == null || include.isEmpty()) {
            return true;
        }
        // If include list is specified, only include fields in the list
        return include.contains(fieldPath) || isParentIncluded(fieldPath);
    }

    /**
     * Checks if a parent path of the given field is in the include list.
     * For example, if "properties" is included, "properties.datetime" should also be included.
     *
     * @param fieldPath the field path to check
     * @return true if a parent is included
     */
    private boolean isParentIncluded(String fieldPath) {
        if (include == null) {
            return false;
        }
        String[] parts = fieldPath.split("\\.");
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                path.append(".");
            }
            path.append(parts[i]);
            if (include.contains(path.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts this FieldSelector to its string representation.
     *
     * @return comma-separated field expressions with +/- prefixes
     */
    public String toSpecString() {
        StringBuilder sb = new StringBuilder();

        if (include != null) {
            sb.append(include.stream()
                    .map(f -> "+" + f)
                    .collect(Collectors.joining(",")));
        }

        if (exclude != null && !exclude.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(exclude.stream()
                    .map(f -> "-" + f)
                    .collect(Collectors.joining(",")));
        }

        return sb.toString();
    }
}
