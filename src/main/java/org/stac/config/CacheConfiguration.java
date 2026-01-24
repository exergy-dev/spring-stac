package org.stac.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for caching in the STAC API server.
 * <p>
 * Uses Caffeine cache with configurable TTL for caching collections and items.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Value("${stac.cache.collections.ttl-seconds:300}")
    private long collectionsTtlSeconds;

    @Value("${stac.cache.items.ttl-seconds:60}")
    private long itemsTtlSeconds;

    @Value("${stac.cache.collections.max-size:1000}")
    private long collectionsMaxSize;

    @Value("${stac.cache.items.max-size:10000}")
    private long itemsMaxSize;

    /**
     * Creates a CacheManager with configured caches for collections and items.
     *
     * @return configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("Configuring cache manager with collections TTL={}s, items TTL={}s",
                collectionsTtlSeconds, itemsTtlSeconds);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Register specific cache configurations
        cacheManager.registerCustomCache("collections",
                Caffeine.newBuilder()
                        .expireAfterWrite(collectionsTtlSeconds, TimeUnit.SECONDS)
                        .maximumSize(collectionsMaxSize)
                        .recordStats()
                        .build());

        cacheManager.registerCustomCache("items",
                Caffeine.newBuilder()
                        .expireAfterWrite(itemsTtlSeconds, TimeUnit.SECONDS)
                        .maximumSize(itemsMaxSize)
                        .recordStats()
                        .build());

        return cacheManager;
    }
}
