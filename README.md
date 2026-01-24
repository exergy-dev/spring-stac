# Spring STAC

A Java 21 Spring Boot implementation of the [STAC (SpatioTemporal Asset Catalog)](https://stacspec.org/) API server. Designed as a drop-in replacement for [stac-fastapi-pgstac](https://github.com/stac-utils/stac-fastapi-pgstac) with enterprise-grade features and performance.

## Features

- **Full STAC API v1.0.0 compliance**
- **Multiple backends**: pgSTAC (PostgreSQL/PostGIS) and Elasticsearch/OpenSearch
- **Extensions**:
  - Filter (CQL2-JSON and CQL2-Text)
  - Query, Sort, Fields, Context
  - Transaction (CRUD operations)
  - Aggregation (faceted search)
- **Free-text search** (`q` parameter)
- **Bulk operations** endpoint
- **Virtual threads** (Java 21) for high concurrency
- **Caching** with Caffeine
- **Security**: OAuth2/JWT, rate limiting, CORS
- **Observability**: Prometheus metrics, health checks
- **Docker & Kubernetes** ready

## Quick Start

### Prerequisites

- Java 21+ (or use the Gradle wrapper which downloads it automatically)
- Docker (for pgSTAC database)

### Run with Docker Compose

```bash
# Start the server with pgSTAC backend
docker-compose up -d

# The API is available at http://localhost:8080
curl http://localhost:8080
```

### Run from Source

```bash
# Clone the repository
git clone https://github.com/exergy-dev/spring-stac.git
cd spring-stac

# Start pgSTAC database
docker-compose up -d postgres

# Run the application
./gradlew bootRun

# Or with a specific backend
./gradlew bootRun --args='--spring.profiles.active=elasticsearch'
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Landing page |
| `/conformance` | GET | Conformance classes |
| `/collections` | GET, POST | List/create collections |
| `/collections/{id}` | GET, PUT, PATCH, DELETE | Collection operations |
| `/collections/{id}/items` | GET, POST | List/create items |
| `/collections/{id}/items/{itemId}` | GET, PUT, PATCH, DELETE | Item operations |
| `/collections/{id}/bulk_items` | POST | Bulk item operations |
| `/collections/{id}/queryables` | GET | Collection queryables |
| `/collections/{id}/aggregate` | GET, POST | Collection aggregation |
| `/search` | GET, POST | Cross-collection search |
| `/aggregate` | GET, POST | Global aggregation |
| `/queryables` | GET | Global queryables |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `STAC_REPOSITORY_TYPE` | Backend: `pgstac` or `elasticsearch` | `pgstac` |
| `STAC_API_ROOT_URL` | Base URL for generated links | `http://localhost:8080` |
| `STAC_PGSTAC_USE_API_HYDRATE` | API-level hydration (like stac-fastapi) | `false` |
| `STAC_CORS_ENABLED` | Enable CORS | `true` |
| `STAC_CORS_ALLOWED_ORIGINS` | Allowed origins | `*` |
| `STAC_SECURITY_ENABLED` | Enable OAuth2/JWT | `false` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/stac` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `stac` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `stac` |

### Application Properties

See [`application.yml`](src/main/resources/application.yml) for all configuration options.

## Development

### Build

```bash
# Build (downloads Gradle and JDK 21 if needed)
./gradlew build

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run integration tests (requires Docker)
./gradlew test --tests "*IntegrationTest"
```

### Project Structure

```
src/main/java/org/stac/
├── config/          # Spring configuration
├── controller/      # REST endpoints
├── exception/       # Exception handling
├── model/           # STAC data models
├── repository/      # Data access layer
│   ├── pgstac/      # PostgreSQL/pgSTAC implementation
│   └── elasticsearch/  # Elasticsearch implementation
├── service/         # Business logic
└── util/            # Utilities (geometry serializers)
```

### Architecture

```
┌─────────────────────────────────────┐
│         REST Controllers            │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│          Service Layer              │
│  (StacService, Cql2FilterParser)    │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│       Repository Interface          │
└────────┬────────────────┬───────────┘
         │                │
┌────────▼───────┐ ┌──────▼──────────┐
│    pgSTAC      │ │  Elasticsearch  │
│  (PostgreSQL)  │ │   (OpenSearch)  │
└────────────────┘ └─────────────────┘
```

## STAC API Validation

Validate compliance with the official STAC API validator:

```bash
# Install validator
pip install stac-api-validator

# Run validation (server must be running)
./scripts/validate-stac-api.sh

# Or manually
stac-api-validator \
    --root-url http://localhost:8080 \
    --conformance core \
    --conformance collections \
    --conformance item-search \
    --conformance filter
```

## Deployment

### Docker

```bash
# Build image
docker build -t spring-stac .

# Run container
docker run -p 8080:8080 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/stac \
    spring-stac
```

### Kubernetes

See [`design.md`](design.md) for Kubernetes deployment manifests.

## Comparison with stac-fastapi-pgstac

| Feature | spring-stac | stac-fastapi-pgstac |
|---------|-------------|---------------------|
| Language | Java 21 | Python |
| Framework | Spring Boot 3.2 | FastAPI |
| Virtual threads | ✅ | ❌ (GIL) |
| Elasticsearch backend | ✅ Built-in | ❌ Separate project |
| Aggregation extension | ✅ | ❌ |
| Bulk operations | ✅ | ✅ |
| Free-text search | ✅ | ✅ |
| CQL2 filters | ✅ | ✅ |
| Transaction extension | ✅ | ✅ |
| JSON Merge Patch | ✅ | ❌ |
| Built-in caching | ✅ Caffeine | ❌ |
| Rate limiting | ✅ | ❌ |
| OAuth2/JWT | ✅ | ❌ |
| Prometheus metrics | ✅ | ❌ |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [STAC Specification](https://stacspec.org/)
- [pgSTAC](https://github.com/stac-utils/pgstac)
- [stac-fastapi](https://github.com/stac-utils/stac-fastapi)
