# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring-stac is a Java 21 Spring Boot implementation of the STAC (SpatioTemporal Asset Catalog) API server. It provides full STAC API v1.0.0 compliance with support for multiple backend data stores (pgSTAC/PostgreSQL and Elasticsearch).

See `design.md` for the complete software design document.

## Build and Test Commands

This project uses Gradle with automatic Java 21 toolchain support. The Gradle wrapper will download both Gradle and the required JDK if not present (via Foojay resolver).

**Note:** If your system Java is older than 17, set JAVA_HOME to the local JDK 21 installation:
```bash
export JAVA_HOME=~/.local/jdk
```

```bash
# Build (downloads Gradle and Java 21 if needed)
./gradlew build

# Build without tests
./gradlew build -x test

# Run all tests
./gradlew test

# Run integration tests (requires Docker for TestContainers)
./gradlew test --tests "*IntegrationTest"

# Run application
./gradlew bootRun

# Run with specific backend profile
./gradlew bootRun --args='--spring.profiles.active=pgstac'
./gradlew bootRun --args='--spring.profiles.active=elasticsearch'

# Clean build
./gradlew clean build

# STAC API compliance validation (after server is running)
stac-api-validator --root-url http://localhost:8080 --conformance core --conformance item-search --conformance filter
```

## Architecture

### Layered Architecture
```
Presentation Layer (REST Controllers)
         ↓
Service Layer (Business Logic, CQL2 Parsing)
         ↓
Repository Interface (StacRepository)
         ↓
Backend Implementations (PgStacRepositoryImpl, ElasticsearchStacRepositoryImpl)
```

### Package Structure (planned)
- `org.stac.controller` - REST endpoints (LandingController, CollectionsController, SearchController)
- `org.stac.service` - Business logic (StacService, Cql2FilterParser)
- `org.stac.repository` - Data access interface and implementations
- `org.stac.model` - STAC data models (Item, Collection, SearchRequest, etc.)
- `org.stac.config` - Spring configuration classes
- `org.stac.util` - Geometry serializers/deserializers

### Backend Selection

Backend is selected via `stac.repository.type` property:
- `pgstac` - PostgreSQL with PostGIS (uses pgSTAC stored procedures)
- `elasticsearch` - Elasticsearch/OpenSearch

Implementations use `@ConditionalOnProperty` for bean selection.

## Key Dependencies

- **JTS 1.20.0** - Java Topology Suite for geometry operations
- **GeoTools 31.0** - CQL/ECQL parsing and geospatial operations
- **Hibernate Spatial** - PostGIS dialect for JPA
- **SpringDoc OpenAPI** - API documentation

## STAC Specification

Implements STAC API v1.0.0 with extensions:
- Filter (CQL2-JSON, CQL2-Text)
- Query, Sort, Fields, Context
- Transaction (CRUD operations)
- Aggregation (faceted search)

Core endpoints: `/`, `/conformance`, `/collections`, `/collections/{id}/items`, `/search`, `/aggregate`

## Configuration (Environment Variables)

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `STAC_REPOSITORY_TYPE` | Backend type: `pgstac` or `elasticsearch` | `pgstac` |
| `STAC_PGSTAC_USE_API_HYDRATE` | When `true`, hydration (link injection) happens at API level; when `false`, pgSTAC handles it | `false` |
| `STAC_API_ROOT_URL` | Base URL for generated links | `http://localhost:8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/stac` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `stac` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `stac` |

## STAC API Validation

Run the official STAC API validator to verify compliance:

```bash
# Using the validation script (server must be running)
./scripts/validate-stac-api.sh

# Or manually with stac-api-validator
stac-api-validator \
    --root-url http://localhost:8080 \
    --conformance core \
    --conformance collections \
    --conformance item-search \
    --conformance filter \
    --conformance sort \
    --conformance fields \
    --conformance context \
    --conformance features
```
