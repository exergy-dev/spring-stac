package org.stac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.stac.model.FilterCriteria;
import org.stac.model.FilterType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Cql2FilterParser.
 */
class Cql2FilterParserTest {

    private Cql2FilterParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new Cql2FilterParser(objectMapper);
    }

    @Nested
    @DisplayName("CQL2-Text Parsing Tests")
    class Cql2TextParsingTests {

        @Test
        @DisplayName("should parse simple equality")
        void shouldParseSimpleEquality() {
            // Given
            String cql = "platform = 'sentinel-2'";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("=", result.getOperator());
            assertEquals("platform", result.getPropertyName());
            assertEquals("sentinel-2", result.getValue());
        }

        @Test
        @DisplayName("should parse less than operator")
        void shouldParseLessThanOperator() {
            // Given
            String cql = "cloud_cover < 10";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("<", result.getOperator());
            assertEquals("cloud_cover", result.getPropertyName());
        }

        @Test
        @DisplayName("should parse less than or equal operator")
        void shouldParseLessThanOrEqualOperator() {
            // Given
            String cql = "cloud_cover <= 20";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("<=", result.getOperator());
        }

        @Test
        @DisplayName("should parse greater than operator")
        void shouldParseGreaterThanOperator() {
            // Given
            String cql = "eo:cloud_cover > 5";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals(">", result.getOperator());
        }

        @Test
        @DisplayName("should parse greater than or equal operator")
        void shouldParseGreaterThanOrEqualOperator() {
            // Given
            String cql = "gsd >= 10.0";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals(">=", result.getOperator());
        }

        @Test
        @DisplayName("should parse not equal operator")
        void shouldParseNotEqualOperator() {
            // Given
            String cql = "platform <> 'landsat'";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("<>", result.getOperator());
        }

        @Test
        @DisplayName("should parse LIKE operator")
        void shouldParseLikeOperator() {
            // Given
            String cql = "title LIKE '%satellite%'";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("LIKE", result.getOperator());
            assertEquals("title", result.getPropertyName());
            assertTrue(result.getValue().toString().contains("satellite"));
        }

        @Test
        @DisplayName("should parse AND expression")
        void shouldParseAndExpression() {
            // Given
            String cql = "platform = 'sentinel-2' AND cloud_cover < 10";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.AND, result.getType());
            assertEquals("AND", result.getOperator());
            assertNotNull(result.getChildren());
            assertEquals(2, result.getChildren().size());

            // Verify first child
            FilterCriteria child1 = result.getChildren().get(0);
            assertEquals(FilterType.COMPARISON, child1.getType());
            assertEquals("=", child1.getOperator());
            assertEquals("platform", child1.getPropertyName());

            // Verify second child
            FilterCriteria child2 = result.getChildren().get(1);
            assertEquals(FilterType.COMPARISON, child2.getType());
            assertEquals("<", child2.getOperator());
            assertEquals("cloud_cover", child2.getPropertyName());
        }

        @Test
        @DisplayName("should parse OR expression")
        void shouldParseOrExpression() {
            // Given
            String cql = "platform = 'sentinel-2' OR platform = 'landsat-8'";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.OR, result.getType());
            assertEquals("OR", result.getOperator());
            assertNotNull(result.getChildren());
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("should parse NOT expression")
        void shouldParseNotExpression() {
            // Given - use a complex expression that can't be simplified to <>
            String cql = "NOT (platform = 'landsat' AND sensor = 'multispectral')";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.NOT, result.getType());
            assertEquals("NOT", result.getOperator());
            assertNotNull(result.getChildren());
            assertEquals(1, result.getChildren().size());

            // Verify child is an AND expression
            FilterCriteria child = result.getChildren().get(0);
            assertEquals(FilterType.AND, child.getType());
        }

        @Test
        @DisplayName("should parse spatial INTERSECTS with WKT geometry")
        void shouldParseSpatialIntersects() {
            // Given
            String cql = "INTERSECTS(geometry, POLYGON((0 0, 1 0, 1 1, 0 1, 0 0)))";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.SPATIAL, result.getType());
            assertEquals("INTERSECTS", result.getOperator());
            assertEquals("geometry", result.getPropertyName());
            assertNotNull(result.getValue());
            assertTrue(result.getValue() instanceof Geometry);
        }

        @Test
        @DisplayName("should parse BETWEEN expression")
        void shouldParseBetween() {
            // Given
            String cql = "cloud_cover BETWEEN 0 AND 20";

            // When
            FilterCriteria result = parser.parseCql2Text(cql);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("BETWEEN", result.getOperator());
            assertEquals("cloud_cover", result.getPropertyName());
            assertNotNull(result.getValue());
            assertTrue(result.getValue() instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) result.getValue();
            assertEquals(2, values.size());
        }

        @Test
        @DisplayName("should return null for empty filter")
        void shouldReturnNullForEmptyFilter() {
            // When
            FilterCriteria result = parser.parseCql2Text("");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should return null for null filter")
        void shouldReturnNullForNullFilter() {
            // When
            FilterCriteria result = parser.parseCql2Text(null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should throw on invalid CQL")
        void shouldThrowOnInvalidCql() {
            // Given
            String invalidCql = "INVALID SYNTAX !!!";

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseCql2Text(invalidCql));
        }
    }

    @Nested
    @DisplayName("CQL2-JSON Parsing Tests")
    class Cql2JsonParsingTests {

        @Test
        @DisplayName("should parse JSON equality")
        void shouldParseJsonEquality() throws Exception {
            // Given
            String json = """
                {
                    "op": "=",
                    "args": [
                        {"property": "platform"},
                        "sentinel-2"
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("=", result.getOperator());
            assertEquals("platform", result.getPropertyName());
            assertEquals("sentinel-2", result.getValue());
        }

        @Test
        @DisplayName("should parse JSON AND expression")
        void shouldParseJsonAnd() throws Exception {
            // Given
            String json = """
                {
                    "op": "and",
                    "args": [
                        {
                            "op": "=",
                            "args": [{"property": "platform"}, "sentinel-2"]
                        },
                        {
                            "op": "<",
                            "args": [{"property": "cloud_cover"}, 10]
                        }
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.AND, result.getType());
            assertNotNull(result.getChildren());
            assertEquals(2, result.getChildren().size());

            // Verify children
            FilterCriteria child1 = result.getChildren().get(0);
            assertEquals(FilterType.COMPARISON, child1.getType());
            assertEquals("=", child1.getOperator());

            FilterCriteria child2 = result.getChildren().get(1);
            assertEquals(FilterType.COMPARISON, child2.getType());
            assertEquals("<", child2.getOperator());
        }

        @Test
        @DisplayName("should parse JSON OR expression")
        void shouldParseJsonOr() throws Exception {
            // Given
            String json = """
                {
                    "op": "or",
                    "args": [
                        {
                            "op": "=",
                            "args": [{"property": "platform"}, "sentinel-2"]
                        },
                        {
                            "op": "=",
                            "args": [{"property": "platform"}, "landsat-8"]
                        }
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.OR, result.getType());
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("should parse JSON NOT expression")
        void shouldParseJsonNot() throws Exception {
            // Given
            String json = """
                {
                    "op": "not",
                    "args": [
                        {
                            "op": "=",
                            "args": [{"property": "platform"}, "landsat"]
                        }
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.NOT, result.getType());
            assertEquals(1, result.getChildren().size());
        }

        @Test
        @DisplayName("should parse JSON spatial INTERSECTS with GeoJSON geometry")
        void shouldParseJsonSpatialIntersects() throws Exception {
            // Given
            String json = """
                {
                    "op": "s_intersects",
                    "args": [
                        {"property": "geometry"},
                        {
                            "type": "Polygon",
                            "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]]
                        }
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.SPATIAL, result.getType());
            assertEquals("INTERSECTS", result.getOperator());
            assertEquals("geometry", result.getPropertyName());
            assertNotNull(result.getValue());
            assertTrue(result.getValue() instanceof Geometry);
        }

        @Test
        @DisplayName("should parse JSON comparison operators")
        void shouldParseJsonComparisonOperators() throws Exception {
            // Test less than
            String ltJson = """
                {"op": "lt", "args": [{"property": "value"}, 10]}
                """;
            FilterCriteria ltResult = parser.parseCql2Json(objectMapper.readTree(ltJson));
            assertEquals("<", ltResult.getOperator());

            // Test less than or equal
            String lteJson = """
                {"op": "lte", "args": [{"property": "value"}, 10]}
                """;
            FilterCriteria lteResult = parser.parseCql2Json(objectMapper.readTree(lteJson));
            assertEquals("<=", lteResult.getOperator());

            // Test greater than
            String gtJson = """
                {"op": "gt", "args": [{"property": "value"}, 10]}
                """;
            FilterCriteria gtResult = parser.parseCql2Json(objectMapper.readTree(gtJson));
            assertEquals(">", gtResult.getOperator());

            // Test greater than or equal
            String gteJson = """
                {"op": "gte", "args": [{"property": "value"}, 10]}
                """;
            FilterCriteria gteResult = parser.parseCql2Json(objectMapper.readTree(gteJson));
            assertEquals(">=", gteResult.getOperator());

            // Test not equal
            String neqJson = """
                {"op": "neq", "args": [{"property": "value"}, 10]}
                """;
            FilterCriteria neqResult = parser.parseCql2Json(objectMapper.readTree(neqJson));
            assertEquals("<>", neqResult.getOperator());
        }

        @Test
        @DisplayName("should parse JSON BETWEEN expression")
        void shouldParseJsonBetween() throws Exception {
            // Given
            String json = """
                {
                    "op": "between",
                    "args": [
                        {"property": "cloud_cover"},
                        0,
                        20
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("BETWEEN", result.getOperator());
            assertEquals("cloud_cover", result.getPropertyName());
            assertNotNull(result.getValue());
            assertTrue(result.getValue() instanceof List);
        }

        @Test
        @DisplayName("should parse JSON IN expression")
        void shouldParseJsonIn() throws Exception {
            // Given
            String json = """
                {
                    "op": "in",
                    "args": [
                        {"property": "platform"},
                        ["sentinel-2", "landsat-8", "modis"]
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("IN", result.getOperator());
            assertEquals("platform", result.getPropertyName());
            assertNotNull(result.getValue());
            assertTrue(result.getValue() instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) result.getValue();
            assertEquals(3, values.size());
        }

        @Test
        @DisplayName("should parse JSON IS NULL expression")
        void shouldParseJsonIsNull() throws Exception {
            // Given
            String json = """
                {
                    "op": "isNull",
                    "args": [
                        {"property": "end_datetime"}
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.COMPARISON, result.getType());
            assertEquals("IS NULL", result.getOperator());
            assertEquals("end_datetime", result.getPropertyName());
            assertNull(result.getValue());
        }

        @Test
        @DisplayName("should return null for null JSON node")
        void shouldReturnNullForNullJsonNode() {
            // When
            FilterCriteria result = parser.parseCql2Json(null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should throw on invalid JSON structure")
        void shouldThrowOnInvalidJsonStructure() throws Exception {
            // Given
            String json = """
                {"invalid": "structure"}
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseCql2Json(jsonNode));
        }

        @Test
        @DisplayName("should throw on unknown operator")
        void shouldThrowOnUnknownOperator() throws Exception {
            // Given
            String json = """
                {
                    "op": "unknown_op",
                    "args": [{"property": "test"}, "value"]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseCql2Json(jsonNode));
        }
    }

    @Nested
    @DisplayName("Filter Language Selection Tests")
    class FilterLanguageTests {

        @Test
        @DisplayName("should parse with cql2-json filter language")
        void shouldParseWithCql2Json() throws Exception {
            // Given
            String filter = """
                {"op": "=", "args": [{"property": "platform"}, "sentinel-2"]}
                """;

            // When
            FilterCriteria result = parser.parseFilter(filter, "cql2-json");

            // Then
            assertNotNull(result);
            assertEquals("=", result.getOperator());
        }

        @Test
        @DisplayName("should parse with cql2-text filter language")
        void shouldParseWithCql2Text() {
            // Given
            String filter = "platform = 'sentinel-2'";

            // When
            FilterCriteria result = parser.parseFilter(filter, "cql2-text");

            // Then
            assertNotNull(result);
            assertEquals("=", result.getOperator());
        }

        @Test
        @DisplayName("should default to cql2-text when filter language not specified")
        void shouldDefaultToCql2Text() {
            // Given
            String filter = "platform = 'sentinel-2'";

            // When
            FilterCriteria result = parser.parseFilter(filter, null);

            // Then
            assertNotNull(result);
            assertEquals("=", result.getOperator());
        }

        @Test
        @DisplayName("should return null for empty filter string")
        void shouldReturnNullForEmptyFilterString() {
            // When
            FilterCriteria result = parser.parseFilter("", "cql2-text");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should return null for blank filter string")
        void shouldReturnNullForBlankFilterString() {
            // When
            FilterCriteria result = parser.parseFilter("   ", "cql2-text");

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Temporal Operator Tests")
    class TemporalOperatorTests {

        @Test
        @DisplayName("should parse JSON temporal T_BEFORE")
        void shouldParseJsonTemporalBefore() throws Exception {
            // Given
            String json = """
                {
                    "op": "t_before",
                    "args": [
                        {"property": "datetime"},
                        "2023-01-01T00:00:00Z"
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.TEMPORAL, result.getType());
            assertEquals("T_BEFORE", result.getOperator());
            assertEquals("datetime", result.getPropertyName());
        }

        @Test
        @DisplayName("should parse JSON temporal T_AFTER")
        void shouldParseJsonTemporalAfter() throws Exception {
            // Given
            String json = """
                {
                    "op": "t_after",
                    "args": [
                        {"property": "datetime"},
                        "2023-01-01T00:00:00Z"
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.TEMPORAL, result.getType());
            assertEquals("T_AFTER", result.getOperator());
        }

        @Test
        @DisplayName("should parse JSON temporal T_DURING with interval")
        void shouldParseJsonTemporalDuring() throws Exception {
            // Given
            String json = """
                {
                    "op": "t_during",
                    "args": [
                        {"property": "datetime"},
                        {"interval": ["2023-01-01T00:00:00Z", "2023-12-31T23:59:59Z"]}
                    ]
                }
                """;
            JsonNode jsonNode = objectMapper.readTree(json);

            // When
            FilterCriteria result = parser.parseCql2Json(jsonNode);

            // Then
            assertNotNull(result);
            assertEquals(FilterType.TEMPORAL, result.getType());
            assertEquals("T_DURING", result.getOperator());
            assertEquals("datetime", result.getPropertyName());
            assertNotNull(result.getValue());
        }
    }

    @Nested
    @DisplayName("Geometry Conversion Tests")
    class GeometryConversionTests {

        @Test
        @DisplayName("should convert geometry to GeoJSON")
        void shouldConvertGeometryToGeoJson() {
            // Given
            String cql = "INTERSECTS(geometry, POLYGON((0 0, 1 0, 1 1, 0 1, 0 0)))";
            FilterCriteria result = parser.parseCql2Text(cql);
            Geometry geometry = (Geometry) result.getValue();

            // When
            String geoJson = parser.geometryToGeoJson(geometry);

            // Then
            assertNotNull(geoJson);
            assertTrue(geoJson.contains("Polygon"));
            assertTrue(geoJson.contains("coordinates"));
        }

        @Test
        @DisplayName("should return null for null geometry")
        void shouldReturnNullForNullGeometry() {
            // When
            String result = parser.geometryToGeoJson(null);

            // Then
            assertNull(result);
        }
    }
}
