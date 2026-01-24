package org.stac.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.stac.model.Asset;
import org.stac.model.BoundingBox;
import org.stac.model.Collection;
import org.stac.model.Extent;
import org.stac.model.Item;
import org.stac.model.Link;
import org.stac.model.SearchCriteria;
import org.stac.model.SpatialExtent;
import org.stac.model.TemporalExtent;

import java.time.Instant;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixtures helper class for creating test STAC objects.
 * <p>
 * Provides factory methods for creating test Collections and Items
 * with sensible defaults that can be customized as needed.
 */
public final class TestFixtures {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private TestFixtures() {
        // Utility class
    }

    /**
     * Creates a test STAC Collection with default values.
     *
     * @param id the collection ID
     * @return a fully populated test collection
     */
    public static Collection createTestCollection(String id) {
        return createTestCollection(id, "Test Collection: " + id, "Test description for " + id);
    }

    /**
     * Creates a test STAC Collection with custom title and description.
     *
     * @param id          the collection ID
     * @param title       the collection title
     * @param description the collection description
     * @return a fully populated test collection
     */
    public static Collection createTestCollection(String id, String title, String description) {
        SpatialExtent spatialExtent = SpatialExtent.builder()
                .bbox(List.of(new double[]{-180.0, -90.0, 180.0, 90.0}))
                .build();

        TemporalExtent temporalExtent = TemporalExtent.builder()
                .interval(List.of(Arrays.asList("2020-01-01T00:00:00Z", null)))
                .build();

        Extent extent = Extent.builder()
                .spatial(spatialExtent)
                .temporal(temporalExtent)
                .build();

        Link selfLink = Link.builder()
                .rel("self")
                .href("http://localhost:8080/collections/" + id)
                .type("application/json")
                .build();

        Link itemsLink = Link.builder()
                .rel("items")
                .href("http://localhost:8080/collections/" + id + "/items")
                .type("application/geo+json")
                .build();

        return Collection.builder()
                .id(id)
                .type("Collection")
                .stacVersion("1.0.0")
                .title(title)
                .description(description)
                .license("proprietary")
                .keywords(List.of("test", "stac", id))
                .extent(extent)
                .links(List.of(selfLink, itemsLink))
                .build();
    }

    /**
     * Creates a test STAC Item with default geometry at (0, 0).
     *
     * @param id           the item ID
     * @param collectionId the collection ID this item belongs to
     * @return a fully populated test item
     */
    public static Item createTestItem(String id, String collectionId) {
        return createTestItem(id, collectionId, 0.0, 0.0);
    }

    /**
     * Creates a test STAC Item with custom point geometry.
     *
     * @param id           the item ID
     * @param collectionId the collection ID this item belongs to
     * @param longitude    the longitude coordinate
     * @param latitude     the latitude coordinate
     * @return a fully populated test item
     */
    public static Item createTestItem(String id, String collectionId, double longitude, double latitude) {
        // Create a small polygon around the point
        Polygon geometry = createPolygonAroundPoint(longitude, latitude, 0.1);

        double[] bbox = new double[]{
                longitude - 0.1, latitude - 0.1,
                longitude + 0.1, latitude + 0.1
        };

        Map<String, Object> properties = new HashMap<>();
        properties.put("datetime", Instant.now().toString());
        properties.put("title", "Test Item: " + id);
        properties.put("description", "Test item for integration testing");

        Link selfLink = Link.builder()
                .rel("self")
                .href("http://localhost:8080/collections/" + collectionId + "/items/" + id)
                .type("application/geo+json")
                .build();

        Link collectionLink = Link.builder()
                .rel("collection")
                .href("http://localhost:8080/collections/" + collectionId)
                .type("application/json")
                .build();

        Asset thumbnailAsset = Asset.builder()
                .href("http://example.com/thumbnail/" + id + ".png")
                .type("image/png")
                .title("Thumbnail")
                .roles(List.of("thumbnail"))
                .build();

        return Item.builder()
                .id(id)
                .type("Feature")
                .stacVersion("1.0.0")
                .collection(collectionId)
                .geometry(geometry)
                .bbox(bbox)
                .properties(properties)
                .links(List.of(selfLink, collectionLink))
                .assets(Map.of("thumbnail", thumbnailAsset))
                .build();
    }

    /**
     * Creates a test STAC Item with a specific datetime.
     *
     * @param id           the item ID
     * @param collectionId the collection ID this item belongs to
     * @param datetime     the datetime in ISO 8601 format
     * @return a fully populated test item
     */
    public static Item createTestItemWithDatetime(String id, String collectionId, String datetime) {
        Item item = createTestItem(id, collectionId);
        item.getProperties().put("datetime", datetime);
        return item;
    }

    /**
     * Creates a test STAC Item with a specific property.
     *
     * @param id            the item ID
     * @param collectionId  the collection ID this item belongs to
     * @param propertyName  the property name to set
     * @param propertyValue the property value to set
     * @return a fully populated test item
     */
    public static Item createTestItemWithProperty(String id, String collectionId,
                                                   String propertyName, Object propertyValue) {
        Item item = createTestItem(id, collectionId);
        item.getProperties().put(propertyName, propertyValue);
        return item;
    }

