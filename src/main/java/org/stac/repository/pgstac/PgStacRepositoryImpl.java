package org.stac.repository.pgstac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stac.exception.NotFoundException;
import org.stac.exception.RepositoryException;
import org.stac.model.AggregationRequest;
import org.stac.model.BulkOperationResult;
import org.stac.model.Collection;
import org.stac.model.Context;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.ItemQueryCriteria;
import org.stac.model.Link;
import org.stac.model.SearchCriteria;
import org.stac.repository.StacRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * pgSTAC implementation of the StacRepository interface.
 * <p>
 * This implementation uses pgSTAC stored procedures for all data access operations.
 * pgSTAC is a PostgreSQL schema and functions for indexing and searching large
 * collections of STAC items.
 * <p>
 * Requires a PostgreSQL database with pgSTAC extension installed.
 *
 * @see <a href="https://github.com/stac-utils/pgstac">pgSTAC Documentation</a>
 * @see StacRepository
 */
@Slf4j
@RequiredArgsConstructor
public class PgStacRepositoryImpl implements StacRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PgStacFunctionMapper functionMapper;
    private final ItemRowMapper itemRowMapper;
    private final CollectionRowMapper collectionRowMapper;

    // ========================================================================
    // SQL Constants for pgSTAC stored procedures
    // Official pgstac functions use the pgstac schema
    // @see https://github.com/stac-utils/pgstac
    // ========================================================================

    private static final String SQL_SEARCH = "SELECT pgstac.search(CAST(? AS jsonb)) AS content";
    private static final String SQL_GET_ITEM = "SELECT pgstac.get_item(?, ?) AS content";
    private static final String SQL_CREATE_ITEM = "SELECT pgstac.create_item(CAST(? AS jsonb))";
    private static final String SQL_UPDATE_ITEM = "SELECT pgstac.update_item(CAST(? AS jsonb))";
    private static final String SQL_UPSERT_ITEM = "SELECT pgstac.upsert_item(CAST(? AS jsonb))";
    private static final String SQL_DELETE_ITEM = "SELECT pgstac.delete_item(?, ?)";

    private static final String SQL_GET_COLLECTION = "SELECT pgstac.get_collection(?) AS content";
    private static final String SQL_GET_COLLECTIONS = "SELECT pgstac.all_collections() AS collections_json";
    private static final String SQL_CREATE_COLLECTION = "SELECT pgstac.create_collection(CAST(? AS jsonb))";
    private static final String SQL_UPDATE_COLLECTION = "SELECT pgstac.update_collection(CAST(? AS jsonb))";
    private static final String SQL_UPSERT_COLLECTION = "SELECT pgstac.upsert_collection(CAST(? AS jsonb))";
    private static final String SQL_DELETE_COLLECTION = "SELECT pgstac.delete_collection(?)";

    private static final String SQL_COUNT_ITEMS = "SELECT count(*) FROM pgstac.items WHERE collection = ?";
    private static final String SQL_COUNT_COLLECTIONS = "SELECT count(*) FROM pgstac.collections";

    // ========================================================================
    // Collection Operations
    // ========================================================================

    @Override
    public Page<Collection> findAllCollections(Pageable pageable) {
        try {
            log.debug("Finding all collections with pagination: {}", pageable);

            // Get total count
            long totalCount = countCollections();

            // Get collections using pgSTAC function - returns JSON array
            String collectionsJson = jdbcTemplate.queryForObject(
                    SQL_GET_COLLECTIONS,
                    String.class
            );

            List<Collection> collections;
            if (collectionsJson == null || collectionsJson.isBlank() || collectionsJson.equals("null")) {
                collections = Collections.emptyList();
            } else {
                collections = collectionRowMapper.parseCollectionsFromJsonArray(collectionsJson);
            }

            // Apply pagination manually (pgSTAC all_collections() doesn't support pagination)
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), collections.size());

            List<Collection> pageContent = (start < collections.size())
                    ? collections.subList(start, end)
                    : Collections.emptyList();

            return new PageImpl<>(pageContent, pageable, totalCount);
        } catch (DataAccessException e) {
            log.error("Failed to find all collections: {}", e.getMessage());
            throw new RepositoryException("Failed to retrieve collections", e);
        } catch (Exception e) {
            log.error("Failed to parse collections: {}", e.getMessage());
            throw new RepositoryException("Failed to parse collections", e);
        }
    }

    @Override
    public Optional<Collection> findCollectionById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        try {
            log.debug("Finding collection by id: {}", id);
            Collection collection = jdbcTemplate.queryForObject(
                    SQL_GET_COLLECTION,
                    collectionRowMapper,
                    id
            );
            return Optional.ofNullable(collection);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Collection not found: {}", id);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find collection {}: {}", id, e.getMessage());
            throw new RepositoryException("Failed to retrieve collection", e);
        }
    }

    @Override
    public Collection saveCollection(Collection collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection cannot be null");
        }

        try {
            log.debug("Saving collection: {}", collection.getId());
            String collectionJson = functionMapper.buildCollectionJson(collection);

            // Use upsert_collection which handles both create and update
            // Official pgstac functions return VOID, so we execute without expecting a result
            jdbcTemplate.execute(
                    String.format("SELECT pgstac.upsert_collection(CAST('%s' AS jsonb))",
                            collectionJson.replace("'", "''"))
            );

            log.info("Successfully saved collection: {}", collection.getId());

            // Return the saved collection by fetching it back
            return findCollectionById(collection.getId()).orElse(collection);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize collection {}: {}", collection.getId(), e.getMessage());
            throw RepositoryException.save("Collection", e);
        } catch (DataAccessException e) {
            log.error("Failed to save collection {}: {}", collection.getId(), e.getMessage());
            throw RepositoryException.save("Collection", e);
        }
    }

    @Override
    public void deleteCollection(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }

        try {
            log.debug("Deleting collection: {}", id);

            // Verify collection exists
            if (findCollectionById(id).isEmpty()) {
                throw NotFoundException.collection(id);
            }

            // Official pgstac delete_collection returns VOID
            jdbcTemplate.execute(
                    String.format("SELECT pgstac.delete_collection('%s')", id.replace("'", "''"))
            );

            log.info("Successfully deleted collection: {}", id);
        } catch (NotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Failed to delete collection {}: {}", id, e.getMessage());
            throw RepositoryException.delete("Collection", id, e);
        }
    }

    @Override
    public long countCollections() {
        try {
            Long count = jdbcTemplate.queryForObject(SQL_COUNT_COLLECTIONS, Long.class);
            return count != null ? count : 0L;
        } catch (DataAccessException e) {
            log.error("Failed to count collections: {}", e.getMessage());
            throw new RepositoryException("Failed to count collections", e);
        }
    }

    // ========================================================================
    // Item Operations
    // ========================================================================

    @Override
    public Page<Item> findItemsByCollection(String collectionId, ItemQueryCriteria criteria,
                                            Pageable pageable) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }

        try {
            log.debug("Finding items in collection {} with criteria: {}", collectionId, criteria);

            // Build search criteria with collection constraint
            SearchCriteria searchCriteria = (criteria != null)
                    ? criteria.toSearchCriteria(collectionId)
                    : SearchCriteria.builder()
                            .collections(List.of(collectionId))
                            .limit(pageable.getPageSize())
                            .build();

            // Set limit from pageable if not already set
            if (searchCriteria.getLimit() == null) {
                searchCriteria = SearchCriteria.builder()
                        .collections(searchCriteria.getCollections())
                        .ids(searchCriteria.getIds())
                        .bbox(searchCriteria.getBbox())
                        .datetime(searchCriteria.getDatetime())
                        .intersects(searchCriteria.getIntersects())
                        .limit(pageable.getPageSize())
                        .token(searchCriteria.getToken())
                        .filter(searchCriteria.getFilter())
                        .sortBy(searchCriteria.getSortBy())
                        .fields(searchCriteria.getFields())
                        .query(searchCriteria.getQuery())
                        .build();
            }

            ItemCollection result = search(searchCriteria);
            List<Item> items = result.getFeatures() != null ? result.getFeatures() : Collections.emptyList();

            // Get total count
            long totalCount = result.getContext() != null && result.getContext().getMatched() != null
                    ? result.getContext().getMatched()
                    : countItemsByCollection(collectionId);

            return new PageImpl<>(items, pageable, totalCount);
        } catch (DataAccessException e) {
            log.error("Failed to find items in collection {}: {}", collectionId, e.getMessage());
            throw new RepositoryException("Failed to retrieve items", e);
        }
    }

    @Override
    public Optional<Item> findItemById(String collectionId, String itemId) {
        if (collectionId == null || collectionId.isBlank() || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }

        try {
            log.debug("Finding item {} in collection {}", itemId, collectionId);
            Item item = jdbcTemplate.queryForObject(
                    SQL_GET_ITEM,
                    itemRowMapper,
                    itemId,
                    collectionId
            );
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Item {} not found in collection {}", itemId, collectionId);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find item {} in collection {}: {}", itemId, collectionId, e.getMessage());
            throw new RepositoryException("Failed to retrieve item", e);
        }
    }

    @Override
    public Item saveItem(String collectionId, Item item) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        try {
            log.debug("Saving item {} to collection {}", item.getId(), collectionId);

            // Ensure the item has the correct collection reference
            item.setCollection(collectionId);
            String itemJson = functionMapper.buildItemJson(item);

            // Use upsert_item which handles both create and update
            // Official pgstac functions return VOID
            jdbcTemplate.execute(
                    String.format("SELECT pgstac.upsert_item(CAST('%s' AS jsonb))",
                            itemJson.replace("'", "''"))
            );

            log.info("Successfully saved item {} to collection {}", item.getId(), collectionId);

            // Return the saved item by fetching it back
            return findItemById(collectionId, item.getId()).orElse(item);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize item {}: {}", item.getId(), e.getMessage());
            throw RepositoryException.save("Item", e);
        } catch (DataAccessException e) {
            log.error("Failed to save item {} to collection {}: {}",
                    item.getId(), collectionId, e.getMessage());
            throw RepositoryException.save("Item", e);
        }
    }

    @Override
    public void deleteItem(String collectionId, String itemId) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }

        try {
            log.debug("Deleting item {} from collection {}", itemId, collectionId);

            // Verify item exists
            if (findItemById(collectionId, itemId).isEmpty()) {
                throw NotFoundException.item(collectionId, itemId);
            }

            // Official pgstac delete_item returns VOID
            jdbcTemplate.execute(
                    String.format("SELECT pgstac.delete_item('%s', '%s')",
                            itemId.replace("'", "''"),
                            collectionId.replace("'", "''"))
            );

            log.info("Successfully deleted item {} from collection {}", itemId, collectionId);
        } catch (NotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Failed to delete item {} from collection {}: {}",
                    itemId, collectionId, e.getMessage());
            throw RepositoryException.delete("Item", itemId, e);
        }
    }

    @Override
    public long countItemsByCollection(String collectionId) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }

        try {
            Long count = jdbcTemplate.queryForObject(SQL_COUNT_ITEMS, Long.class, collectionId);
            return count != null ? count : 0L;
        } catch (DataAccessException e) {
            log.error("Failed to count items in collection {}: {}", collectionId, e.getMessage());
            throw new RepositoryException("Failed to count items", e);
        }
    }

    // ========================================================================
    // Search Operations
    // ========================================================================

    @Override
    public ItemCollection search(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = SearchCriteria.defaults().build();
        }

        try {
            log.debug("Executing search with criteria: {}", criteria);
            String searchJson = functionMapper.buildSearchJson(criteria);

            String resultJson = jdbcTemplate.queryForObject(
                    SQL_SEARCH,
                    String.class,
                    searchJson
            );

            ItemCollection result = parseSearchResult(resultJson, criteria.getLimit());

            // If matched count is not provided by pgSTAC, compute it
            if (result.getContext() != null && result.getContext().getMatched() == null) {
                Integer matchedCount = computeMatchedCount(criteria);
                if (matchedCount != null) {
                    Context updatedContext = Context.builder()
                            .returned(result.getContext().getReturned())
                            .limit(result.getContext().getLimit())
                            .matched(matchedCount)
                            .build();
                    result = ItemCollection.builder()
                            .type(result.getType())
                            .features(result.getFeatures())
                            .links(result.getLinks())
                            .context(updatedContext)
                            .numberReturned(result.getNumberReturned())
                            .numberMatched(matchedCount)
                            .build();
                }
            }

            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to build search JSON: {}", e.getMessage());
            throw RepositoryException.search("Failed to build search request", e);
        } catch (DataAccessException e) {
            log.error("Failed to execute search: {}", e.getMessage());
            throw RepositoryException.search("Search failed", e);
        } catch (Exception e) {
            log.error("Failed to parse search results: {}", e.getMessage());
            throw RepositoryException.search("Failed to parse search results", e);
        }
    }

    /**
     * Computes the total matched count for a search criteria.
     * Uses a count query when pgSTAC doesn't return the matched count in search results.
     *
     * @param criteria the search criteria
     * @return the total matched count, or null if it can't be computed
     */
    private Integer computeMatchedCount(SearchCriteria criteria) {
        try {
            // For simple collection-based searches, use countItemsByCollection
            if (criteria.getCollections() != null && criteria.getCollections().size() == 1
                    && criteria.getBbox() == null && criteria.getDatetime() == null
                    && criteria.getIntersects() == null && criteria.getFilter() == null
                    && criteria.getIds() == null) {
                return (int) countItemsByCollection(criteria.getCollections().get(0));
            }

            // For more complex searches, use pgSTAC context function if available
            String searchJson = functionMapper.buildSearchJson(criteria);
            String countSql = "SELECT count(*) FROM pgstac.search_query(CAST(? AS jsonb))";
            Long count = jdbcTemplate.queryForObject(countSql, Long.class, searchJson);
            return count != null ? count.intValue() : null;
        } catch (Exception e) {
            log.debug("Could not compute matched count: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses the search result JSON from pgSTAC into an ItemCollection.
     *
     * @param resultJson the JSON result from pgSTAC search function
     * @param requestLimit the limit value from the original search request (may be null)
     */
    private ItemCollection parseSearchResult(String resultJson, Integer requestLimit) throws Exception {
        if (resultJson == null || resultJson.isBlank()) {
            return ItemCollection.builder()
                    .features(Collections.emptyList())
                    .build();
        }

        JsonNode rootNode = objectMapper.readTree(resultJson);

        ItemCollection.ItemCollectionBuilder builder = ItemCollection.builder();

        // Type
        if (rootNode.has("type")) {
            builder.type(rootNode.get("type").asText());
        }

        // Features
        if (rootNode.has("features")) {
            List<Item> items = itemRowMapper.parseItemsFromJsonArray(
                    rootNode.get("features").toString()
            );
            builder.features(items);
        } else {
            builder.features(Collections.emptyList());
        }

        // Links
        if (rootNode.has("links")) {
            List<Link> links = objectMapper.readValue(
                    rootNode.get("links").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Link.class)
            );
            builder.links(links);
        }

        // Context - check both STAC Context Extension format and pgSTAC/OGC format
        var contextBuilder = Context.builder();
        boolean hasContext = false;

        if (rootNode.has("context")) {
            JsonNode contextNode = rootNode.get("context");
            hasContext = true;

            if (contextNode.has("returned")) {
                contextBuilder.returned(contextNode.get("returned").asInt());
            }
            if (contextNode.has("limit")) {
                contextBuilder.limit(contextNode.get("limit").asInt());
            }
            if (contextNode.has("matched")) {
                contextBuilder.matched(contextNode.get("matched").asInt());
            }
        }

        // Number matched/returned (OGC API style / pgSTAC style)
        if (rootNode.has("numberMatched")) {
            int matched = rootNode.get("numberMatched").asInt();
            builder.numberMatched(matched);
            contextBuilder.matched(matched);
            hasContext = true;
        }
        if (rootNode.has("numberReturned")) {
            int returned = rootNode.get("numberReturned").asInt();
            builder.numberReturned(returned);
            contextBuilder.returned(returned);
            hasContext = true;
        }

        // If we have any context info, build and set it
        if (hasContext) {
            // Set limit from the original request if not already set from response
            if (requestLimit != null) {
                contextBuilder.limit(requestLimit);
            }
            builder.context(contextBuilder.build());
        }

        return builder.build();
    }

    // ========================================================================
    // Aggregation Operations
    // ========================================================================

    @Override
    public Map<String, Object> aggregate(AggregationRequest request) {
        if (request == null || request.getAggregationCount() == 0) {
            return Collections.emptyMap();
        }

        log.debug("Executing aggregation: {}", request);
        Map<String, Object> results = new HashMap<>();

        for (String field : request.getAggregations()) {
            try {
                Map<String, Object> fieldResult = aggregateField(field, request);
                if (fieldResult != null) {
                    results.put(field, fieldResult);
                }
            } catch (Exception e) {
                log.warn("Failed to aggregate field {}: {}", field, e.getMessage());
            }
        }

        return results;
    }

    /**
     * Aggregates a single field using SQL queries on pgSTAC items table.
     */
    private Map<String, Object> aggregateField(String field, AggregationRequest request) {
        // Determine the field path and aggregation type
        String fieldPath = mapFieldToJsonPath(field);
        String collectionFilter = buildCollectionFilter(request.getCollections());

        // Handle special aggregation fields
        return switch (field) {
            case "collection" -> aggregateCollectionTerms(collectionFilter);
            case "datetime" -> aggregateDatetimeHistogram(collectionFilter);
            case "geometry" -> aggregateGeometryBounds(collectionFilter);
            default -> aggregatePropertyTerms(fieldPath, collectionFilter);
        };
    }

    /**
     * Maps field names to JSON path expressions for PostgreSQL.
     */
    private String mapFieldToJsonPath(String field) {
        if (field.startsWith("properties.")) {
            // e.g., properties.eo:cloud_cover -> content->'properties'->>'eo:cloud_cover'
            String propName = field.substring("properties.".length());
            return "content->'properties'->>'" + propName.replace("'", "''") + "'";
        } else if (field.equals("datetime")) {
            return "content->'properties'->>'datetime'";
        } else if (field.equals("collection")) {
            return "collection";
        } else {
            // Direct field access
            return "content->>'" + field.replace("'", "''") + "'";
        }
    }

    /**
     * Builds SQL WHERE clause for collection filtering.
     */
    private String buildCollectionFilter(List<String> collections) {
        if (collections == null || collections.isEmpty()) {
            return "";
        }
        String inClause = collections.stream()
                .map(c -> "'" + c.replace("'", "''") + "'")
                .collect(java.util.stream.Collectors.joining(","));
        return " WHERE collection IN (" + inClause + ")";
    }

    /**
     * Aggregates collection field to get term counts.
     */
    private Map<String, Object> aggregateCollectionTerms(String collectionFilter) {
        try {
            String sql = "SELECT collection as key, count(*) as count FROM pgstac.items" +
                    collectionFilter + " GROUP BY collection ORDER BY count DESC LIMIT 100";

            List<Map<String, Object>> buckets = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> bucket = new HashMap<>();
                bucket.put("key", rs.getString("key"));
                bucket.put("count", rs.getLong("count"));
                return bucket;
            });

            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);
            return result;
        } catch (DataAccessException e) {
            log.warn("Failed to aggregate collections: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Aggregates datetime field to get histogram.
     */
    private Map<String, Object> aggregateDatetimeHistogram(String collectionFilter) {
        try {
            // Get date range and monthly histogram
            String sql = """
                SELECT
                    date_trunc('month', (content->'properties'->>'datetime')::timestamptz) as key,
                    count(*) as count,
                    min((content->'properties'->>'datetime')::timestamptz) as min_date,
                    max((content->'properties'->>'datetime')::timestamptz) as max_date
                FROM pgstac.items
                """ + collectionFilter + """
                GROUP BY date_trunc('month', (content->'properties'->>'datetime')::timestamptz)
                ORDER BY key
                LIMIT 100
                """;

            List<Map<String, Object>> buckets = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> bucket = new HashMap<>();
                java.sql.Timestamp key = rs.getTimestamp("key");
                if (key != null) {
                    bucket.put("key", key.toInstant().toString());
                }
                bucket.put("count", rs.getLong("count"));
                return bucket;
            });

            // Get overall min/max
            String rangeSql = """
                SELECT
                    min((content->'properties'->>'datetime')::timestamptz) as min_date,
                    max((content->'properties'->>'datetime')::timestamptz) as max_date
                FROM pgstac.items
                """ + collectionFilter;

            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);

            jdbcTemplate.query(rangeSql, rs -> {
                java.sql.Timestamp minDate = rs.getTimestamp("min_date");
                java.sql.Timestamp maxDate = rs.getTimestamp("max_date");
                if (minDate != null) {
                    result.put("min", minDate.toInstant().toString());
                }
                if (maxDate != null) {
                    result.put("max", maxDate.toInstant().toString());
                }
            });

            return result;
        } catch (DataAccessException e) {
            log.warn("Failed to aggregate datetime: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Aggregates geometry field to get bounding box.
     */
    private Map<String, Object> aggregateGeometryBounds(String collectionFilter) {
        try {
            String sql = """
                SELECT
                    ST_XMin(ST_Extent(geometry)) as xmin,
                    ST_YMin(ST_Extent(geometry)) as ymin,
                    ST_XMax(ST_Extent(geometry)) as xmax,
                    ST_YMax(ST_Extent(geometry)) as ymax,
                    count(*) as count
                FROM pgstac.items
                """ + collectionFilter;

            Map<String, Object> result = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                double xmin = rs.getDouble("xmin");
                double ymin = rs.getDouble("ymin");
                double xmax = rs.getDouble("xmax");
                double ymax = rs.getDouble("ymax");
                if (!rs.wasNull()) {
                    result.put("bounds", new double[]{xmin, ymin, xmax, ymax});
                }
                result.put("count", rs.getLong("count"));
            });

            return result;
        } catch (DataAccessException e) {
            log.warn("Failed to aggregate geometry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Aggregates a property field to get term counts.
     */
    private Map<String, Object> aggregatePropertyTerms(String fieldPath, String collectionFilter) {
        try {
            String sql = String.format("""
                SELECT %s as key, count(*) as count
                FROM pgstac.items
                %s
                AND %s IS NOT NULL
                GROUP BY %s
                ORDER BY count DESC
                LIMIT 100
                """, fieldPath,
                collectionFilter.isEmpty() ? "WHERE 1=1" : collectionFilter,
                fieldPath, fieldPath);

            List<Map<String, Object>> buckets = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> bucket = new HashMap<>();
                bucket.put("key", rs.getString("key"));
                bucket.put("count", rs.getLong("count"));
                return bucket;
            });

            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);
            return result;
        } catch (DataAccessException e) {
            log.warn("Failed to aggregate property {}: {}", fieldPath, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // Bulk Operations
    // ========================================================================

    @Override
    public BulkOperationResult bulkInsert(String collectionId, List<Item> items) {
        return bulkUpsert(collectionId, items, "insert");
    }

    @Override
    public BulkOperationResult bulkUpdate(String collectionId, List<Item> items) {
        return bulkUpsert(collectionId, items, "update");
    }

    /**
     * Internal method that performs bulk upsert operations.
     * Both insert and update use pgSTAC's upsert_item function.
     */
    private BulkOperationResult bulkUpsert(String collectionId, List<Item> items, String operation) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            return BulkOperationResult.empty();
        }

        log.debug("Bulk {} {} items in collection {}", operation, items.size(), collectionId);
        BulkOperationResult result = BulkOperationResult.builder()
                .items(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        for (Item item : items) {
            try {
                item.setCollection(collectionId);
                String itemJson = functionMapper.buildItemJson(item);

                // Use upsert_item for both insert and update (official pgstac returns VOID)
                jdbcTemplate.execute(
                        String.format("SELECT pgstac.upsert_item(CAST('%s' AS jsonb))",
                                itemJson.replace("'", "''"))
                );
                result.addSuccess(item.getId());
            } catch (Exception e) {
                log.warn("Failed to {} item {}: {}", operation, item.getId(), e.getMessage());
                result.addFailure(String.format("Item %s: %s", item.getId(), e.getMessage()));
            }
        }

        log.info("Bulk {} completed: {} succeeded, {} failed",
                operation, result.getSuccess(), result.getFailed());
        return result;
    }

    @Override
    public BulkOperationResult bulkDelete(String collectionId, List<String> itemIds) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("Collection ID cannot be null or empty");
        }
        if (itemIds == null || itemIds.isEmpty()) {
            return BulkOperationResult.empty();
        }

        log.debug("Bulk deleting {} items from collection {}", itemIds.size(), collectionId);
        BulkOperationResult result = BulkOperationResult.builder()
                .items(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        for (String itemId : itemIds) {
            try {
                // Official pgstac delete_item returns VOID
                jdbcTemplate.execute(
                        String.format("SELECT pgstac.delete_item('%s', '%s')",
                                itemId.replace("'", "''"),
                                collectionId.replace("'", "''"))
                );
                result.addSuccess(itemId);
            } catch (Exception e) {
                log.warn("Failed to delete item {}: {}", itemId, e.getMessage());
                result.addFailure(String.format("Item %s: %s", itemId, e.getMessage()));
            }
        }

        log.info("Bulk delete completed: {} succeeded, {} failed",
                result.getSuccess(), result.getFailed());
        return result;
    }
}
