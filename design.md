# STAC Server - Software Design Document
## Java 21 Spring Boot Implementation

**Version:** 1.0  
**Date:** January 2026  
**Author:** Software Architecture Team

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Overview](#2-system-overview)
3. [Architecture](#3-architecture)
4. [STAC Specification Compliance](#4-stac-specification-compliance)
5. [Component Design](#5-component-design)
6. [Repository Pattern](#6-repository-pattern)
7. [Data Models](#7-data-models)
8. [API Design](#8-api-design)
9. [Technology Stack](#9-technology-stack)
10. [Implementation Plan](#10-implementation-plan)
11. [Testing Strategy](#11-testing-strategy)
12. [Performance Considerations](#12-performance-considerations)
13. [Security](#13-security)
14. [Deployment](#14-deployment)

---

## 1. Executive Summary

### 1.1 Purpose
This document outlines the design for a STAC (SpatioTemporal Asset Catalog) API server implementation using Java 21 and Spring Boot. The system will be fully compatible with existing Python FastAPI STAC implementations while providing enterprise-grade features, performance, and maintainability.

### 1.2 Goals
- **Specification Compliance**: Full STAC API v1.0.0 compliance
- **Interoperability**: Drop-in replacement for existing Python FastAPI implementations
- **Extensibility**: Plugin architecture for multiple backend data stores
- **Performance**: Leverage Java 21 virtual threads and modern JVM optimizations
- **Maintainability**: Clean architecture with well-defined interfaces

### 1.3 Key Features
- Support for pgSTAC (PostgreSQL with PostGIS)
- Support for Elasticsearch/OpenSearch
- Extensible repository pattern for additional backends
- RESTful API following STAC specification
- Geospatial query support
- Pagination and filtering
- Transaction support (STAC Transaction extension)
- CQL2 filter support (Text and JSON)

---

## 2. System Overview

### 2.1 What is STAC?
STAC (SpatioTemporal Asset Catalog) is a specification for describing geospatial information assets in a standardized way. It consists of:
- **Collections**: Groups of related items
- **Items**: Individual geospatial assets with metadata
- **Assets**: The actual data files (images, metadata, etc.)
- **Catalog**: The overall structure organizing collections

### 2.2 System Context
```
┌─────────────────────────────────────────────────────────┐
│                     Client Applications                  │
│  (Web Apps, GIS Tools, Python Clients, etc.)            │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              STAC API (Spring Boot)                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │         REST Controllers                         │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Service Layer                            │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Repository Interface                     │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
│   pgSTAC        │ │ Elasticsearch│ │    Future    │
│  (PostgreSQL)   │ │  OpenSearch  │ │  Backends    │
└─────────────────┘ └──────────────┘ └──────────────┘
```

---

## 3. Architecture

### 3.1 Layered Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  REST Controllers (STAC API Endpoints)                     │  │
│  │  - Landing Page, Conformance, Collections, Items, Search  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Business Logic                                            │  │
│  │  - STAC Validation, CQL2 Parsing, Search Logic            │  │
│  │  - GeoJSON Serialization/Deserialization                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Interface                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  StacRepository (Interface)                                │  │
│  │  - CRUD Operations, Search, Filter, Aggregations          │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
            │                         │                      │
            ▼                         ▼                      ▼
┌────────────────────┐  ┌──────────────────────┐  ┌────────────────┐
│ PgStacRepository   │  │ ElasticsearchRepo    │  │  Future Impls  │
│ (PostgreSQL +      │  │ (Elasticsearch/      │  │  - MongoDB     │
│  PostGIS)          │  │  OpenSearch)         │  │  - DynamoDB    │
└────────────────────┘  └──────────────────────┘  └────────────────┘
```

### 3.2 Key Design Principles

- **Separation of Concerns**: Clear boundaries between layers
- **Dependency Inversion**: Repository interface defines contracts
- **Open/Closed Principle**: Open for extension (new backends), closed for modification
- **Single Responsibility**: Each component has one reason to change

---

## 4. STAC Specification Compliance

### 4.1 Core Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Landing page |
| `/conformance` | GET | Conformance classes |
| `/collections` | GET | List all collections |
| `/collections/{collectionId}` | GET | Get collection metadata |
| `/collections/{collectionId}/items` | GET | List items in collection |
| `/collections/{collectionId}/items/{itemId}` | GET | Get specific item |
| `/search` | GET/POST | Cross-collection search |

### 4.2 STAC Extensions Support

- **Filter Extension**: CQL2-JSON and CQL2-Text support
- **Query Extension**: Additional property filtering
- **Sort Extension**: Custom sorting
- **Fields Extension**: Select specific fields
- **Context Extension**: Result metadata
- **Transaction Extension**: Create, Update, Delete operations

### 4.3 Conformance Classes

```
http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core
http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30
http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson
https://api.stacspec.org/v1.0.0/core
https://api.stacspec.org/v1.0.0/collections
https://api.stacspec.org/v1.0.0/item-search
https://api.stacspec.org/v1.0.0/item-search#filter
https://api.stacspec.org/v1.0.0/item-search#query
https://api.stacspec.org/v1.0.0/item-search#sort
https://api.stacspec.org/v1.0.0/item-search#fields
https://api.stacspec.org/v1.0.0/item-search#context
https://api.stacspec.org/v1.0.0/ogcapi-features
```

---

## 5. Component Design

### 5.1 Controllers

**LandingController**
```java
@RestController
@RequestMapping("/")
public class LandingController {
    @GetMapping
    public ResponseEntity<LandingPage> getLandingPage();
    
    @GetMapping("/conformance")
    public ResponseEntity<Conformance> getConformance();
}
```

**CollectionsController**
```java
@RestController
@RequestMapping("/collections")
public class CollectionsController {
    @GetMapping
    public ResponseEntity<Collections> getCollections(
        @RequestParam(required = false) Integer limit);
    
    @GetMapping("/{collectionId}")
    public ResponseEntity<Collection> getCollection(
        @PathVariable String collectionId);
    
    @GetMapping("/{collectionId}/items")
    public ResponseEntity<ItemCollection> getItems(
        @PathVariable String collectionId,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String bbox,
        @RequestParam(required = false) String datetime);
    
    @GetMapping("/{collectionId}/items/{itemId}")
    public ResponseEntity<Item> getItem(
        @PathVariable String collectionId,
        @PathVariable String itemId);
}
```

**SearchController**
```java
@RestController
@RequestMapping("/search")
public class SearchController {
    @GetMapping
    public ResponseEntity<ItemCollection> searchGet(
        @ModelAttribute SearchRequest request);
    
    @PostMapping
    public ResponseEntity<ItemCollection> searchPost(
        @RequestBody SearchRequest request);
}
```

### 5.2 Service Layer

**StacService**
```java
@Service
public class StacService {
    private final StacRepository repository;
    private final Cql2FilterParser cqlParser;
    private final ValidationService validationService;
    
    public ItemCollection search(SearchRequest request);
    public Collection getCollection(String collectionId);
    public Item getItem(String collectionId, String itemId);
    public void validateStacItem(Item item);
}
```

**Cql2FilterParser**
```java
@Component
public class Cql2FilterParser {
    private final ObjectMapper objectMapper;
    
    // Parse CQL2 Text format
    public FilterCriteria parseCql2Text(String cql);
    
    // Parse CQL2 JSON format
    public FilterCriteria parseCql2Json(JsonNode cql);
    
    // Convert to backend-specific query
    public <T> T convertToBackendQuery(
        FilterCriteria criteria, Class<T> queryClass);
}
```

---

## 6. Repository Pattern

### 6.1 Core Repository Interface

```java
public interface StacRepository {
    
    // Collection Operations
    Page<Collection> findAllCollections(Pageable pageable);
    Optional<Collection> findCollectionById(String id);
    Collection saveCollection(Collection collection);
    void deleteCollection(String id);
    long countCollections();
    
    // Item Operations
    Page<Item> findItemsByCollection(
        String collectionId, 
        ItemQueryCriteria criteria,
        Pageable pageable);
    Optional<Item> findItemById(String collectionId, String itemId);
    Item saveItem(String collectionId, Item item);
    void deleteItem(String collectionId, String itemId);
    long countItemsByCollection(String collectionId);
    
    // Search Operations
    ItemCollection search(SearchCriteria criteria);
    
    // Aggregation Operations
    Map<String, Object> aggregate(AggregationRequest request);
    
    // Transaction Support
    BulkOperationResult bulkInsert(String collectionId, List<Item> items);
    BulkOperationResult bulkUpdate(String collectionId, List<Item> items);
    BulkOperationResult bulkDelete(String collectionId, List<String> itemIds);
}
```

### 6.2 Search Criteria

```java
@Data
@Builder
public class SearchCriteria {
    private List<String> collections;
    private List<String> ids;
    private BoundingBox bbox;
    private TemporalExtent datetime;
    private Integer limit;
    private String token; // pagination cursor
    private FilterCriteria filter; // CQL2 filter
    private List<SortBy> sortBy;
    private FieldSelector fields;
    private Map<String, QueryOperation> query; // property queries
}

@Data
public class FilterCriteria {
    private FilterType type; // AND, OR, NOT, COMPARISON, SPATIAL, TEMPORAL
    private String operator;
    private List<FilterCriteria> children;
    private String propertyName;
    private Object value;
}
```

### 6.3 pgSTAC Implementation

```java
@Repository
@Profile("pgstac")
public class PgStacRepositoryImpl implements StacRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final PgStacFunctionMapper functionMapper;
    
    @Override
    public ItemCollection search(SearchCriteria criteria) {
        // Build pgSTAC search JSON
        String searchJson = buildPgStacSearchJson(criteria);
        
        // Call pgSTAC search function
        String sql = "SELECT * FROM search(CAST(? AS jsonb))";
        
        List<Item> items = jdbcTemplate.query(
            sql,
            new Object[]{searchJson},
            new ItemRowMapper()
        );
        
        return ItemCollection.builder()
            .type("FeatureCollection")
            .features(items)
            .build();
    }
    
    @Override
    public Optional<Item> findItemById(String collectionId, String itemId) {
        String sql = "SELECT * FROM get_item(?, ?)";
        try {
            Item item = jdbcTemplate.queryForObject(
                sql,
                new Object[]{itemId, collectionId},
                new ItemRowMapper()
            );
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    private String buildPgStacSearchJson(SearchCriteria criteria) {
        ObjectNode searchNode = objectMapper.createObjectNode();
        
        if (criteria.getCollections() != null) {
            searchNode.set("collections", 
                objectMapper.valueToTree(criteria.getCollections()));
        }
        
        if (criteria.getBbox() != null) {
            searchNode.set("bbox", 
                objectMapper.valueToTree(criteria.getBbox().toArray()));
        }
        
        if (criteria.getDatetime() != null) {
            searchNode.put("datetime", criteria.getDatetime().toString());
        }
        
        if (criteria.getFilter() != null) {
            searchNode.set("filter", 
                convertFilterToCql2Json(criteria.getFilter()));
        }
        
        searchNode.put("limit", criteria.getLimit());
        
        return searchNode.toString();
    }
}
```

**Key Features:**
- Uses pgSTAC stored procedures (`search()`, `create_item()`, `get_item()`)
- Leverages PostGIS for spatial operations
- Efficient JSON handling with JSONB
- Native pagination support via cursors

### 6.4 Elasticsearch/OpenSearch Implementation

```java
@Repository
@Profile("elasticsearch")
public class ElasticsearchStacRepositoryImpl implements StacRepository {
    
    private final ElasticsearchClient elasticsearchClient;
    private final Cql2ToElasticsearchConverter queryConverter;
    
    @Override
    public ItemCollection search(SearchCriteria criteria) {
        SearchRequest searchRequest = buildSearchRequest(criteria);
        
        try {
            SearchResponse<Item> response = elasticsearchClient.search(
                searchRequest, 
                Item.class
            );
            
            return convertToItemCollection(response);
        } catch (IOException e) {
            throw new RepositoryException("Search failed", e);
        }
    }
    
    private SearchRequest buildSearchRequest(SearchCriteria criteria) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        
        // Collection filter
        if (criteria.getCollections() != null && !criteria.getCollections().isEmpty()) {
            boolQuery.filter(f -> f.terms(t -> t
                .field("collection")
                .terms(tv -> tv.value(
                    criteria.getCollections().stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList())
                ))
            ));
        }
        
        // Spatial filter (bbox)
        if (criteria.getBbox() != null) {
            boolQuery.filter(f -> f.geoShape(gs -> gs
                .field("geometry")
                .shape(s -> s.envelope(e -> e
                    .coordinates(criteria.getBbox().toCoordinatesList())
                ))
            ));
        }
        
        // Temporal filter
        if (criteria.getDatetime() != null) {
            boolQuery.filter(buildTemporalQuery(criteria.getDatetime()));
        }
        
        // CQL2 filter
        if (criteria.getFilter() != null) {
            Query cql2Query = queryConverter.convert(criteria.getFilter());
            boolQuery.filter(cql2Query);
        }
        
        // Build search request
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
            .index("stac-items-*")
            .query(boolQuery.build()._toQuery())
            .size(criteria.getLimit() != null ? criteria.getLimit() : 10);
        
        // Sorting
        if (criteria.getSortBy() != null) {
            criteria.getSortBy().forEach(sort -> 
                requestBuilder.sort(buildSortOptions(sort))
            );
        }
        
        // Search after for pagination
        if (criteria.getToken() != null) {
            requestBuilder.searchAfter(
                parseSearchAfterToken(criteria.getToken())
            );
        }
        
        return requestBuilder.build();
    }
}
```

**Key Features:**
- Geo-shape queries for spatial filtering
- Full-text search capabilities
- Flexible index patterns
- Aggregations for statistics
- Efficient scroll/search_after pagination

### 6.5 Repository Configuration

```java
@Configuration
public class RepositoryConfiguration {
    
    @Bean
    @ConditionalOnProperty(
        name = "stac.repository.type", 
        havingValue = "pgstac"
    )
    public StacRepository pgStacRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        return new PgStacRepositoryImpl(jdbcTemplate, objectMapper);
    }
    
    @Bean
    @ConditionalOnProperty(
        name = "stac.repository.type", 
        havingValue = "elasticsearch"
    )
    public StacRepository elasticsearchRepository(
            ElasticsearchClient client,
            Cql2ToElasticsearchConverter converter) {
        return new ElasticsearchStacRepositoryImpl(client, converter);
    }
}
```

---

## 7. Data Models

### 7.1 Core STAC Models

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {
    @JsonProperty("type")
    private String type = "Feature";
    
    @JsonProperty("stac_version")
    private String stacVersion = "1.0.0";
    
    @JsonProperty("stac_extensions")
    private List<String> stacExtensions;
    
    @JsonProperty("id")
    @NotNull
    private String id;
    
    @JsonProperty("geometry")
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Geometry geometry;
    
    @JsonProperty("bbox")
    private double[] bbox;
    
    @JsonProperty("properties")
    @NotNull
    private Map<String, Object> properties;
    
    @JsonProperty("links")
    private List<Link> links;
    
    @JsonProperty("assets")
    private Map<String, Asset> assets;
    
    @JsonProperty("collection")
    private String collection;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collection {
    @JsonProperty("type")
    private String type = "Collection";
    
    @JsonProperty("stac_version")
    private String stacVersion = "1.0.0";
    
    @JsonProperty("stac_extensions")
    private List<String> stacExtensions;
    
    @JsonProperty("id")
    @NotNull
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    @NotNull
    private String description;
    
    @JsonProperty("keywords")
    private List<String> keywords;
    
    @JsonProperty("license")
    @NotNull
    private String license;
    
    @JsonProperty("providers")
    private List<Provider> providers;
    
    @JsonProperty("extent")
    @NotNull
    private Extent extent;
    
    @JsonProperty("summaries")
    private Map<String, Object> summaries;
    
    @JsonProperty("links")
    private List<Link> links;
    
    @JsonProperty("assets")
    private Map<String, Asset> assets;
}

@Data
@Builder
public class Extent {
    private SpatialExtent spatial;
    private TemporalExtent temporal;
}

@Data
@Builder
public class SpatialExtent {
    private List<double[]> bbox;
}

@Data
@Builder
public class TemporalExtent {
    private List<List<String>> interval;
}
```

### 7.2 JTS Geometry Integration

```java
// Custom serializers/deserializers for JTS geometries
public class GeometrySerializer extends JsonSerializer<Geometry> {
    private final GeoJsonWriter writer = new GeoJsonWriter();
    
    @Override
    public void serialize(
            Geometry geometry,
            JsonGenerator gen,
            SerializerProvider serializers) throws IOException {
        if (geometry != null) {
            gen.writeRawValue(writer.write(geometry).toString());
        } else {
            gen.writeNull();
        }
    }
}

public class GeometryDeserializer extends JsonDeserializer<Geometry> {
    private final GeoJsonReader reader = new GeoJsonReader();
    
    @Override
    public Geometry deserialize(
            JsonParser p,
            DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return reader.read(node.toString());
    }
}
```

---

## 8. API Design

### 8.1 Request/Response DTOs

```java
@Data
public class SearchRequest {
    private List<String> collections;
    private List<String> ids;
    private Integer limit = 10;
    private String bbox;
    private String datetime;
    private String intersects;
    private JsonNode filter;
    private String filterLang; // "cql2-text" or "cql2-json"
    private Map<String, Object> query;
    private List<String> sortby;
    private String fields;
    private String token;
}

@Data
@Builder
public class ItemCollection {
    private String type = "FeatureCollection";
    private List<Item> features;
    private List<Link> links;
    private Context context;
    private String timeStamp;
    private Integer numberMatched;
    private Integer numberReturned;
}

@Data
@Builder
public class Context {
    private Integer returned;
    private Integer limit;
    private Integer matched;
}
```

### 8.2 OpenAPI Specification

The API will be documented using SpringDoc OpenAPI:

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "STAC API",
        version = "1.0.0",
        description = "SpatioTemporal Asset Catalog API"
    ),
    servers = {
        @Server(url = "/", description = "Default Server URL")
    }
)
public class OpenApiConfiguration {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components())
            .info(new Info()
                .title("STAC API")
                .version("1.0.0")
                .description("STAC API Implementation"));
    }
}
```

---

## 9. Technology Stack

### 9.1 Core Framework
- **Java**: 21 (LTS) with Virtual Threads
- **Spring Boot**: 3.2+
- **Spring Framework**: 6.1+

### 9.2 Geospatial Libraries

#### JTS (Java Topology Suite)
Primary geometry library for spatial operations.
```xml
<dependency>
    <groupId>org.locationtech.jts</groupId>
    <artifactId>jts-core</artifactId>
    <version>1.20.0</version>
</dependency>
```

#### GeoTools
For CQL/ECQL parsing and advanced geospatial operations.
```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>31.0</version>
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-cql</artifactId>
    <version>31.0</version>
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geojson</artifactId>
    <version>31.0</version>
</dependency>
```

#### GeoJSON Libraries

**Option 1: jts2geojson** (Recommended for STAC)
```xml
<dependency>
    <groupId>org.wololo</groupId>
    <artifactId>jts2geojson</artifactId>
    <version>0.18.1</version>
</dependency>
```

**Option 2: Jackson Datatype JTS**
```xml
<dependency>
    <groupId>com.graphhopper.external</groupId>
    <artifactId>jackson-datatype-jts</artifactId>
    <version>2.14</version>
</dependency>
```

**Option 3: GeoLatte-GeoJSON** (For Hibernate Spatial integration)
```xml
<dependency>
    <groupId>org.geolatte</groupId>
    <artifactId>geolatte-geojson</artifactId>
    <version>1.9.1</version>
</dependency>
```

### 9.3 Database Support

#### PostgreSQL/pgSTAC
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-spatial</artifactId>
    <version>6.4.1.Final</version>
</dependency>
```

#### Elasticsearch
```xml
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>8.11.4</version>
</dependency>
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-java</artifactId>
    <version>2.8.0</version>
</dependency>
```

### 9.4 Additional Libraries

```xml
<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- API Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <scope>test</scope>
</dependency>

<!-- Observability -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 9.5 Build Tool

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
    </parent>
    
    <groupId>org.stac</groupId>
    <artifactId>stac-server</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    
    <properties>
        <java.version>21</java.version>
        <jts.version>1.20.0</jts.version>
        <geotools.version>31.0</geotools.version>
    </properties>
</project>
```

---

## 10. Implementation Plan

### Phase 1: Foundation (Weeks 1-2)
- Set up project structure
- Configure Spring Boot application
- Implement core data models (Item, Collection, etc.)
- Set up JTS geometry serialization/deserialization
- Create repository interface

### Phase 2: pgSTAC Implementation (Weeks 3-4)
- Implement pgSTAC repository
- Set up PostgreSQL/PostGIS integration
- Implement basic CRUD operations
- Add search functionality
- Implement CQL2 filter support

### Phase 3: Elasticsearch Implementation (Weeks 5-6)
- Implement Elasticsearch repository
- Set up Elasticsearch client
- Implement search and filter operations
- Add aggregation support
- Implement pagination

### Phase 4: REST API (Weeks 7-8)
- Implement controllers
- Add request validation
- Implement error handling
- Add OpenAPI documentation
- Implement HATEOS links

### Phase 5: Extensions (Weeks 9-10)
- Implement Filter Extension (CQL2)
- Implement Query Extension
- Implement Sort Extension
- Implement Fields Extension
- Implement Context Extension
- Implement Transaction Extension

### Phase 6: Testing & Performance (Weeks 11-12)
- Unit tests
- Integration tests
- Performance testing
- Load testing
- Compatibility testing with Python implementations

---

## 11. Testing Strategy

### 11.1 Unit Tests
```java
@SpringBootTest
class StacServiceTest {
    
    @MockBean
    private StacRepository repository;
    
    @Autowired
    private StacService service;
    
    @Test
    void shouldSearchItems() {
        SearchCriteria criteria = SearchCriteria.builder()
            .collections(List.of("test-collection"))
            .limit(10)
            .build();
            
        when(repository.search(any())).thenReturn(
            ItemCollection.builder().features(List.of()).build()
        );
        
        ItemCollection result = service.search(
            SearchRequest.builder()
                .collections(List.of("test-collection"))
                .build()
        );
        
        assertNotNull(result);
    }
}
```

### 11.2 Integration Tests with Testcontainers

```java
@SpringBootTest
@Testcontainers
class PgStacRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgis/postgis:15-3.3")
            .withDatabaseName("stac")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-pgstac.sql");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private StacRepository repository;
    
    @Test
    void shouldSaveAndRetrieveItem() {
        Item item = createTestItem();
        repository.saveItem("test-collection", item);
        
        Optional<Item> retrieved = repository.findItemById(
            "test-collection", 
            item.getId()
        );
        
        assertTrue(retrieved.isPresent());
        assertEquals(item.getId(), retrieved.get().getId());
    }
}
```

### 11.3 Compliance Tests

Use the official STAC API validator to ensure compliance:
```bash
stac-api-validator \
  --root-url http://localhost:8080 \
  --conformance core \
  --conformance item-search \
  --conformance filter
```

---

## 12. Performance Considerations

### 12.1 Virtual Threads (Java 21)

```java
@Configuration
public class AsyncConfiguration {
    
    @Bean
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

Enable virtual threads for Tomcat:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 12.2 Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 12.3 Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("collections", "items");
    }
}

@Service
public class StacService {
    
    @Cacheable(value = "collections", key = "#id")
    public Collection getCollection(String id) {
        return repository.findCollectionById(id)
            .orElseThrow(() -> new NotFoundException("Collection not found"));
    }
}
```

### 12.4 Database Indexing

For pgSTAC:
```sql
-- Spatial index
CREATE INDEX items_geometry_idx ON items USING GIST (geometry);

-- Property indices
CREATE INDEX items_datetime_idx ON items ((properties->>'datetime'));
CREATE INDEX items_collection_idx ON items (collection);

-- JSONB index for property queries
CREATE INDEX items_properties_idx ON items USING GIN (properties);
```

For Elasticsearch:
```json
{
  "mappings": {
    "properties": {
      "geometry": {
        "type": "geo_shape"
      },
      "properties.datetime": {
        "type": "date"
      },
      "collection": {
        "type": "keyword"
      }
    }
  }
}
```

---

## 13. Security

### 13.1 Authentication & Authorization

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/conformance", "/collections/**").permitAll()
                .requestMatchers("/search").permitAll()
                .requestMatchers(HttpMethod.POST, "/collections/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/collections/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/collections/**").authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        
        return http.build();
    }
}
```

### 13.2 Rate Limiting

```java
@Configuration
public class RateLimitConfiguration {
    
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(100.0); // 100 requests per second
    }
}

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        
        if (!rateLimiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }
}
```

### 13.3 Input Validation

```java
@RestController
public class SearchController {
    
    @PostMapping("/search")
    public ResponseEntity<ItemCollection> search(
            @Valid @RequestBody SearchRequest request) {
        
        // Validate bbox
        if (request.getBbox() != null) {
            validateBbox(request.getBbox());
        }
        
        // Validate datetime
        if (request.getDatetime() != null) {
            validateDatetime(request.getDatetime());
        }
        
        return ResponseEntity.ok(stacService.search(request));
    }
}
```

---

## 14. Deployment

### 14.1 Docker Configuration

**Dockerfile**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/stac-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", \
    "app.jar"]
```

**docker-compose.yml**
```yaml
version: '3.8'

services:
  stac-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=pgstac
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/stac
      - SPRING_DATASOURCE_USERNAME=stac
      - SPRING_DATASOURCE_PASSWORD=stac
    depends_on:
      - postgres
    
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      - POSTGRES_DB=stac
      - POSTGRES_USER=stac
      - POSTGRES_PASSWORD=stac
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-pgstac.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

volumes:
  postgres-data:
```

### 14.2 Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stac-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: stac-server
  template:
    metadata:
      labels:
        app: stac-server
    spec:
      containers:
      - name: stac-server
        image: stac-server:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "pgstac"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: stac-db-secret
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: stac-server
spec:
  selector:
    app: stac-server
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

### 14.3 Configuration Management

**application.yml**
```yaml
spring:
  application:
    name: stac-server
  
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/stac}
    username: ${SPRING_DATASOURCE_USERNAME:stac}
    password: ${SPRING_DATASOURCE_PASSWORD:stac}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.spatial.dialect.postgis.PostgisDialect
    show-sql: false
  
  threads:
    virtual:
      enabled: true

stac:
  repository:
    type: ${STAC_REPOSITORY_TYPE:pgstac}
  
  api:
    title: "STAC API"
    description: "SpatioTemporal Asset Catalog API"
    version: "1.0.0"
    default-limit: 10
    max-limit: 10000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Appendices

### A. CQL2 to Backend Query Mapping

This section documents how CQL2 filter expressions are translated to backend-specific queries.

### B. STAC Extension Support Matrix

| Extension | Status | Notes |
|-----------|--------|-------|
| Filter | ✅ Planned | CQL2-JSON, CQL2-Text |
| Query | ✅ Planned | Property-based filtering |
| Sort | ✅ Planned | Multi-field sorting |
| Fields | ✅ Planned | Include/exclude fields |
| Context | ✅ Planned | Result metadata |
| Transaction | ✅ Planned | CRUD operations |
| Aggregation | 🔄 Future | Faceted search |

### C. Migration Guide from Python FastAPI

Steps for migrating from Python FastAPI STAC implementation:
1. Export existing data
2. Deploy Java STAC server
3. Import data using Transaction API
4. Update client configurations
5. Run validation tests
6. Switch traffic to new server

---

**Document Version History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01 | Architecture Team | Initial version |
