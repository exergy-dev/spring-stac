package org.stac.repository.pgstac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stac.model.AggregationRequest;
import org.stac.model.BoundingBox;
import org.stac.model.Collection;
import org.stac.model.FieldSelector;
import org.stac.model.FilterCriteria;
import org.stac.model.Item;
import org.stac.model.ItemQueryCriteria;
import org.stac.model.SearchCriteria;
import org.stac.model.SortBy;
import org.stac.model.SortDirection;
import org.stac.model.TemporalExtent;

import java.util.List;

/**
 * Helper class for building JSON payloads for pgSTAC stored procedures.
 * <p>
 * pgSTAC uses JSON/JSONB parameters for its stored procedures.
 * This class converts STAC model objects to the JSON format expected by pgSTAC.
 *
 * @see <a href="https://github.com/stac-utils/pgstac">pgSTAC Documentation</a>
 */
@Component
@RequiredArgsConstructor
public class PgStacFunctionMapper {

    private final ObjectMapper objectMapper;

    /**
     * When true, hydration (link injection, property enrichment) happens at API level.
     * When false (default), pgSTAC handles hydration in the database.
     * <p>
     * Equivalent to stac-fastapi-pgstac's USE_API_HYDRATE environment variable.
     */
    @Value("${stac.pgstac.use-api-hydrate:false}")
    private boolean useApiHydrate;

    /**
     * Builds the JSON payload for pgSTAC search function.
     * <p>
     * Converts SearchCriteria to the JSON format expected by pgSTAC's search() function.
     *
     * @param criteria the search criteria
     * @return JSON string for pgSTAC search
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String buildSearchJson(SearchCriteria criteria) throws JsonProcessingException {
        ObjectNode searchNode = objectMapper.createObjectNode();

        // Collections filter
        if (criteria.getCollections() != null && !criteria.getCollections().isEmpty()) {
            ArrayNode collectionsArray = objectMapper.createArrayNode();
            criteria.getCollections().forEach(collectionsArray::add);
            searchNode.set("collections", collectionsArray);
        }

        // IDs filter
        if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
            ArrayNode idsArray = objectMapper.createArrayNode();
            criteria.getIds().forEach(idsArray::add);
            searchNode.set("ids", idsArray);
        }

        // Bounding box
        if (criteria.getBbox() != null) {
            searchNode.set("bbox", buildBboxArray(criteria.getBbox()));
        }

        // Datetime/Temporal extent
        if (criteria.getDatetime() != null) {
            String datetimeStr = buildDatetimeString(criteria.getDatetime());
            if (datetimeStr != null) {
                searchNode.put("datetime", datetimeStr);
            }
        }

        // Intersects geometry
        if (criteria.getIntersects() != null) {
            searchNode.set("intersects", buildGeometryNode(criteria.getIntersects()));
        }

        // Limit
        if (criteria.getLimit() != null) {
            searchNode.put("limit", criteria.getLimit());
        }

        // Token for pagination
        if (criteria.getToken() != null && !criteria.getToken().isBlank()) {
            searchNode.put("token", criteria.getToken());
        }

        // CQL2 Filter
        if (criteria.getFilter() != null) {
            searchNode.set("filter", buildFilterNode(criteria.getFilter()));
            searchNode.put("filter-lang", "cql2-json");
        }

        // Sort
        if (criteria.getSortBy() != null && !criteria.getSortBy().isEmpty()) {
            searchNode.set("sortby", buildSortByArray(criteria.getSortBy()));
        }

        // Fields
        if (criteria.getFields() != null && criteria.getFields().hasSelections()) {
            searchNode.set("fields", buildFieldsNode(criteria.getFields()));
        }

        // Free-text search (q parameter)
        if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
            searchNode.put("q", criteria.getQ());
        }

        // Configuration for pgSTAC - request context with exact counts
        // nohydrate: when true, pgSTAC returns raw items without link injection
        // API then handles hydration (equivalent to USE_API_HYDRATE in stac-fastapi-pgstac)
        ObjectNode confNode = objectMapper.createObjectNode();
        confNode.put("context", "exact");
        confNode.put("nohydrate", useApiHydrate);
        searchNode.set("conf", confNode);

        return objectMapper.writeValueAsString(searchNode);
    }

    /**
     * Builds the JSON payload for item query within a collection.
     *
     * @param collectionId the collection ID
     * @param criteria     the query criteria
     * @return JSON string for pgSTAC items query
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String buildItemQueryJson(String collectionId, ItemQueryCriteria criteria)
            throws JsonProcessingException {
        // Convert ItemQueryCriteria to SearchCriteria and use existing method
        SearchCriteria searchCriteria = criteria != null
                ? criteria.toSearchCriteria(collectionId)
                : SearchCriteria.builder()
                        .collections(List.of(collectionId))
                        .build();
        return buildSearchJson(searchCriteria);
    }

    /**
     * Builds the JSON payload for pgSTAC create_item function.
     *
     * @param item the item to create
     * @return JSON string representation of the item
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String buildItemJson(Item item) throws JsonProcessingException {
        return objectMapper.writeValueAsString(item);
    }

    /**
     * Builds the JSON payload for pgSTAC create_collection function.
     *
     * @param collection the collection to create
     * @return JSON string representation of the collection
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String buildCollectionJson(Collection collection) throws JsonProcessingException {
        return objectMapper.writeValueAsString(collection);
    }

    /**
     * Builds the JSON payload for aggregation queries.
     *
     * @param request the aggregation request
     * @return JSON string for pgSTAC aggregation
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String buildAggregationJson(AggregationRequest request) throws JsonProcessingException {
        ObjectNode aggNode = objectMapper.createObjectNode();

        // Collections filter
        if (request.getCollections() != null && !request.getCollections().isEmpty()) {
            ArrayNode collectionsArray = objectMapper.createArrayNode();
            request.getCollections().forEach(collectionsArray::add);
            aggNode.set("collections", collectionsArray);
        }

        // Aggregation fields
        if (request.getAggregations() != null && !request.getAggregations().isEmpty()) {
            ArrayNode aggregationsArray = objectMapper.createArrayNode();
            request.getAggregations().forEach(aggregationsArray::add);
            aggNode.set("aggregations", aggregationsArray);
        }

        // Filter
        if (request.getFilter() != null) {
            aggNode.set("filter", buildFilterNode(request.getFilter()));
        }

        return objectMapper.writeValueAsString(aggNode);
    }

    /**
     * Builds a JSON array node for bounding box.
     */
    private ArrayNode buildBboxArray(BoundingBox bbox) {
        ArrayNode bboxArray = objectMapper.createArrayNode();
        double[] coords = bbox.toArray();
        for (double coord : coords) {
            bboxArray.add(coord);
        }
        return bboxArray;
    }

