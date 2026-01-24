package org.stac.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stac.exception.ValidationException;
import org.stac.model.Collection;
import org.stac.model.Item;
import org.stac.model.SearchRequest;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating STAC resources.
 * <p>
 * Provides validation methods for Items, Collections, and SearchRequests
 * according to the STAC specification requirements.
 */
@Slf4j
@Component
public class ValidationService {

    private static final Pattern BBOX_PATTERN = Pattern.compile(
            "^-?\\d+\\.?\\d*,-?\\d+\\.?\\d*,-?\\d+\\.?\\d*,-?\\d+\\.?\\d*(,-?\\d+\\.?\\d*,-?\\d+\\.?\\d*)?$");

    private static final Pattern ISO_8601_INSTANT = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?)?$");

    private static final Pattern DATETIME_INTERVAL = Pattern.compile(
            "^(\\.\\.|\\.\\.|\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?)?)/(\\.\\.|\\.\\.|\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?)?)$");

    /**
     * Validates a STAC Item.
     *
     * @param item the Item to validate
     * @throws ValidationException if the Item is invalid
     */
    public void validateItem(Item item) {
        if (item == null) {
            throw ValidationException.item("Item cannot be null");
        }

        List<String> errors = new ArrayList<>();

        // Required: id
        if (item.getId() == null || item.getId().isBlank()) {
            errors.add("Item id is required");
        }

        // Required: type must be "Feature"
        if (item.getType() == null || !"Feature".equals(item.getType())) {
            errors.add("Item type must be 'Feature'");
        }

        // Required: properties
        if (item.getProperties() == null) {
            errors.add("Item properties are required");
        } else {
            // Required: properties.datetime (can be null if start_datetime and end_datetime exist)
            Object datetime = item.getProperties().get("datetime");
            Object startDatetime = item.getProperties().get("start_datetime");
            Object endDatetime = item.getProperties().get("end_datetime");

            if (datetime == null && (startDatetime == null || endDatetime == null)) {
                errors.add("Item must have either 'datetime' or both 'start_datetime' and 'end_datetime'");
            }

            // Validate datetime format if present
            if (datetime != null && datetime instanceof String) {
                try {
                    validateDatetimeValue((String) datetime);
                } catch (ValidationException e) {
                    errors.add("Invalid datetime format: " + e.getMessage());
                }
            }
        }

        // Geometry is required (can be null for certain items per spec, but must be present)
        // Note: geometry can be null for items that have no geospatial extent
        // We don't enforce this as strictly per STAC spec

        // Validate bbox if present
        if (item.getBbox() != null) {
            double[] bbox = item.getBbox();
            if (bbox.length != 4 && bbox.length != 6) {
                errors.add("Item bbox must have 4 or 6 values");
            } else {
                try {
                    validateBboxArray(bbox);
                } catch (ValidationException e) {
                    errors.add(e.getMessage());
                }
            }
        }

        // Validate stac_version if present
        if (item.getStacVersion() != null && !item.getStacVersion().matches("\\d+\\.\\d+\\.\\d+.*")) {
            errors.add("Invalid stac_version format");
        }

        if (!errors.isEmpty()) {
            throw ValidationException.item(errors);
        }

        log.debug("Item validation passed for id: {}", item.getId());
    }

    /**
     * Validates a STAC Collection.
     *
     * @param collection the Collection to validate
     * @throws ValidationException if the Collection is invalid
     */
    public void validateCollection(Collection collection) {
        if (collection == null) {
            throw ValidationException.collection("Collection cannot be null");
        }

        List<String> errors = new ArrayList<>();

        // Required: id
        if (collection.getId() == null || collection.getId().isBlank()) {
            errors.add("Collection id is required");
        }

        // Required: type must be "Collection"
        if (collection.getType() == null || !"Collection".equals(collection.getType())) {
            errors.add("Collection type must be 'Collection'");
        }

        // Required: description
        if (collection.getDescription() == null || collection.getDescription().isBlank()) {
            errors.add("Collection description is required");
        }

        // Required: license
        if (collection.getLicense() == null || collection.getLicense().isBlank()) {
            errors.add("Collection license is required");
        }

        // Required: extent
        if (collection.getExtent() == null) {
            errors.add("Collection extent is required");
        } else {
            // Validate spatial extent
            if (collection.getExtent().getSpatial() == null) {
                errors.add("Collection spatial extent is required");
            } else if (collection.getExtent().getSpatial().getBbox() == null ||
                       collection.getExtent().getSpatial().getBbox().isEmpty()) {
                errors.add("Collection spatial extent must have at least one bbox");
            }

            // Validate temporal extent
            if (collection.getExtent().getTemporal() == null) {
                errors.add("Collection temporal extent is required");
            } else if (collection.getExtent().getTemporal().getInterval() == null ||
                       collection.getExtent().getTemporal().getInterval().isEmpty()) {
                errors.add("Collection temporal extent must have at least one interval");
            }
        }

        // Validate stac_version if present
        if (collection.getStacVersion() != null && !collection.getStacVersion().matches("\\d+\\.\\d+\\.\\d+.*")) {
            errors.add("Invalid stac_version format");
        }

        if (!errors.isEmpty()) {
            throw ValidationException.collection(errors);
        }

        log.debug("Collection validation passed for id: {}", collection.getId());
    }

    /**
     * Validates a SearchRequest.
     *
     * @param request the SearchRequest to validate
     * @throws ValidationException if the SearchRequest is invalid
     */
    public void validateSearchRequest(SearchRequest request) {
        if (request == null) {
            throw ValidationException.searchRequest("SearchRequest cannot be null");
        }

        List<String> errors = new ArrayList<>();

        // Validate bbox if present
        if (request.getBbox() != null && !request.getBbox().isBlank()) {
            try {
                validateBbox(request.getBbox());
            } catch (ValidationException e) {
                errors.add("Invalid bbox: " + e.getMessage());
            }
        }

        // Validate datetime if present
        if (request.getDatetime() != null && !request.getDatetime().isBlank()) {
            try {
                validateDatetime(request.getDatetime());
            } catch (ValidationException e) {
                errors.add("Invalid datetime: " + e.getMessage());
            }
        }

        // Validate limit if present
        if (request.getLimit() != null) {
            if (request.getLimit() < 1) {
                errors.add("Limit must be at least 1");
            }
        }

        // Validate intersects geometry if present
        if (request.getIntersects() != null && !request.getIntersects().isBlank()) {
            try {
                validateGeoJson(request.getIntersects());
            } catch (Exception e) {
                errors.add("Invalid intersects geometry: " + e.getMessage());
            }
        }

        // Validate filter-lang if filter is present
        if (request.getFilter() != null && request.getFilterLang() != null) {
            String filterLang = request.getFilterLang().toLowerCase();
            if (!filterLang.equals("cql2-text") && !filterLang.equals("cql2-json")) {
                errors.add("filter-lang must be 'cql2-text' or 'cql2-json'");
            }
        }

        if (!errors.isEmpty()) {
            throw ValidationException.searchRequest(errors);
        }

        log.debug("SearchRequest validation passed");
    }

    /**
     * Validates a bounding box string.
     *
     * @param bbox the bbox string to validate (4 or 6 comma-separated values)
     * @throws ValidationException if the bbox is invalid
     */
    public void validateBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            throw new ValidationException("Bbox cannot be null or empty");
        }

        String trimmed = bbox.trim();
        if (!BBOX_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid bbox format. Expected 4 or 6 comma-separated numbers");
        }

        String[] parts = trimmed.split(",");
        if (parts.length != 4 && parts.length != 6) {
            throw new ValidationException("Bbox must have 4 or 6 values, got " + parts.length);
        }

        try {
            double[] values = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                values[i] = Double.parseDouble(parts[i].trim());
            }
            validateBboxArray(values);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid number format in bbox: " + e.getMessage());
        }
    }

    private void validateBboxArray(double[] bbox) {
        double west, south, east, north;

        if (bbox.length == 4) {
            west = bbox[0];
            south = bbox[1];
            east = bbox[2];
            north = bbox[3];
        } else {
            // 6-value bbox: west, south, minZ, east, north, maxZ
            west = bbox[0];
            south = bbox[1];
            east = bbox[3];
            north = bbox[4];
            double minZ = bbox[2];
            double maxZ = bbox[5];
            if (minZ > maxZ) {
                throw new ValidationException("minZ cannot be greater than maxZ");
            }
        }

        if (west < -180 || west > 180) {
            throw new ValidationException("West longitude must be between -180 and 180");
        }
        if (east < -180 || east > 180) {
            throw new ValidationException("East longitude must be between -180 and 180");
        }
        if (south < -90 || south > 90) {
            throw new ValidationException("South latitude must be between -90 and 90");
        }
        if (north < -90 || north > 90) {
            throw new ValidationException("North latitude must be between -90 and 90");
        }
        if (south > north) {
            throw new ValidationException("South latitude cannot be greater than north latitude");
        }
    }

    /**
     * Validates a datetime string in ISO 8601 format or interval.
     *
     * @param datetime the datetime string to validate
     * @throws ValidationException if the datetime is invalid
     */
    public void validateDatetime(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            throw new ValidationException("Datetime cannot be null or empty");
        }

        String trimmed = datetime.trim();

        // Check if it's an interval
        if (trimmed.contains("/")) {
            validateDatetimeInterval(trimmed);
        } else {
            validateDatetimeValue(trimmed);
        }
    }

    private void validateDatetimeInterval(String interval) {
        String[] parts = interval.split("/");
        if (parts.length != 2) {
            throw new ValidationException("Invalid interval format. Expected 'start/end'");
        }

        String start = parts[0].trim();
        String end = parts[1].trim();

        // Open intervals are allowed with ".."
        if (!start.equals("..")) {
            validateDatetimeValue(start);
        }
        if (!end.equals("..")) {
            validateDatetimeValue(end);
        }

        // Both cannot be open
        if (start.equals("..") && end.equals("..")) {
            throw new ValidationException("Invalid interval: both start and end cannot be open");
        }
    }

    private void validateDatetimeValue(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return; // Null datetime is valid in some contexts
        }

        // Try ISO_INSTANT format
        try {
            DateTimeFormatter.ISO_INSTANT.parse(datetime);
            return;
        } catch (DateTimeParseException ignored) {
            // Try other formats
        }

        // Try ISO_DATE_TIME format
        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(datetime);
            return;
        } catch (DateTimeParseException ignored) {
            // Try other formats
        }

        // Try ISO_DATE format (date only)
        try {
            DateTimeFormatter.ISO_DATE.parse(datetime);
            return;
        } catch (DateTimeParseException ignored) {
            // Invalid format
        }

        // Check against pattern
        if (!ISO_8601_INSTANT.matcher(datetime).matches()) {
            throw new ValidationException("Invalid datetime format. Expected ISO 8601 format: " + datetime);
        }
    }

    /**
     * Validates that a limit is within acceptable bounds.
     *
     * @param limit    the limit value to validate
     * @param maxLimit the maximum allowed limit
     * @throws ValidationException if the limit is invalid
     */
    public void validateLimit(Integer limit, int maxLimit) {
        if (limit == null) {
            return; // Null is acceptable, will use default
        }

        if (limit < 1) {
            throw new ValidationException("Limit must be at least 1");
        }

        if (limit > maxLimit) {
            throw new ValidationException("Limit cannot exceed " + maxLimit);
        }
    }

    private void validateGeoJson(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            throw new ValidationException("GeoJSON cannot be null or empty");
        }

        String trimmed = geoJson.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new ValidationException("Invalid GeoJSON: must be a JSON object");
        }

        // Basic structure validation - more thorough validation happens during parsing
        if (!trimmed.contains("\"type\"")) {
            throw new ValidationException("Invalid GeoJSON: missing 'type' field");
        }

        if (!trimmed.contains("\"coordinates\"") && !trimmed.contains("\"geometries\"")) {
            throw new ValidationException("Invalid GeoJSON: missing 'coordinates' or 'geometries' field");
        }
    }
}
