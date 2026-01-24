package org.stac.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;

/**
 * Test configuration for Elasticsearch integration tests.
 * <p>
 * Creates indices with proper mappings for STAC collections and items
 * before tests run. This includes:
 * <ul>
 *   <li>stac-collections index with keyword fields</li>
 *   <li>stac-items-* index template with geo_shape, date, and keyword mappings</li>
 * </ul>
 */
@TestConfiguration
public class ElasticsearchTestConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchTestConfiguration.class);

    private static final String COLLECTIONS_INDEX = "stac-collections";
    private static final String ITEMS_INDEX_TEMPLATE = "stac-items-template";
    private static final String ITEMS_INDEX_PATTERN = "stac-items-*";

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * Creates the required indices and index templates after the test context is initialized.
     */
    @PostConstruct
    public void createIndices() throws IOException {
        createCollectionsIndex();
        createItemsIndexTemplate();
    }

    /**
     * Creates the stac-collections index with proper mappings for STAC collections.
     * <p>
     * Mappings include:
     * <ul>
     *   <li>id - keyword</li>
     *   <li>type - keyword</li>
     *   <li>stac_version - keyword</li>
     *   <li>title - text with keyword sub-field</li>
     *   <li>description - text</li>
     *   <li>license - keyword</li>
     *   <li>keywords - keyword array</li>
     * </ul>
     */
    public void createCollectionsIndex() throws IOException {
        // Check if index exists
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index(COLLECTIONS_INDEX))
                .value();

        if (indexExists) {
            log.info("Collections index '{}' already exists, skipping creation", COLLECTIONS_INDEX);
            return;
        }

        log.info("Creating collections index: {}", COLLECTIONS_INDEX);

        CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(COLLECTIONS_INDEX)
                .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                        .refreshInterval(t -> t.time("1s"))
                )
                .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("stac_version", p -> p.keyword(k -> k))
                        .properties("stac_extensions", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t
                                .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
                        ))
                        .properties("description", p -> p.text(t -> t))
                        .properties("license", p -> p.keyword(k -> k))
                        .properties("keywords", p -> p.keyword(k -> k))
                        .properties("extent", p -> p.object(o -> o
                                .properties("spatial", sp -> sp.object(so -> so
                                        .properties("bbox", bp -> bp.double_(d -> d))
                                ))
                                .properties("temporal", tp -> tp.object(to -> to
                                        .properties("interval", ip -> ip.keyword(k -> k))
                                ))
                        ))
                        .properties("links", p -> p.nested(n -> n
                                .properties("href", hp -> hp.keyword(k -> k))
                                .properties("rel", rp -> rp.keyword(k -> k))
                                .properties("type", tp -> tp.keyword(k -> k))
                                .properties("title", tp -> tp.text(t -> t))
                        ))
                        .properties("providers", p -> p.nested(n -> n
                                .properties("name", np -> np.text(t -> t
                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
                                ))
                                .properties("url", up -> up.keyword(k -> k))
                                .properties("roles", rp -> rp.keyword(k -> k))
                        ))
                        .properties("summaries", p -> p.object(o -> o.enabled(true)))
                        .properties("assets", p -> p.object(o -> o.enabled(true)))
                )
        );

        elasticsearchClient.indices().create(request);
        log.info("Collections index '{}' created successfully", COLLECTIONS_INDEX);
    }

    /**
     * Creates an index template for stac-items-* indices with proper mappings.
     * <p>
     * Mappings include:
     * <ul>
     *   <li>id - keyword</li>
     *   <li>type - keyword</li>
     *   <li>collection - keyword</li>
     *   <li>geometry - geo_shape</li>
     *   <li>bbox - double array</li>
     *   <li>properties.datetime - date</li>
     *   <li>properties.* - dynamic mapped</li>
     * </ul>
     */
    public void createItemsIndexTemplate() throws IOException {
        log.info("Creating items index template: {}", ITEMS_INDEX_TEMPLATE);

        PutIndexTemplateRequest request = PutIndexTemplateRequest.of(builder -> builder
                .name(ITEMS_INDEX_TEMPLATE)
                .indexPatterns(ITEMS_INDEX_PATTERN)
                .priority(100)
                .template(t -> t
                        .settings(s -> s
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                                .refreshInterval(tr -> tr.time("1s"))
                        )
                        .mappings(m -> m
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("type", p -> p.keyword(k -> k))
                                .properties("stac_version", p -> p.keyword(k -> k))
                                .properties("stac_extensions", p -> p.keyword(k -> k))
                                .properties("collection", p -> p.keyword(k -> k))
                                .properties("geometry", p -> p.geoShape(g -> g))
                                .properties("bbox", p -> p.double_(d -> d))
                                .properties("properties", p -> p.object(o -> o
                                        .properties("datetime", dp -> dp.date(d -> d
                                                .format("strict_date_optional_time||epoch_millis")
                                        ))
                                        .properties("start_datetime", dp -> dp.date(d -> d
                                                .format("strict_date_optional_time||epoch_millis")
                                        ))
                                        .properties("end_datetime", dp -> dp.date(d -> d
                                                .format("strict_date_optional_time||epoch_millis")
                                        ))
                                        .properties("created", dp -> dp.date(d -> d
                                                .format("strict_date_optional_time||epoch_millis")
                                        ))
                                        .properties("updated", dp -> dp.date(d -> d
                                                .format("strict_date_optional_time||epoch_millis")
                                        ))
                                        .properties("title", tp -> tp.text(tx -> tx
                                                .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
                                        ))
                                        .properties("description", tp -> tp.text(tx -> tx))
                                        .properties("platform", pp -> pp.keyword(k -> k))
                                        .properties("constellation", cp -> cp.keyword(k -> k))
                                        .properties("instrument", ip -> ip.keyword(k -> k))
                                        .properties("gsd", gp -> gp.float_(f -> f))
                                        .properties("eo:cloud_cover", cp -> cp.float_(f -> f))
                                ))
                                .properties("links", p -> p.nested(n -> n
                                        .properties("href", hp -> hp.keyword(k -> k))
                                        .properties("rel", rp -> rp.keyword(k -> k))
                                        .properties("type", tp -> tp.keyword(k -> k))
                                        .properties("title", tilp -> tilp.text(txt -> txt))
                                ))
                                .properties("assets", p -> p.object(o -> o.enabled(true)))
                        )
                )
        );

        elasticsearchClient.indices().putIndexTemplate(request);
        log.info("Items index template '{}' created successfully", ITEMS_INDEX_TEMPLATE);
    }

    /**
     * Creates a specific items index for a collection.
     *
     * @param collectionId the collection ID
     */
    public void createItemsIndex(String collectionId) throws IOException {
        String indexName = "stac-items-" + collectionId;

        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();

        if (indexExists) {
            log.info("Items index '{}' already exists, skipping creation", indexName);
            return;
        }

        log.info("Creating items index: {}", indexName);

        // The template will be applied automatically, but we create explicitly for immediate availability
        CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
        );

        elasticsearchClient.indices().create(request);
        log.info("Items index '{}' created successfully", indexName);
    }

    /**
     * Refreshes all STAC indices to make documents immediately searchable.
     */
    public void refreshIndices() throws IOException {
        elasticsearchClient.indices().refresh(r -> r.index(COLLECTIONS_INDEX, ITEMS_INDEX_PATTERN));
    }

    /**
     * Deletes all documents from the collections index.
     */
    public void clearCollectionsIndex() throws IOException {
        elasticsearchClient.deleteByQuery(d -> d
                .index(COLLECTIONS_INDEX)
                .query(q -> q.matchAll(m -> m))
        );
        refreshIndices();
    }

    /**
     * Deletes all items indices.
     */
    public void clearItemsIndices() throws IOException {
        try {
            elasticsearchClient.indices().delete(d -> d.index(ITEMS_INDEX_PATTERN).ignoreUnavailable(true));
        } catch (Exception e) {
            log.warn("Could not delete items indices: {}", e.getMessage());
        }
    }

    /**
     * Clears all test data from indices.
     */
    public void clearAllData() throws IOException {
        clearCollectionsIndex();
        clearItemsIndices();
    }
}