    /**
     * Creates a test STAC Item with multiple properties.
     *
     * @param id              the item ID
     * @param collectionId    the collection ID this item belongs to
     * @param extraProperties additional properties to add
     * @return a fully populated test item
     */
    public static Item createTestItemWithProperties(String id, String collectionId,
                                                     Map<String, Object> extraProperties) {
        Item item = createTestItem(id, collectionId);
        item.getProperties().putAll(extraProperties);
        return item;
    }

    /**
     * Creates a test STAC Item with a polygon geometry.
     *
     * @param id           the item ID
     * @param collectionId the collection ID this item belongs to
     * @param polygon      the polygon geometry
     * @return a fully populated test item
     */
    public static Item createTestItemWithPolygon(String id, String collectionId, Polygon polygon) {
        Item item = createTestItem(id, collectionId);
        item.setGeometry(polygon);

        // Update bbox based on polygon envelope
        org.locationtech.jts.geom.Envelope envelope = polygon.getEnvelopeInternal();
        item.setBbox(new double[]{
                envelope.getMinX(), envelope.getMinY(),
                envelope.getMaxX(), envelope.getMaxY()
        });

        return item;
    }

    /**
     * Creates a small polygon around a point.
     *
     * @param longitude the center longitude
     * @param latitude  the center latitude
     * @param size      the half-size of the polygon in degrees
     * @return a polygon geometry
     */
    public static Polygon createPolygonAroundPoint(double longitude, double latitude, double size) {
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(longitude - size, latitude - size),
                new Coordinate(longitude + size, latitude - size),
                new Coordinate(longitude + size, latitude + size),
                new Coordinate(longitude - size, latitude + size),
                new Coordinate(longitude - size, latitude - size)  // Close the ring
        };
        return GEOMETRY_FACTORY.createPolygon(coordinates);
    }

    /**
     * Creates a point geometry.
     *
     * @param longitude the longitude coordinate
     * @param latitude  the latitude coordinate
     * @return a point geometry
     */
    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Creates a bounding box around a point.
     *
     * @param longitude the center longitude
     * @param latitude  the center latitude
     * @param size      the half-size of the bounding box in degrees
     * @return a double array representing [west, south, east, north]
     */
    public static double[] createBbox(double longitude, double latitude, double size) {
        return new double[]{
                longitude - size, latitude - size,
                longitude + size, latitude + size
        };
    }

    /**
     * Creates a list of test items for bulk operations.
     *
     * @param collectionId the collection ID
     * @param count        the number of items to create
     * @param idPrefix     the prefix for item IDs
     * @return a list of test items
     */
    public static List<Item> createTestItems(String collectionId, int count, String idPrefix) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> createTestItem(idPrefix + "-" + i, collectionId,
                        -180 + (i * (360.0 / count)),  // Spread items across longitudes
                        0.0))
                .toList();
    }

    /**
     * Creates a test STAC Item with a default polygon geometry.
     *
     * @param id           the item ID
     * @param collectionId the collection ID this item belongs to
     * @return a fully populated test item with polygon geometry
     */
    public static Item createTestItemWithPolygon(String id, String collectionId) {
        Polygon polygon = createPolygonAroundPoint(0.0, 0.0, 1.0);
        return createTestItemWithPolygon(id, collectionId, polygon);
    }

    /**
     * Creates a SearchCriteria with a bounding box filter.
     *
     * @param west  the western longitude
     * @param south the southern latitude
     * @param east  the eastern longitude
     * @param north the northern latitude
     * @return SearchCriteria with the specified bounding box
     */
    public static SearchCriteria createSearchCriteriaWithBbox(double west, double south, double east, double north) {
        BoundingBox bbox = BoundingBox.builder()
                .west(west)
                .south(south)
                .east(east)
                .north(north)
                .build();
        return SearchCriteria.builder()
                .bbox(bbox)
                .build();
    }

    /**
     * Creates a SearchCriteria with a datetime range filter.
     *
     * @param start the start datetime in ISO 8601 format
     * @param end   the end datetime in ISO 8601 format
     * @return SearchCriteria with the specified datetime range
     */
    public static SearchCriteria createSearchCriteriaWithDatetime(String start, String end) {
        TemporalExtent temporal = TemporalExtent.builder()
                .interval(List.of(List.of(start, end)))
                .build();
        return SearchCriteria.builder()
                .datetime(temporal)
                .build();
    }

    /**
     * Creates a SearchCriteria with a collections filter.
     *
     * @param collections the collection IDs to filter by
     * @return SearchCriteria with the specified collections
     */
    public static SearchCriteria createSearchCriteriaWithCollections(String... collections) {
        return SearchCriteria.builder()
                .collections(Arrays.asList(collections))
                .build();
    }

    /**
     * Creates a default SearchCriteria with no filters.
     *
     * @return SearchCriteria with default values
     */
    public static SearchCriteria createTestSearchCriteria() {
        return SearchCriteria.builder().build();
    }
}