    /**
     * Builds a datetime string from TemporalExtent.
     */
    private String buildDatetimeString(TemporalExtent temporal) {
        if (temporal.getInterval() == null || temporal.getInterval().isEmpty()) {
            return null;
        }

        List<String> firstInterval = temporal.getInterval().get(0);
        if (firstInterval == null || firstInterval.size() < 2) {
            return null;
        }

        String start = firstInterval.get(0);
        String end = firstInterval.get(1);

        if (start == null && end == null) {
            return null;
        } else if (start == null) {
            return "../" + end;
        } else if (end == null) {
            return start + "/..";
        } else if (start.equals(end)) {
            return start;
        } else {
            return start + "/" + end;
        }
    }

    /**
     * Builds a GeoJSON geometry node.
     */
    private ObjectNode buildGeometryNode(Geometry geometry) throws JsonProcessingException {
        // Use the ObjectMapper which should have GeoJSON serialization configured
        String geoJson = objectMapper.writeValueAsString(geometry);
        return (ObjectNode) objectMapper.readTree(geoJson);
    }

    /**
     * Builds a CQL2-JSON filter node from FilterCriteria.
     */
    private ObjectNode buildFilterNode(FilterCriteria filter) {
        ObjectNode filterNode = objectMapper.createObjectNode();

        if (filter.isLogical()) {
            // Logical operators: AND, OR, NOT
            filterNode.put("op", filter.getOperator().toLowerCase());
            ArrayNode argsArray = objectMapper.createArrayNode();
            if (filter.getChildren() != null) {
                for (FilterCriteria child : filter.getChildren()) {
                    argsArray.add(buildFilterNode(child));
                }
            }
            filterNode.set("args", argsArray);
        } else {
            // Comparison, spatial, or temporal operators
            filterNode.put("op", mapOperator(filter.getOperator()));
            ArrayNode argsArray = objectMapper.createArrayNode();

            // Property reference
            ObjectNode propertyNode = objectMapper.createObjectNode();
            propertyNode.put("property", filter.getPropertyName());
            argsArray.add(propertyNode);

            // Value
            if (filter.getValue() != null) {
                argsArray.add(objectMapper.valueToTree(filter.getValue()));
            }

            filterNode.set("args", argsArray);
        }

        return filterNode;
    }

    /**
     * Maps STAC filter operators to CQL2-JSON operators.
     */
    private String mapOperator(String operator) {
        if (operator == null) {
            return "eq";
        }
        return switch (operator) {
            case "=" -> "eq";
            case "<>" -> "neq";
            case "<" -> "lt";
            case "<=" -> "lte";
            case ">" -> "gt";
            case ">=" -> "gte";
            case "LIKE" -> "like";
            case "IN" -> "in";
            case "IS NULL" -> "isNull";
            case "BETWEEN" -> "between";
            case "INTERSECTS" -> "s_intersects";
            case "WITHIN" -> "s_within";
            case "CONTAINS" -> "s_contains";
            case "DISJOINT" -> "s_disjoint";
            case "T_INTERSECTS" -> "t_intersects";
            case "T_BEFORE" -> "t_before";
            case "T_AFTER" -> "t_after";
            case "T_DURING" -> "t_during";
            default -> operator.toLowerCase();
        };
    }

    /**
     * Builds sortby array for pgSTAC.
     */
    private ArrayNode buildSortByArray(List<SortBy> sortByList) {
        ArrayNode sortArray = objectMapper.createArrayNode();
        for (SortBy sortBy : sortByList) {
            ObjectNode sortNode = objectMapper.createObjectNode();
            sortNode.put("field", sortBy.getField());
            sortNode.put("direction", sortBy.getDirection() == SortDirection.DESC ? "desc" : "asc");
            sortArray.add(sortNode);
        }
        return sortArray;
    }

    /**
     * Builds fields node for pgSTAC.
     */
    private ObjectNode buildFieldsNode(FieldSelector fields) {
        ObjectNode fieldsNode = objectMapper.createObjectNode();

        if (fields.getInclude() != null && !fields.getInclude().isEmpty()) {
            ArrayNode includeArray = objectMapper.createArrayNode();
            fields.getInclude().forEach(includeArray::add);
            fieldsNode.set("include", includeArray);
        }

        if (fields.getExclude() != null && !fields.getExclude().isEmpty()) {
            ArrayNode excludeArray = objectMapper.createArrayNode();
            fields.getExclude().forEach(excludeArray::add);
            fieldsNode.set("exclude", excludeArray);
        }

        return fieldsNode;
    }
}
