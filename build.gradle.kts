plugins {
    java
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.stac"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    maven {
        name = "osgeo"
        url = uri("https://repo.osgeo.org/repository/release/")
    }
    mavenCentral()
}

val jtsVersion = "1.20.0"
val geotoolsVersion = "31.0"
val jts2geojsonVersion = "0.18.1"
val elasticsearchVersion = "8.11.4"
val opensearchVersion = "2.8.0"
val springdocVersion = "2.3.0"
val guavaVersion = "33.0.0-jre"
val caffeineVersion = "3.1.8"
val testcontainersVersion = "1.19.3"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // PostgreSQL/PostGIS
    implementation("org.postgresql:postgresql")
    implementation("org.hibernate.orm:hibernate-spatial")

    // Elasticsearch
    implementation("co.elastic.clients:elasticsearch-java:$elasticsearchVersion")
    implementation("org.opensearch.client:opensearch-java:$opensearchVersion")

    // JTS Geometry
    implementation("org.locationtech.jts:jts-core:$jtsVersion")

    // GeoTools
    implementation("org.geotools:gt-api:$geotoolsVersion")
    implementation("org.geotools:gt-main:$geotoolsVersion")
    implementation("org.geotools:gt-cql:$geotoolsVersion")
    implementation("org.geotools:gt-geojson:$geotoolsVersion")
    runtimeOnly("org.geotools:gt-epsg-hsql:$geotoolsVersion")

    // GeoJSON
    implementation("org.wololo:jts2geojson:$jts2geojsonVersion")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    // Rate Limiting
    implementation("com.google.guava:guava:$guavaVersion")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:elasticsearch")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}
