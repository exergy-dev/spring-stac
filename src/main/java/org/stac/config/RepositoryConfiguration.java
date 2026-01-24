package org.stac.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stac.repository.StacRepository;
import org.stac.repository.elasticsearch.Cql2ToElasticsearchConverter;
import org.stac.repository.elasticsearch.ElasticsearchItemMapper;
import org.stac.repository.elasticsearch.ElasticsearchStacRepositoryImpl;
import org.stac.repository.pgstac.CollectionRowMapper;
import org.stac.repository.pgstac.ItemRowMapper;
import org.stac.repository.pgstac.PgStacFunctionMapper;
import org.stac.repository.pgstac.PgStacRepositoryImpl;

/**
 * Configuration for STAC repository beans.
 * Selects the appropriate repository implementation based on the
 * stac.repository.type property.
 */
@Configuration
public class RepositoryConfiguration {

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "pgstac", matchIfMissing = true)
    public PgStacFunctionMapper pgStacFunctionMapper(ObjectMapper objectMapper) {
        return new PgStacFunctionMapper(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "pgstac", matchIfMissing = true)
    public ItemRowMapper itemRowMapper(ObjectMapper objectMapper) {
        return new ItemRowMapper(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "pgstac", matchIfMissing = true)
    public CollectionRowMapper collectionRowMapper(ObjectMapper objectMapper) {
        return new CollectionRowMapper(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "pgstac", matchIfMissing = true)
    public StacRepository pgStacRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PgStacFunctionMapper functionMapper,
            ItemRowMapper itemRowMapper,
            CollectionRowMapper collectionRowMapper) {
        return new PgStacRepositoryImpl(jdbcTemplate, objectMapper, functionMapper, itemRowMapper, collectionRowMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "elasticsearch")
    public ElasticsearchClient elasticsearchClient(
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String[] uris,
            @Value("${spring.elasticsearch.username:}") String username,
            @Value("${spring.elasticsearch.password:}") String password,
            ObjectMapper objectMapper) {

        HttpHost[] hosts = new HttpHost[uris.length];
        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i];
            String host = uri.replaceFirst("https?://", "").split(":")[0];
            int port = 9200;
            if (uri.contains(":") && !uri.endsWith("://")) {
                String portStr = uri.substring(uri.lastIndexOf(":") + 1);
                port = Integer.parseInt(portStr);
            }
            boolean ssl = uri.startsWith("https://");
            hosts[i] = new HttpHost(host, port, ssl ? "https" : "http");
        }

        RestClientBuilder restClientBuilder = RestClient.builder(hosts);

        if (username != null && !username.isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            restClientBuilder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        RestClient restClient = restClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper));

        return new ElasticsearchClient(transport);
    }

    @Bean
    @ConditionalOnProperty(name = "stac.repository.type", havingValue = "elasticsearch")
    public StacRepository elasticsearchRepository(
            ElasticsearchClient client,
            ObjectMapper objectMapper,
            Cql2ToElasticsearchConverter converter,
            ElasticsearchItemMapper itemMapper) {
        return new ElasticsearchStacRepositoryImpl(client, objectMapper, converter, itemMapper);
    }
}
