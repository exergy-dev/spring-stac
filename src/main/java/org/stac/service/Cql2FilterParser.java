package org.stac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.After;
import org.geotools.api.filter.temporal.Before;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.TEquals;
import org.springframework.stereotype.Component;
import org.stac.model.FilterCriteria;
import org.stac.model.FilterType;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for CQL2 filter expressions.
 * <p>
 * Uses GeoTools CQL library for CQL2-Text parsing and Jackson with JtsModule for GeoJSON handling.
 * Supports both CQL2-Text and CQL2-JSON formats as specified in the
 * OGC CQL2 and STAC Filter Extension specifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cql2FilterParser {

    private final ObjectMapper objectMapper;

    /**
     * Parses a filter expression based on the specified filter language.
     *
     * @param filter     the filter expression (text or JSON string)
     * @param filterLang the filter language: "cql2-text" or "cql2-json"
     * @return parsed FilterCriteria
     * @throws IllegalArgumentException if the filter cannot be parsed
     */
    public FilterCriteria parseFilter(String filter, String filterLang) {
        if (filter == null || filter.isBlank()) {
            return null;
        }

        if ("cql2-json".equalsIgnoreCase(filterLang)) {
            try {
                JsonNode jsonNode = objectMapper.readTree(filter);
                return parseCql2Json(jsonNode);
            } catch (Exception e) {
                log.error("Failed to parse CQL2-JSON filter: {}", filter, e);
                throw new IllegalArgumentException("Invalid CQL2-JSON filter: " + e.getMessage(), e);
            }
        } else {
            // Default to CQL2-Text
            return parseCql2Text(filter);
        }
    }

    /**
     * Parses a CQL2-Text format filter expression using GeoTools CQL library.
     *
     * @param cql the CQL2-Text expression
     * @return parsed FilterCriteria
     * @throws IllegalArgumentException if the expression cannot be parsed
     */
    public FilterCriteria parseCql2Text(String cql) {
        if (cql == null || cql.isBlank()) {
            return null;
        }

        String trimmed = cql.trim();
        log.debug("Parsing CQL2-Text: {}", trimmed);

        try {
            // Use GeoTools ECQL parser (extended CQL with more features)
            Filter filter = ECQL.toFilter(trimmed);
            return convertGeoToolsFilter(filter);
        } catch (Exception e) {
            log.warn("ECQL parsing failed, trying standard CQL: {}", e.getMessage());
            try {
                // Fallback to standard CQL
                Filter filter = CQL.toFilter(trimmed);
                return convertGeoToolsFilter(filter);
            } catch (Exception e2) {
                log.error("Failed to parse CQL2-Text: {}", cql, e2);
                throw new IllegalArgumentException("Invalid CQL2-Text expression: " + e2.getMessage(), e2);
            }
        }
    }

    /**
     * Parses a CQL2-JSON format filter expression.
     *
     * @param cql the JSON node containing the CQL2 expression
     * @return parsed FilterCriteria
     * @throws IllegalArgumentException if the expression cannot be parsed
     */
    public FilterCriteria parseCql2Json(JsonNode cql) {
        if (cql == null || cql.isNull()) {
            return null;
        }

        log.debug("Parsing CQL2-JSON: {}", cql);

        try {
            return parseJsonExpression(cql);
        } catch (Exception e) {
            log.error("Failed to parse CQL2-JSON: {}", cql, e);
            throw new IllegalArgumentException("Invalid CQL2-JSON expression: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a GeoTools Filter to our internal FilterCriteria representation.
     */
    private FilterCriteria convertGeoToolsFilter(Filter filter) {
        if (filter == null || filter == Filter.INCLUDE) {
            return null;
        }

        if (filter == Filter.EXCLUDE) {
            return FilterCriteria.builder()
                    .type(FilterType.COMPARISON)
                    .operator("=")
                    .propertyName("1")
                    .value(0)
                    .build();
        }

        // Logical operators
        if (filter instanceof And andFilter) {
            List<FilterCriteria> children = new ArrayList<>();
            for (Filter child : andFilter.getChildren()) {
                FilterCriteria childCriteria = convertGeoToolsFilter(child);
                if (childCriteria != null) {
                    children.add(childCriteria);
                }
            }
            return FilterCriteria.and(children);
        }

        if (filter instanceof Or orFilter) {
            List<FilterCriteria> children = new ArrayList<>();
            for (Filter child : orFilter.getChildren()) {
                FilterCriteria childCriteria = convertGeoToolsFilter(child);
                if (childCriteria != null) {
                    children.add(childCriteria);
                }
            }
            return FilterCriteria.or(children);
        }

        if (filter instanceof Not notFilter) {
            // Check if this is a Not(PropertyIsEqualTo) which represents <>
            if (notFilter.getFilter() instanceof PropertyIsEqualTo eq) {
                return buildComparisonCriteria("<>", eq.getExpression1(), eq.getExpression2());
            }
            FilterCriteria child = convertGeoToolsFilter(notFilter.getFilter());
            return FilterCriteria.not(child);
        }

        // Comparison operators
        if (filter instanceof PropertyIsEqualTo eq) {
            return buildComparisonCriteria("=", eq.getExpression1(), eq.getExpression2());
        }

        if (filter instanceof PropertyIsNotEqualTo neq) {
            return buildComparisonCriteria("<>", neq.getExpression1(), neq.getExpression2());
        }

        if (filter instanceof PropertyIsLessThan lt) {
            return buildComparisonCriteria("<", lt.getExpression1(), lt.getExpression2());
        }

        if (filter instanceof PropertyIsLessThanOrEqualTo lte) {
            return buildComparisonCriteria("<=", lte.getExpression1(), lte.getExpression2());
        }

        if (filter instanceof PropertyIsGreaterThan gt) {
            return buildComparisonCriteria(">", gt.getExpression1(), gt.getExpression2());
        }

        if (filter instanceof PropertyIsGreaterThanOrEqualTo gte) {
            return buildComparisonCriteria(">=", gte.getExpression1(), gte.getExpression2());
        }

        if (filter instanceof PropertyIsLike like) {
            String propertyName = extractPropertyName(like.getExpression());
            return FilterCriteria.builder()
                    .type(FilterType.COMPARISON)
                    .operator("LIKE")
                    .propertyName(propertyName)
                    .value(like.getLiteral())
                    .build();
        }

        if (filter instanceof PropertyIsBetween between) {
            String propertyName = extractPropertyName(between.getExpression());
            Object lowerValue = extractValue(between.getLowerBoundary());
            Object upperValue = extractValue(between.getUpperBoundary());
            return FilterCriteria.builder()
                    .type(FilterType.COMPARISON)
                    .operator("BETWEEN")
                    .propertyName(propertyName)
                    .value(List.of(lowerValue, upperValue))
                    .build();
        }

        if (filter instanceof PropertyIsNull isNull) {
            String propertyName = extractPropertyName(isNull.getExpression());
            return FilterCriteria.builder()
                    .type(FilterType.COMPARISON)
                    .operator("IS NULL")
                    .propertyName(propertyName)
                    .value(null)
                    .build();
        }

        // Spatial operators
        if (filter instanceof Intersects intersects) {
            return buildSpatialCriteria("INTERSECTS", intersects.getExpression1(), intersects.getExpression2());
        }

        if (filter instanceof Contains contains) {
            return buildSpatialCriteria("CONTAINS", contains.getExpression1(), contains.getExpression2());
        }

        if (filter instanceof Within within) {
            return buildSpatialCriteria("WITHIN", within.getExpression1(), within.getExpression2());
        }

        if (filter instanceof Disjoint disjoint) {
            return buildSpatialCriteria("DISJOINT", disjoint.getExpression1(), disjoint.getExpression2());
        }

        if (filter instanceof BBOX bbox) {
            return FilterCriteria.builder()
                    .type(FilterType.SPATIAL)
                    .operator("BBOX")
                    .propertyName(extractPropertyName(bbox.getExpression1()))
                    .value(bbox.getBounds())
                    .build();
        }

        // Temporal operators
        if (filter instanceof Before before) {
            return buildTemporalCriteria("T_BEFORE", before.getExpression1(), before.getExpression2());
        }

        if (filter instanceof After after) {
            return buildTemporalCriteria("T_AFTER", after.getExpression1(), after.getExpression2());
        }

        if (filter instanceof During during) {
            return buildTemporalCriteria("T_DURING", during.getExpression1(), during.getExpression2());
        }

        if (filter instanceof TEquals tEquals) {
            return buildTemporalCriteria("T_EQUALS", tEquals.getExpression1(), tEquals.getExpression2());
        }

        log.warn("Unsupported filter type: {}", filter.getClass().getSimpleName());
        throw new IllegalArgumentException("Unsupported filter type: " + filter.getClass().getSimpleName());
    }

    private FilterCriteria buildComparisonCriteria(String operator, Expression expr1, Expression expr2) {
        String propertyName = extractPropertyName(expr1);
        Object value = extractValue(expr2);
        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator(operator)
                .propertyName(propertyName)
                .value(value)
                .build();
    }

    private FilterCriteria buildSpatialCriteria(String operator, Expression expr1, Expression expr2) {
        String propertyName = extractPropertyName(expr1);
        Object value = extractValue(expr2);
        return FilterCriteria.builder()
                .type(FilterType.SPATIAL)
                .operator(operator)
                .propertyName(propertyName)
                .value(value)
                .build();
    }

    private FilterCriteria buildTemporalCriteria(String operator, Expression expr1, Expression expr2) {
        String propertyName = extractPropertyName(expr1);
        Object value = extractValue(expr2);
        return FilterCriteria.builder()
                .type(FilterType.TEMPORAL)
                .operator(operator)
                .propertyName(propertyName)
                .value(value)
                .build();
    }

    private String extractPropertyName(Expression expr) {
        if (expr instanceof PropertyName propertyName) {
            return propertyName.getPropertyName();
        }
        return expr.toString();
    }

    private Object extractValue(Expression expr) {
        if (expr instanceof Literal literal) {
            return literal.getValue();
        }
        return expr.toString();
    }

    // CQL2-JSON parsing methods

    private FilterCriteria parseJsonExpression(JsonNode node) {
        if (node.has("op")) {
            String op = node.get("op").asText().toLowerCase();

            return switch (op) {
                case "and" -> parseJsonLogical(node, FilterType.AND);
                case "or" -> parseJsonLogical(node, FilterType.OR);
                case "not" -> parseJsonNot(node);
                case "=", "eq" -> parseJsonComparison(node, "=");
                case "<>", "neq", "!=" -> parseJsonComparison(node, "<>");
                case "<", "lt" -> parseJsonComparison(node, "<");
                case "<=", "lte" -> parseJsonComparison(node, "<=");
                case ">", "gt" -> parseJsonComparison(node, ">");
                case ">=", "gte" -> parseJsonComparison(node, ">=");
                case "like" -> parseJsonComparison(node, "LIKE");
                case "between" -> parseJsonBetween(node);
                case "in" -> parseJsonIn(node);
                case "isnull", "is null", "isNull" -> parseJsonIsNull(node);
                case "s_intersects" -> parseJsonSpatial(node, "INTERSECTS");
                case "s_contains" -> parseJsonSpatial(node, "CONTAINS");
                case "s_within" -> parseJsonSpatial(node, "WITHIN");
                case "s_disjoint" -> parseJsonSpatial(node, "DISJOINT");
                case "t_before" -> parseJsonTemporal(node, "T_BEFORE");
                case "t_after" -> parseJsonTemporal(node, "T_AFTER");
                case "t_during" -> parseJsonTemporal(node, "T_DURING");
                case "t_equals" -> parseJsonTemporal(node, "T_EQUALS");
                case "t_intersects" -> parseJsonTemporal(node, "T_INTERSECTS");
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }

        // Handle property reference
        if (node.has("property")) {
            return FilterCriteria.builder()
                    .propertyName(node.get("property").asText())
                    .build();
        }

        throw new IllegalArgumentException("Invalid CQL2-JSON node: " + node);
    }

    private FilterCriteria parseJsonLogical(JsonNode node, FilterType type) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray()) {
            throw new IllegalArgumentException(type + " operator requires args array");
        }

        List<FilterCriteria> children = new ArrayList<>();
        for (JsonNode arg : args) {
            children.add(parseJsonExpression(arg));
        }

        return FilterCriteria.builder()
                .type(type)
                .operator(type.name())
                .children(children)
                .build();
    }

    private FilterCriteria parseJsonNot(JsonNode node) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.isEmpty()) {
            throw new IllegalArgumentException("NOT operator requires one argument");
        }
        return FilterCriteria.not(parseJsonExpression(args.get(0)));
    }

    private FilterCriteria parseJsonComparison(JsonNode node, String operator) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.size() < 2) {
            throw new IllegalArgumentException(operator + " operator requires two arguments");
        }

        String propertyName = extractJsonPropertyName(args.get(0));
        Object value = extractJsonValue(args.get(1));

        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator(operator)
                .propertyName(propertyName)
                .value(value)
                .build();
    }

    private FilterCriteria parseJsonBetween(JsonNode node) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.size() < 3) {
            throw new IllegalArgumentException("BETWEEN operator requires three arguments");
        }

        String propertyName = extractJsonPropertyName(args.get(0));
        Object low = extractJsonValue(args.get(1));
        Object high = extractJsonValue(args.get(2));

        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator("BETWEEN")
                .propertyName(propertyName)
                .value(List.of(low, high))
                .build();
    }

    private FilterCriteria parseJsonIn(JsonNode node) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.size() < 2) {
            throw new IllegalArgumentException("IN operator requires at least two arguments");
        }

        String propertyName = extractJsonPropertyName(args.get(0));
        List<Object> values = new ArrayList<>();

        // Second argument should be an array of values
        JsonNode valuesNode = args.get(1);
        if (valuesNode.isArray()) {
            for (JsonNode val : valuesNode) {
                values.add(extractJsonValue(val));
            }
        } else {
            // Handle case where values are separate arguments
            for (int i = 1; i < args.size(); i++) {
                values.add(extractJsonValue(args.get(i)));
            }
        }

        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator("IN")
                .propertyName(propertyName)
                .value(values)
                .build();
    }

    private FilterCriteria parseJsonIsNull(JsonNode node) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.isEmpty()) {
            throw new IllegalArgumentException("IS NULL operator requires one argument");
        }

        String propertyName = extractJsonPropertyName(args.get(0));

        return FilterCriteria.builder()
                .type(FilterType.COMPARISON)
                .operator("IS NULL")
                .propertyName(propertyName)
                .value(null)
                .build();
    }

    private FilterCriteria parseJsonSpatial(JsonNode node, String operator) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.size() < 2) {
            throw new IllegalArgumentException(operator + " operator requires two arguments");
        }

        String propertyName = extractJsonPropertyName(args.get(0));
        Geometry geometry = parseGeoJsonGeometry(args.get(1));

        return FilterCriteria.builder()
                .type(FilterType.SPATIAL)
                .operator(operator)
                .propertyName(propertyName)
                .value(geometry)
                .build();
    }

    private FilterCriteria parseJsonTemporal(JsonNode node, String operator) {
        JsonNode args = node.get("args");
        if (args == null || !args.isArray() || args.size() < 2) {
            throw new IllegalArgumentException(operator + " operator requires two arguments");
        }

        String propertyName = extractJsonPropertyName(args.get(0));
        Object temporalValue = extractJsonTemporalValue(args.get(1));

        return FilterCriteria.builder()
                .type(FilterType.TEMPORAL)
                .operator(operator)
                .propertyName(propertyName)
                .value(temporalValue)
                .build();
    }

    private String extractJsonPropertyName(JsonNode node) {
        if (node.has("property")) {
            return node.get("property").asText();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        throw new IllegalArgumentException("Cannot extract property name from: " + node);
    }

    private Object extractJsonValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return node.asLong();
            }
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        } else if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode element : node) {
                values.add(extractJsonValue(element));
            }
            return values;
        }
        return node.toString();
    }

    private Object extractJsonTemporalValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.has("interval")) {
            JsonNode interval = node.get("interval");
            if (interval.isArray() && interval.size() >= 2) {
                String start = interval.get(0).isNull() ? ".." : interval.get(0).asText();
                String end = interval.get(1).isNull() ? ".." : interval.get(1).asText();
                return start + "/" + end;
            }
        } else if (node.has("timestamp")) {
            return node.get("timestamp").asText();
        } else if (node.has("date")) {
            return node.get("date").asText();
        }
        return node.toString();
    }

    /**
     * Parses a GeoJSON geometry from a JsonNode using Jackson with JtsModule.
     */
    private Geometry parseGeoJsonGeometry(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, Geometry.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse GeoJSON geometry: " + node, e);
        }
    }

    /**
     * Converts a JTS Geometry to GeoJSON string using Jackson with JtsModule.
     */
    public String geometryToGeoJson(Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert geometry to GeoJSON", e);
        }
    }
}
