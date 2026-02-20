package com.guidewire.integration.gateway.config;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides an in-memory idempotent repository for deduplicating Kafka messages.
 * For production, this should be replaced with a persistent store (e.g., Redis or JDBC).
 *
 * Issue #50 - Kafka Consumer improvements
 */
@Configuration
public class IdempotentRepositoryConfig {

    @Bean("simpleIdempotentRepo")
    public IdempotentRepository simpleIdempotentRepo() {
        // In-memory repo with a max of 1000 entries (LRU eviction). Sufficient for POC.
        return MemoryIdempotentRepository.memoryIdempotentRepository(1000);
    }
}
