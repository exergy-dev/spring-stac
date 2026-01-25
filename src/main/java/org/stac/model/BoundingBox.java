package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bounding box representation for spatial queries.
 * <p>
 * Supports both 2D (4 coordinates) and 3D (6 coordinates) bounding boxes.
 * Coordinates follow the GeoJSON/STAC convention: [west, south, east, north]
 * or [west, south, minZ, east, north, maxZ] for 3D.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {

    /**
     * Western boundary (minimum longitude). Range: -180 to 180.
     */
    private double west;

    /**
     * Southern boundary (minimum latitude). Range: -90 to 90.
     */
    private double south;

    /**
     * Eastern boundary (maximum longitude). Range: -180 to 180.
     */
    private double east;

    /**
     * Northern boundary (maximum latitude). Range: -90 to 90.
     */
    private double north;

    /**
     * Minimum elevation/altitude (optional, for 3D bounding boxes).
     */
    private Double minZ;

    /**
     * Maximum elevation/altitude (optional, for 3D bounding boxes).
     */
    private Double maxZ;

    /**
     * Converts the bounding box to an array of doubles.
     * <p>
     * Returns a 4-element array [west, south, east, north] for 2D bounding boxes,
     * or a 6-element array [west, south, minZ, east, north, maxZ] for 3D bounding boxes.
     *
     * @return array representation of the bounding box
     */
    public double[] toArray() {
        if (minZ != null && maxZ != null) {
            return new double[]{west, south, minZ, east, north, maxZ};
        }
        return new double[]{west, south, east, north};
    }

    /**
     * Converts the bounding box to a list of coordinates suitable for Elasticsearch.
     * <p>
     * Returns a list with two coordinate pairs: [[west, north], [east, south]]
     * representing the top-left and bottom-right corners of the bounding box.
     *
     * @return list of coordinate pairs for Elasticsearch geo_bounding_box query
     */
    public List<List<Double>> toCoordinatesList() {
        List<List<Double>> coordinates = new ArrayList<>();
        // Top-left corner [west, north]
        coordinates.add(Arrays.asList(west, north));
        // Bottom-right corner [east, south]
        coordinates.add(Arrays.asList(east, south));
        return coordinates;
    }

    /**
     * Checks if this is a 3D bounding box (has Z coordinates).
     *
     * @return true if both minZ and maxZ are defined
     */
    public boolean is3D() {
        return minZ != null && maxZ != null;
    }

    /**
     * Parses a bounding box string directly to a double array.
     * <p>
     * This is a convenience method for cases where a raw array is needed
     * instead of a typed BoundingBox object. Returns null for null/empty input
     * rather than throwing an exception.
     *
     * @param bbox comma-separated string of coordinates (e.g., "-180,-90,180,90")
     * @return double array of coordinates, or null if input is null/empty
     * @throws NumberFormatException if coordinates cannot be parsed
     */
    public static double[] parseToArray(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return null;
        }

        String[] parts = bbox.split(",");
        double[] bboxArray = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bboxArray[i] = Double.parseDouble(parts[i].trim());
        }
        return bboxArray;
    }

    /**
     * Parses a bounding box from a comma-separated string.
     * <p>
     * Accepts either 4 values (2D: west,south,east,north) or
     * 6 values (3D: west,south,minZ,east,north,maxZ).
     *
     * @param bbox comma-separated string of coordinates
     * @return parsed BoundingBox instance
     * @throws IllegalArgumentException if the string format is invalid
     */
    public static BoundingBox fromString(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            throw new IllegalArgumentException("Bounding box string cannot be null or empty");
        }

        String[] parts = bbox.split(",");
        if (parts.length != 4 && parts.length != 6) {
            throw new IllegalArgumentException(
                    "Bounding box must have 4 or 6 values, got " + parts.length);
        }

        try {
            BoundingBoxBuilder builder = BoundingBox.builder()
                    .west(Double.parseDouble(parts[0].trim()))
                    .south(Double.parseDouble(parts[1].trim()));

            if (parts.length == 4) {
                builder.east(Double.parseDouble(parts[2].trim()))
                        .north(Double.parseDouble(parts[3].trim()));
            } else {
                builder.minZ(Double.parseDouble(parts[2].trim()))
                        .east(Double.parseDouble(parts[3].trim()))
                        .north(Double.parseDouble(parts[4].trim()))
                        .maxZ(Double.parseDouble(parts[5].trim()));
            }

            BoundingBox result = builder.build();
            result.validate();
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid number format in bounding box: " + bbox, e);
        }
    }

    /**
     * Parses a bounding box from an array of doubles.
     *
     * @param coords array of 4 or 6 coordinate values
     * @return parsed BoundingBox instance
     * @throws IllegalArgumentException if the array length is invalid
     */
    public static BoundingBox fromArray(double[] coords) {
        if (coords == null) {
            throw new IllegalArgumentException("Coordinates array cannot be null");
        }
        if (coords.length != 4 && coords.length != 6) {
            throw new IllegalArgumentException(
                    "Bounding box must have 4 or 6 values, got " + coords.length);
        }

        BoundingBoxBuilder builder = BoundingBox.builder()
                .west(coords[0])
                .south(coords[1]);

        if (coords.length == 4) {
            builder.east(coords[2])
                    .north(coords[3]);
        } else {
            builder.minZ(coords[2])
                    .east(coords[3])
                    .north(coords[4])
                    .maxZ(coords[5]);
        }

        BoundingBox result = builder.build();
        result.validate();
        return result;
    }

    /**
     * Validates the bounding box coordinates.
     *
     * @throws IllegalArgumentException if coordinates are out of valid ranges
     */
    public void validate() {
        if (west < -180 || west > 180) {
            throw new IllegalArgumentException("West longitude must be between -180 and 180");
        }
        if (east < -180 || east > 180) {
            throw new IllegalArgumentException("East longitude must be between -180 and 180");
        }
        if (south < -90 || south > 90) {
            throw new IllegalArgumentException("South latitude must be between -90 and 90");
        }
        if (north < -90 || north > 90) {
            throw new IllegalArgumentException("North latitude must be between -90 and 90");
        }
        if (south > north) {
            throw new IllegalArgumentException("South latitude cannot be greater than north latitude");
        }
        if (minZ != null && maxZ != null && minZ > maxZ) {
            throw new IllegalArgumentException("minZ cannot be greater than maxZ");
        }
    }
}
