package org.stac.repository.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.stac.exception.NotFoundException;
import org.stac.exception.RepositoryException;
import org.stac.model.AggregationRequest;
import org.stac.model.BoundingBox;
import org.stac.model.BulkOperationResult;
import org.stac.model.Collection;
import org.stac.model.Context;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.ItemQueryCriteria;
import org.stac.model.SearchCriteria;
import org.stac.model.SortBy;
import org.stac.model.SortDirection;
import org.stac.model.TemporalExtent;
import org.stac.repository.StacRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch implementation of the STAC Repository.
 * <p>
 * This implementation uses the Elasticsearch Java client to store and query
 * STAC collections and items. It supports full STAC API functionality including
 * spatial, temporal, and attribute filtering, as well as pagination and sorting.
 */
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchStacRepositoryImpl implements StacRepository {

    private static final String ITEMS_INDEX_PATTERN = "stac-items-*";
    private static final String COLLECTIONS_INDEX = "stac-collections";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 10000;

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final Cql2ToElasticsearchConverter cql2Converter;
    private final ElasticsearchItemMapper itemMapper;

    // ========================================================================
    // Collection Operations
    // ========================================================================

    @Override
    public Page<Collection> findAllCollections(Pageable pageable) {
        try {
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(COLLECTIONS_INDEX)
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize());

            // Add sorting if specified
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    requestBuilder.sort(s -> s.field(f -> f
                            .field(order.getProperty())
                            .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)));
                });
            }

            SearchResponse<ObjectNode> response = elasticsearchClient.search(
                    requestBuilder.build(), ObjectNode.class);

            List<Collection> collections = new ArrayList<>();
            for (Hit<ObjectNode> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    collections.add(objectMapper.treeToValue(hit.source(), Collection.class));
                }
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(collections, pageable, total);

        } catch (IOException e) {
            throw RepositoryException.search("Failed to list collections", e);
        }
    }

    @Override
    public Optional<Collection> findCollectionById(String id) {
        try {
            GetRequest request = GetRequest.of(r -> r.index(COLLECTIONS_INDEX).id(id));
            GetResponse<ObjectNode> response = elasticsearchClient.get(request, ObjectNode.class);

            if (response.found() && response.source() != null) {
                return Optional.of(objectMapper.treeToValue(response.source(), Collection.class));
            }
            return Optional.empty();

        } catch (IOException e) {
            throw new RepositoryException("Failed to get collection: " + id, e);
        }
    }

    @Override
    public Collection saveCollection(Collection collection) {
        try {
            Map<String, Object> doc = objectMapper.convertValue(collection, Map.class);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(r -> r
                    .index(COLLECTIONS_INDEX)
                    .id(collection.getId())
                    .document(doc));

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("Saved collection {} with result {}", collection.getId(), response.result());

            return collection;

        } catch (IOException e) {
            throw RepositoryException.save("collection", e);
        }
    }

    @Override
    public void deleteCollection(String id) {
        try {
            // First check if collection exists
            if (findCollectionById(id).isEmpty()) {
                throw new NotFoundException("Collection not found: " + id);
            }

            DeleteRequest request = DeleteRequest.of(r -> r.index(COLLECTIONS_INDEX).id(id));
            DeleteResponse response = elasticsearchClient.delete(request);

            if (response.result().name().equals("NOT_FOUND")) {
                throw new NotFoundException("Collection not found: " + id);
            }

            log.debug("Deleted collection {} with result {}", id, response.result());

        } catch (NotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw RepositoryException.delete("collection", id, e);
        }
    }

    @Override
    public long countCollections() {
        try {
            CountRequest request = CountRequest.of(r -> r.index(COLLECTIONS_INDEX));
            CountResponse response = elasticsearchClient.count(request);
            return response.count();

        } catch (IOException e) {
            throw new RepositoryException("Failed to count collections", e);
        }
    }

    // ========================================================================
    // Item Operations
    // ========================================================================

    @Override
    public Page<Item> findItemsByCollection(String collectionId, ItemQueryCriteria criteria, Pageable pageable) {
        // Convert to search criteria and delegate to search
        SearchCriteria searchCriteria = criteria != null
                ? criteria.toSearchCriteria(collectionId)
                : SearchCriteria.builder().collections(List.of(collectionId)).build();

        // Set limit from pageable
        if (criteria == null || criteria.getLimit() == null) {
            searchCriteria.setLimit(pageable.getPageSize());
        }

        try {
            // Build the query
            Query query = buildSearchQuery(searchCriteria);

            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(getItemIndexForCollection(collectionId))
                    .query(query)
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize());

            // Add sorting
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    requestBuilder.sort(s -> s.field(f -> f
                            .field(order.getProperty())
                            .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)));
                });
            }

            SearchResponse<ObjectNode> response = elasticsearchClient.search(
                    requestBuilder.build(), ObjectNode.class);

            List<Item> items = new ArrayList<>();
            for (Hit<ObjectNode> hit : response.hits().hits()) {
                items.add(itemMapper.toSearchHit(hit));
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(items, pageable, total);

        } catch (IOException e) {
            throw RepositoryException.search("Failed to find items by collection: " + collectionId, e);
        }
    }

    @Override
    public Optional<Item> findItemById(String collectionId, String itemId) {
        try {
            String index = getItemIndexForCollection(collectionId);
            GetRequest request = GetRequest.of(r -> r.index(index).id(itemId));
            GetResponse<ObjectNode> response = elasticsearchClient.get(request, ObjectNode.class);

            if (response.found() && response.source() != null) {
                return Optional.of(itemMapper.fromObjectNode(response.source()));
            }
            return Optional.empty();

        } catch (IOException e) {
            throw new RepositoryException("Failed to get item: " + itemId, e);
        }
    }

    @Override
    public Item saveItem(String collectionId, Item item) {
        try {
            // Ensure item has collection set
            if (item.getCollection() == null) {
                item.setCollection(collectionId);
            }

            Map<String, Object> doc = itemMapper.toDocument(item);
            String index = getItemIndexForCollection(collectionId);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(r -> r
                    .index(index)
                    .id(item.getId())
                    .document(doc));

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("Saved item {} to collection {} with result {}",
                    item.getId(), collectionId, response.result());

            return item;

        } catch (IOException e) {
            throw RepositoryException.save("item", e);
        }
    }

    @Override
    public void deleteItem(String collectionId, String itemId) {
        try {
            String index = getItemIndexForCollection(collectionId);
            DeleteRequest request = DeleteRequest.of(r -> r.index(index).id(itemId));
            DeleteResponse response = elasticsearchClient.delete(request);

            if (response.result().name().equals("NOT_FOUND")) {
                throw new NotFoundException("Item not found: " + itemId);
            }

            log.debug("Deleted item {} from collection {} with result {}",
                    itemId, collectionId, response.result());

        } catch (NotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw RepositoryException.delete("item", itemId, e);
        }
    }

    @Override
    public long countItemsByCollection(String collectionId) {
        try {
            Query query = Query.of(q -> q.term(t -> t.field("collection").value(collectionId)));
            CountRequest request = CountRequest.of(r -> r
                    .index(getItemIndexForCollection(collectionId))
                    .query(query));

            CountResponse response = elasticsearchClient.count(request);
            return response.count();

        } catch (IOException e) {
            throw new RepositoryException("Failed to count items in collection: " + collectionId, e);
        }
    }

    // ========================================================================
    // Search Operations
    // ========================================================================

    @Override
    public ItemCollection search(SearchCriteria criteria) {
        try {
            Query query = buildSearchQuery(criteria);
            int limit = criteria.getEffectiveLimit(DEFAULT_PAGE_SIZE);
            limit = Math.min(limit, MAX_PAGE_SIZE);

            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(ITEMS_INDEX_PATTERN)
                    .query(query)
                    .size(limit);

            // Handle pagination with search_after
            if (criteria.isPaginated()) {
                List<FieldValue> searchAfter = decodeSearchAfterToken(criteria.getToken());
                if (searchAfter != null && !searchAfter.isEmpty()) {
                    requestBuilder.searchAfter(searchAfter);
                }
            }

            // Add sorting
            List<SortOptions> sortOptions = buildSortOptions(criteria.getSortBy());
            if (!sortOptions.isEmpty()) {
                requestBuilder.sort(sortOptions);
            } else {
                // Default sort by datetime desc, id asc for consistent pagination
                requestBuilder.sort(s -> s.field(f -> f
                        .field("properties.datetime")
                        .order(SortOrder.Desc)));
                requestBuilder.sort(s -> s.field(f -> f.field("id").order(SortOrder.Asc)));
            }

            // Request total hits for context
            requestBuilder.trackTotalHits(t -> t.enabled(true));

            SearchResponse<ObjectNode> response = elasticsearchClient.search(
                    requestBuilder.build(), ObjectNode.class);

            // Convert hits to items
            List<Item> items = new ArrayList<>();
            for (Hit<ObjectNode> hit : response.hits().hits()) {
                Item item = itemMapper.toSearchHit(hit);
                if (item != null) {
                    items.add(item);
                }
            }

            // Build pagination token for next page
            String nextToken = null;
            if (!response.hits().hits().isEmpty() && items.size() == limit) {
                Hit<ObjectNode> lastHit = response.hits().hits().get(response.hits().hits().size() - 1);
                if (lastHit.sort() != null && !lastHit.sort().isEmpty()) {
                    nextToken = encodeSearchAfterToken(lastHit.sort());
                }
            }

            // Build context
            long totalMatched = response.hits().total() != null ? response.hits().total().value() : 0;
            Context context = Context.builder()
                    .returned(items.size())
                    .limit(limit)
                    .matched((int) totalMatched)
                    .build();

            // Build links (next page link if applicable)
            List<org.stac.model.Link> links = new ArrayList<>();
            if (nextToken != null) {
                links.add(org.stac.model.Link.builder()
                        .rel("next")
                        .href("/search?token=" + nextToken)
                        .type("application/geo+json")
                        .build());
            }

            return ItemCollection.builder()
                    .type("FeatureCollection")
                    .features(items)
                    .context(context)
                    .links(links)
                    .numberMatched((int) totalMatched)
                    .numberReturned(items.size())
                    .build();

        } catch (IOException e) {
            throw RepositoryException.search("Search operation failed", e);
        }
    }

    // ========================================================================
    // Aggregation Operations
    // ========================================================================

    @Override
    public Map<String, Object> aggregate(AggregationRequest request) {
        try {
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(ITEMS_INDEX_PATTERN)
                    .size(0); // We only want aggregations, not documents

            // Apply collection filter if specified
            if (request.hasCollectionScope()) {
                Query collectionFilter = Query.of(q -> q.terms(t -> t
                        .field("collection")
                        .terms(tv -> tv.value(request.getCollections().stream()
                                .map(FieldValue::of)
                                .toList()))));
                requestBuilder.query(collectionFilter);
            }

            // Apply additional filter if specified
            if (request.hasFilter()) {
                Query filter = cql2Converter.convert(request.getFilter());
                if (request.hasCollectionScope()) {
                    // Combine with collection filter
                    requestBuilder.query(q -> q.bool(b -> b
                            .must(requestBuilder.build().query())
                            .must(filter)));
                } else {
                    requestBuilder.query(filter);
                }
            }

            // Build aggregations
            Map<String, Aggregation> aggregations = new HashMap<>();
            if (request.getAggregations() != null) {
                for (String field : request.getAggregations()) {
                    String aggName = field.replace(".", "_");

                    // Determine aggregation type based on field
                    if (field.contains("datetime") || field.contains("date")) {
                        // Date histogram aggregation
                        aggregations.put(aggName, Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(field)
                                        .calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Month))));
                    } else {
                        // Terms aggregation for most fields
                        aggregations.put(aggName, Aggregation.of(a -> a
                                .terms(t -> t.field(field).size(100))));
                    }
                }
            }

            requestBuilder.aggregations(aggregations);

            SearchResponse<ObjectNode> response = elasticsearchClient.search(
                    requestBuilder.build(), ObjectNode.class);

            // Extract aggregation results
            Map<String, Object> results = new HashMap<>();
            if (response.aggregations() != null) {
                response.aggregations().forEach((name, agg) -> {
                    List<Map<String, Object>> buckets = new ArrayList<>();

                    if (agg.isSterms()) {
                        agg.sterms().buckets().array().forEach(bucket -> {
                            Map<String, Object> bucketMap = new HashMap<>();
                            bucketMap.put("key", bucket.key().stringValue());
                            bucketMap.put("doc_count", bucket.docCount());
                            buckets.add(bucketMap);
                        });
                    } else if (agg.isLterms()) {
                        agg.lterms().buckets().array().forEach(bucket -> {
                            Map<String, Object> bucketMap = new HashMap<>();
                            bucketMap.put("key", bucket.key());
                            bucketMap.put("doc_count", bucket.docCount());
                            buckets.add(bucketMap);
                        });
                    } else if (agg.isDateHistogram()) {
                        agg.dateHistogram().buckets().array().forEach(bucket -> {
                            Map<String, Object> bucketMap = new HashMap<>();
                            bucketMap.put("key", bucket.keyAsString());
                            bucketMap.put("doc_count", bucket.docCount());
                            buckets.add(bucketMap);
                        });
                    }

                    results.put(name, buckets);
                });
            }

            return results;

        } catch (IOException e) {
            throw new RepositoryException("Aggregation operation failed", e);
        }
    }

    // ========================================================================
    // Bulk Operations
    // ========================================================================

    @Override
    public BulkOperationResult bulkInsert(String collectionId, List<Item> items) {
        return executeBulkOperation(collectionId, items, "index");
    }

    @Override
    public BulkOperationResult bulkUpdate(String collectionId, List<Item> items) {
        return executeBulkOperation(collectionId, items, "index"); // Elasticsearch index is upsert
    }

    @Override
    public BulkOperationResult bulkDelete(String collectionId, List<String> itemIds) {
        try {
            String index = getItemIndexForCollection(collectionId);
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (String itemId : itemIds) {
                bulkBuilder.operations(op -> op
                        .delete(d -> d.index(index).id(itemId)));
            }

            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
            return processBulkResponse(response);

        } catch (IOException e) {
            throw RepositoryException.bulk("delete", e);
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Builds an Elasticsearch query from SearchCriteria.
     */
    private Query buildSearchQuery(SearchCriteria criteria) {
        List<Query> mustQueries = new ArrayList<>();

        // Collection filter
        if (criteria.hasCollectionConstraint()) {
            mustQueries.add(Query.of(q -> q.terms(t -> t
                    .field("collection")
                    .terms(tv -> tv.value(criteria.getCollections().stream()
                            .map(FieldValue::of)
                            .toList())))));
        }

        // ID filter
        if (criteria.hasIdConstraint()) {
            mustQueries.add(Query.of(q -> q.terms(t -> t
                    .field("id")
                    .terms(tv -> tv.value(criteria.getIds().stream()
                            .map(FieldValue::of)
                            .toList())))));
        }

        // Bounding box filter
        if (criteria.getBbox() != null) {
            mustQueries.add(buildBboxQuery(criteria.getBbox()));
        }

        // Geometry intersection filter
        if (criteria.getIntersects() != null) {
            mustQueries.add(buildGeometryIntersectsQuery(criteria.getIntersects()));
        }

        // Datetime filter
        if (criteria.hasTemporalConstraint()) {
            mustQueries.add(buildDatetimeQuery(criteria.getDatetime()));
        }

        // CQL2 filter
        if (criteria.hasFilterConstraint() && criteria.getFilter() != null) {
            mustQueries.add(cql2Converter.convert(criteria.getFilter()));
        }

        // Combine all queries
        if (mustQueries.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        } else if (mustQueries.size() == 1) {
            return mustQueries.get(0);
        } else {
            return Query.of(q -> q.bool(b -> b.must(mustQueries)));
        }
    }

    /**
     * Builds a geo_shape query for bounding box filter.
     */
    private Query buildBboxQuery(BoundingBox bbox) {
        // Create a polygon envelope for the bbox
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", "envelope");
        envelope.put("coordinates", List.of(
                List.of(bbox.getWest(), bbox.getNorth()),  // top-left
                List.of(bbox.getEast(), bbox.getSouth())   // bottom-right
        ));

        return Query.of(q -> q.geoShape(gs -> gs
                .field("geometry")
                .shape(s -> s
                        .shape(JsonData.of(envelope))
                        .relation(co.elastic.clients.elasticsearch._types.GeoShapeRelation.Intersects))));
    }

    /**
     * Builds a geo_shape query for geometry intersection.
     */
    private Query buildGeometryIntersectsQuery(org.locationtech.jts.geom.Geometry geometry) {
        // Convert JTS geometry to GeoJSON
        Map<String, Object> geoJson = convertGeometryToGeoJson(geometry);

        return Query.of(q -> q.geoShape(gs -> gs
                .field("geometry")
                .shape(s -> s
                        .shape(JsonData.of(geoJson))
                        .relation(co.elastic.clients.elasticsearch._types.GeoShapeRelation.Intersects))));
    }

    /**
     * Builds a range query for datetime filter.
     */
    private Query buildDatetimeQuery(TemporalExtent datetime) {
        if (datetime.getInterval() == null || datetime.getInterval().isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        List<String> interval = datetime.getInterval().get(0);
        String start = interval.size() > 0 ? interval.get(0) : null;
        String end = interval.size() > 1 ? interval.get(1) : null;

        return Query.of(q -> q.range(r -> {
            var builder = r.field("properties.datetime");
            if (start != null && !start.equals("..")) {
                builder.gte(JsonData.of(start));
            }
            if (end != null && !end.equals("..")) {
                builder.lte(JsonData.of(end));
            }
            return builder;
        }));
    }

    /**
     * Builds sort options from SortBy list.
     */
    private List<SortOptions> buildSortOptions(List<SortBy> sortBy) {
        List<SortOptions> options = new ArrayList<>();
        if (sortBy != null) {
            for (SortBy sort : sortBy) {
                SortOrder order = sort.getDirection() == SortDirection.DESC
                        ? SortOrder.Desc : SortOrder.Asc;
                options.add(SortOptions.of(s -> s.field(f -> f
                        .field(sort.getField())
                        .order(order))));
            }
        }
        return options;
    }

    /**
     * Encodes search_after values to a pagination token.
     */
    private String encodeSearchAfterToken(List<FieldValue> sortValues) {
        try {
            List<Object> values = sortValues.stream()
                    .map(this::fieldValueToObject)
                    .toList();
            String json = objectMapper.writeValueAsString(values);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Failed to encode search_after token", e);
            return null;
        }
    }

    /**
     * Decodes a pagination token to search_after values.
     */
    private List<FieldValue> decodeSearchAfterToken(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String json = new String(decoded, StandardCharsets.UTF_8);
            List<?> values = objectMapper.readValue(json, List.class);
            return values.stream()
                    .map(this::objectToFieldValue)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to decode search_after token: {}", token, e);
            return null;
        }
    }

    /**
     * Converts FieldValue to Object for JSON serialization.
     */
    private Object fieldValueToObject(FieldValue fv) {
        if (fv.isString()) {
            return fv.stringValue();
        } else if (fv.isLong()) {
            return fv.longValue();
        } else if (fv.isDouble()) {
            return fv.doubleValue();
        } else if (fv.isBoolean()) {
            return fv.booleanValue();
        } else if (fv.isNull()) {
            return null;
        }
        return fv._toJsonString();
    }

    /**
     * Converts Object to FieldValue for Elasticsearch.
     */
    private FieldValue objectToFieldValue(Object obj) {
        if (obj == null) {
            return FieldValue.of(fv -> fv.nullValue());
        } else if (obj instanceof String s) {
            return FieldValue.of(s);
        } else if (obj instanceof Long l) {
            return FieldValue.of(l);
        } else if (obj instanceof Integer i) {
            return FieldValue.of(i.longValue());
        } else if (obj instanceof Double d) {
            return FieldValue.of(d);
        } else if (obj instanceof Boolean b) {
            return FieldValue.of(b);
        }
        return FieldValue.of(obj.toString());
    }

    /**
     * Gets the index name for a collection's items.
     */
    private String getItemIndexForCollection(String collectionId) {
        return "stac-items-" + collectionId.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    /**
     * Executes a bulk index/update operation.
     */
    private BulkOperationResult executeBulkOperation(String collectionId, List<Item> items, String opType) {
        try {
            String index = getItemIndexForCollection(collectionId);
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (Item item : items) {
                if (item.getCollection() == null) {
                    item.setCollection(collectionId);
                }
                Map<String, Object> doc = itemMapper.toDocument(item);

                bulkBuilder.operations(op -> op
                        .index(idx -> idx.index(index).id(item.getId()).document(doc)));
            }

            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
            return processBulkResponse(response);

        } catch (IOException e) {
            throw RepositoryException.bulk(opType, e);
        }
    }

    /**
     * Processes a bulk response into a BulkOperationResult.
     */
    private BulkOperationResult processBulkResponse(BulkResponse response) {
        BulkOperationResult result = BulkOperationResult.builder()
                .success(0)
                .failed(0)
                .items(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                result.addFailure(item.id() + ": " + item.error().reason());
            } else {
                result.addSuccess(item.id());
            }
        }

        return result;
    }

    /**
     * Converts a JTS Geometry to a GeoJSON Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertGeometryToGeoJson(org.locationtech.jts.geom.Geometry geometry) {
        Map<String, Object> geoJson = new HashMap<>();
        String type = geometry.getGeometryType();
        geoJson.put("type", type);

        switch (type) {
            case "Point" -> {
                var coord = geometry.getCoordinate();
                geoJson.put("coordinates", List.of(coord.x, coord.y));
            }
            case "LineString" -> {
                List<List<Double>> coords = new ArrayList<>();
                for (var coord : geometry.getCoordinates()) {
                    coords.add(List.of(coord.x, coord.y));
                }
                geoJson.put("coordinates", coords);
            }
            case "Polygon" -> {
                org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                List<List<List<Double>>> rings = new ArrayList<>();

                // Exterior ring
                List<List<Double>> exterior = new ArrayList<>();
                for (var coord : polygon.getExteriorRing().getCoordinates()) {
                    exterior.add(List.of(coord.x, coord.y));
                }
                rings.add(exterior);

                // Interior rings
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    List<List<Double>> hole = new ArrayList<>();
                    for (var coord : polygon.getInteriorRingN(i).getCoordinates()) {
                        hole.add(List.of(coord.x, coord.y));
                    }
                    rings.add(hole);
                }
                geoJson.put("coordinates", rings);
            }
            default -> {
                // For complex geometries, delegate to mapper
                return objectMapper.convertValue(
                        objectMapper.valueToTree(geometry), Map.class);
            }
        }

        return geoJson;
    }
}
