package org.stac.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for asynchronous task execution.
 * <p>
 * Uses Java 21 virtual threads for efficient, lightweight concurrency.
 */
@Slf4j
@Configuration
public class AsyncConfiguration {

    /**
     * Creates an Executor using virtual threads.
     * <p>
     * Virtual threads (Project Loom) provide lightweight concurrency,
     * allowing efficient handling of many concurrent tasks without
     * the overhead of platform threads.
     *
     * @return Executor using virtual threads
     */
    @Bean
    public Executor taskExecutor() {
        log.info("Configuring virtual thread executor for async tasks");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
