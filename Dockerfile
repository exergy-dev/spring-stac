# Spring Boot STAC API Server
# Multi-stage build: build JAR in first stage, run in minimal second stage

# =============================================================================
# Build Stage
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper and build files first (for better layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached unless build files change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application JAR (skip tests for faster builds)
RUN ./gradlew bootJar --no-daemon -x test

# =============================================================================
# Runtime Stage
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Labels for container metadata
LABEL org.opencontainers.image.title="Spring STAC API Server"
LABEL org.opencontainers.image.description="STAC API v1.0.0 compliant server built with Spring Boot"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Spring STAC Project"
LABEL org.opencontainers.image.source="https://github.com/spring-stac/spring-stac"
LABEL org.opencontainers.image.licenses="Apache-2.0"

# Create non-root user for security
RUN addgroup -g 1001 -S stac && \
    adduser -u 1001 -S stac -G stac

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder --chown=stac:stac /build/build/libs/stac-server-*[!-plain].jar app.jar

# Switch to non-root user
USER stac

# Expose the application port
EXPOSE 8080

# Health check - uses Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM configuration:
# -XX:+UseZGC: Use Z Garbage Collector for low latency
# -XX:MaxRAMPercentage=75.0: Use up to 75% of container memory for heap
# -Djava.security.egd: Faster random number generation
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
