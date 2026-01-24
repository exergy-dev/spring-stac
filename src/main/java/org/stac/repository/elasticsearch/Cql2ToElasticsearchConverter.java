package org.stac.repository.elasticsearch;

import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.stac.model.FilterCriteria;
import org.stac.model.FilterType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts CQL2 FilterCriteria to Elasticsearch Query DSL.
 * <p>
 * This component handles the translation of parsed CQL2 filter expressions
 * into Elasticsearch queries, supporting comparison, spatial, and temporal operators.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cql2ToElasticsearchConverter {

    /**
     * Converts a FilterCriteria to an Elasticsearch Query.
     *
     * @param criteria the filter criteria to convert
     * @return the corresponding Elasticsearch Query
     */
    public Query convert(FilterCriteria criteria) {
        if (criteria == null) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return switch (criteria.getType()) {
            case AND -> convertAnd(criteria);
            case OR -> convertOr(criteria);
            case NOT -> convertNot(criteria);
            case COMPARISON -> convertComparison(criteria);
            case SPATIAL -> convertSpatial(criteria);
            case TEMPORAL -> convertTemporal(criteria);
        };
    }

    /**
     * Converts an AND filter to an Elasticsearch bool query with must clauses.
     */
    private Query convertAnd(FilterCriteria criteria) {
        List<Query> mustQueries = new ArrayList<>();
        if (criteria.getChildren() != null) {
            for (FilterCriteria child : criteria.getChildren()) {
                mustQueries.add(convert(child));
            }
        }
        return Query.of(q -> q.bool(b -> b.must(mustQueries)));
    }

    /**
     * Converts an OR filter to an Elasticsearch bool query with should clauses.
     */
    private Query convertOr(FilterCriteria criteria) {
        List<Query> shouldQueries = new ArrayList<>();
        if (criteria.getChildren() != null) {
            for (FilterCriteria child : criteria.getChildren()) {
                shouldQueries.add(convert(child));
            }
        }
        return Query.of(q -> q.bool(b -> b.should(shouldQueries).minimumShouldMatch("1")));
    }

    /**
     * Converts a NOT filter to an Elasticsearch bool query with must_not clause.
     */
    private Query convertNot(FilterCriteria criteria) {
        List<Query> mustNotQueries = new ArrayList<>();
        if (criteria.getChildren() != null && !criteria.getChildren().isEmpty()) {
            mustNotQueries.add(convert(criteria.getChildren().get(0)));
        }
        return Query.of(q -> q.bool(b -> b.mustNot(mustNotQueries)));
    }

    /**
     * Converts a COMPARISON filter to the appropriate Elasticsearch query.
     * Supports: =, !=, <>, <, <=, >, >=, LIKE, IN, BETWEEN, IS NULL
     */
    private Query convertComparison(FilterCriteria criteria) {
        String operator = criteria.getOperator();
        String propertyName = normalizePropertyName(criteria.getPropertyName());
        Object value = criteria.getValue();

        return switch (operator.toUpperCase()) {
            case "=" -> createTermQuery(propertyName, value);
            case "!=", "<>" -> createNotEqualQuery(propertyName, value);
            case "<" -> createRangeQuery(propertyName, null, value, false, false);
            case "<=" -> createRangeQuery(propertyName, null, value, false, true);
            case ">" -> createRangeQuery(propertyName, value, null, false, false);
            case ">=" -> createRangeQuery(propertyName, value, null, true, false);
            case "LIKE" -> createWildcardQuery(propertyName, value);
            case "IN" -> createTermsQuery(propertyName, value);
            case "BETWEEN" -> createBetweenQuery(propertyName, value);
            case "IS NULL" -> createExistsQuery(propertyName, false);
            case "IS NOT NULL" -> createExistsQuery(propertyName, true);
            default -> {
                log.warn("Unknown comparison operator: {}, using term query", operator);
                yield createTermQuery(propertyName, value);
            }
        };
    }

    /**
     * Converts a SPATIAL filter to an Elasticsearch geo_shape query.
     * Supports: INTERSECTS, CONTAINS, WITHIN, DISJOINT
     */
    private Query convertSpatial(FilterCriteria criteria) {
        String operator = criteria.getOperator();
        String propertyName = normalizePropertyName(criteria.getPropertyName());
        Object value = criteria.getValue();

        GeoShapeRelation relation = switch (operator.toUpperCase()) {
            case "INTERSECTS", "S_INTERSECTS" -> GeoShapeRelation.Intersects;
            case "CONTAINS", "S_CONTAINS" -> GeoShapeRelation.Contains;
            case "WITHIN", "S_WITHIN" -> GeoShapeRelation.Within;
            case "DISJOINT", "S_DISJOINT" -> GeoShapeRelation.Disjoint;
            default -> {
                log.warn("Unknown spatial operator: {}, using intersects", operator);
                yield GeoShapeRelation.Intersects;
            }
        };

        Map<String, Object> geoJsonShape = convertToGeoJson(value);

        return Query.of(q -> q.geoShape(gs -> gs
                .field(propertyName)
                .shape(s -> s
                        .shape(JsonData.of(geoJsonShape))
                        .relation(relation))));
    }

    /**
     * Converts a TEMPORAL filter to an Elasticsearch range query.
     * Supports: T_BEFORE, T_AFTER, T_DURING, T_EQUALS, T_INTERSECTS
     */
    private Query convertTemporal(FilterCriteria criteria) {
        String operator = criteria.getOperator();
        String propertyName = normalizePropertyName(criteria.getPropertyName());
        Object value = criteria.getValue();

        return switch (operator.toUpperCase()) {
            case "T_BEFORE", "BEFORE" -> createTemporalBeforeQuery(propertyName, value);
            case "T_AFTER", "AFTER" -> createTemporalAfterQuery(propertyName, value);
            case "T_DURING", "DURING" -> createTemporalDuringQuery(propertyName, value);
            case "T_EQUALS" -> createTermQuery(propertyName, value);
            case "T_INTERSECTS" -> createTemporalIntersectsQuery(propertyName, value);
            default -> {
                log.warn("Unknown temporal operator: {}, using range query", operator);
                yield createTermQuery(propertyName, value);
            }
        };
    }

    /**
     * Creates a term query for exact match.
     */
    private Query createTermQuery(String field, Object value) {
        return Query.of(q -> q.term(t -> t.field(field).value(toFieldValue(value))));
    }

    /**
     * Creates a bool query with must_not for not equal comparison.
     */
    private Query createNotEqualQuery(String field, Object value) {
        return Query.of(q -> q.bool(b -> b.mustNot(
                Query.of(mq -> mq.term(t -> t.field(field).value(toFieldValue(value))))
        )));
    }

    /**
     * Creates a range query.
     */
    private Query createRangeQuery(String field, Object gte, Object lte, boolean includeGte, boolean includeLte) {
        return Query.of(q -> q.range(r -> {
            RangeQuery.Builder builder = new RangeQuery.Builder().field(field);
            if (gte != null) {
                if (includeGte) {
                    builder.gte(JsonData.of(gte));
                } else {
                    builder.gt(JsonData.of(gte));
                }
            }
            if (lte != null) {
                if (includeLte) {
                    builder.lte(JsonData.of(lte));
                } else {
                    builder.lt(JsonData.of(lte));
                }
            }
            return builder;
        }));
    }

    /**
     * Creates a wildcard query for LIKE operator.
     * Converts SQL LIKE syntax (% and _) to Elasticsearch wildcard syntax (* and ?).
     */
    private Query createWildcardQuery(String field, Object value) {
        String pattern = value.toString()
                .replace("%", "*")
                .replace("_", "?");
        return Query.of(q -> q.wildcard(w -> w.field(field).value(pattern)));
    }

    /**
     * Creates a terms query for IN operator.
     */
    @SuppressWarnings("unchecked")
    private Query createTermsQuery(String field, Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object v : (Collection<?>) value) {
                values.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                values.add(v.toString());
            }
        } else {
            values.add(value.toString());
        }

        return Query.of(q -> q.terms(t -> t
                .field(field)
                .terms(tv -> tv.value(values.stream()
                        .map(v -> co.elastic.clients.elasticsearch._types.FieldValue.of(v))
                        .toList()))));
    }

    /**
     * Creates a range query for BETWEEN operator.
     * Expects value to be a List or array with [lower, upper] bounds.
     */
    @SuppressWarnings("unchecked")
    private Query createBetweenQuery(String field, Object value) {
        Object lower = null;
        Object upper = null;

        if (value instanceof List<?> list && list.size() >= 2) {
            lower = list.get(0);
            upper = list.get(1);
        } else if (value instanceof Object[] arr && arr.length >= 2) {
            lower = arr[0];
            upper = arr[1];
        }

        final Object finalLower = lower;
        final Object finalUpper = upper;

        return Query.of(q -> q.range(r -> r
                .field(field)
                .gte(JsonData.of(finalLower))
                .lte(JsonData.of(finalUpper))));
    }

    /**
     * Creates an exists or not exists query.
     */
    private Query createExistsQuery(String field, boolean exists) {
        if (exists) {
            return Query.of(q -> q.exists(e -> e.field(field)));
        } else {
            return Query.of(q -> q.bool(b -> b.mustNot(
                    Query.of(mq -> mq.exists(e -> e.field(field)))
            )));
        }
    }

    /**
     * Creates a temporal BEFORE query (datetime < value).
     */
    private Query createTemporalBeforeQuery(String field, Object value) {
        return Query.of(q -> q.range(r -> r.field(field).lt(JsonData.of(value.toString()))));
    }

    /**
     * Creates a temporal AFTER query (datetime > value).
     */
    private Query createTemporalAfterQuery(String field, Object value) {
        return Query.of(q -> q.range(r -> r.field(field).gt(JsonData.of(value.toString()))));
    }

    /**
     * Creates a temporal DURING query (datetime within interval).
     * Expects value to be an interval string "start/end" or a List [start, end].
     */
    @SuppressWarnings("unchecked")
    private Query createTemporalDuringQuery(String field, Object value) {
        String start = null;
        String end = null;

        if (value instanceof String strValue) {
            String[] parts = strValue.split("/");
            if (parts.length == 2) {
                start = parts[0].equals("..") ? null : parts[0];
                end = parts[1].equals("..") ? null : parts[1];
            }
        } else if (value instanceof List<?> list && list.size() >= 2) {
            start = list.get(0) != null ? list.get(0).toString() : null;
            end = list.get(1) != null ? list.get(1).toString() : null;
        }

        final String finalStart = start;
        final String finalEnd = end;

        return Query.of(q -> q.range(r -> {
            RangeQuery.Builder builder = new RangeQuery.Builder().field(field);
            if (finalStart != null) {
                builder.gte(JsonData.of(finalStart));
            }
            if (finalEnd != null) {
                builder.lte(JsonData.of(finalEnd));
            }
            return builder;
        }));
    }

    /**
     * Creates a temporal INTERSECTS query for intervals.
     */
    private Query createTemporalIntersectsQuery(String field, Object value) {
        // For temporal intersects, we need to check if the item's temporal extent
        // overlaps with the query interval
        return createTemporalDuringQuery(field, value);
    }

    /**
     * Normalizes property names to Elasticsearch field names.
     * Converts dotted notation like "properties.datetime" to the actual ES field path.
     */
    private String normalizePropertyName(String propertyName) {
        if (propertyName == null) {
            return "properties.datetime";
        }
        // If the property already starts with "properties.", keep it
        // Otherwise, common STAC properties are in the properties object
        if (propertyName.equals("geometry") ||
            propertyName.equals("id") ||
            propertyName.equals("collection") ||
            propertyName.equals("bbox") ||
            propertyName.startsWith("properties.") ||
            propertyName.startsWith("assets.")) {
            return propertyName;
        }
        // Default to looking in properties
        return "properties." + propertyName;
    }

    /**
     * Converts a JTS Geometry or Map to GeoJSON format for Elasticsearch.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToGeoJson(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        } else if (value instanceof Geometry geometry) {
            return geometryToGeoJson(geometry);
        }
        throw new IllegalArgumentException("Cannot convert value to GeoJSON: " + value.getClass());
    }

    /**
     * Converts a JTS Geometry to a GeoJSON Map.
     */
    private Map<String, Object> geometryToGeoJson(Geometry geometry) {
        String type = geometry.getGeometryType();
        List<Object> coordinates = extractCoordinates(geometry);

        return Map.of(
                "type", type,
                "coordinates", coordinates
        );
    }

    /**
     * Extracts coordinates from a JTS Geometry.
     */
    private List<Object> extractCoordinates(Geometry geometry) {
        List<Object> coords = new ArrayList<>();
        String type = geometry.getGeometryType();

        switch (type) {
            case "Point" -> {
                coords.add(geometry.getCoordinate().x);
                coords.add(geometry.getCoordinate().y);
            }
            case "LineString", "LinearRing" -> {
                for (var coord : geometry.getCoordinates()) {
                    coords.add(List.of(coord.x, coord.y));
                }
            }
            case "Polygon" -> {
                org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                List<Object> rings = new ArrayList<>();
                // Exterior ring
                List<List<Double>> exteriorCoords = new ArrayList<>();
                for (var coord : polygon.getExteriorRing().getCoordinates()) {
                    exteriorCoords.add(List.of(coord.x, coord.y));
                }
                rings.add(exteriorCoords);
                // Interior rings (holes)
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    List<List<Double>> holeCoords = new ArrayList<>();
                    for (var coord : polygon.getInteriorRingN(i).getCoordinates()) {
                        holeCoords.add(List.of(coord.x, coord.y));
                    }
                    rings.add(holeCoords);
                }
                return rings;
            }
            case "MultiPoint", "MultiLineString", "MultiPolygon", "GeometryCollection" -> {
                for (int i = 0; i < geometry.getNumGeometries(); i++) {
                    coords.add(extractCoordinates(geometry.getGeometryN(i)));
                }
            }
        }

        return coords;
    }

    /**
     * Helper to convert values to FieldValue.
     */
    private co.elastic.clients.elasticsearch._types.FieldValue toFieldValue(Object value) {
        if (value instanceof String s) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(s);
        } else if (value instanceof Long l) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(l);
        } else if (value instanceof Integer i) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(i.longValue());
        } else if (value instanceof Double d) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(d);
        } else if (value instanceof Float f) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(f.doubleValue());
        } else if (value instanceof Boolean b) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(b);
        } else {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(value.toString());
        }
    }
}
