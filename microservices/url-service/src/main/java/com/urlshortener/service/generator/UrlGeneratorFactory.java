package com.urlshortener.service.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating URL generators based on the specified strategy.
 * Uses Spring's dependency injection to manage generator instances.
 */
@Component
@RequiredArgsConstructor
public class UrlGeneratorFactory {
    private final DistributedSequenceGenerator distributedGenerator;
    private final SimpleCounterGenerator simpleGenerator;

    /**
     * Get the appropriate URL generator based on the strategy.
     * @param strategy The strategy to use for URL generation
     * @return The URL generator implementation
     */
    public UrlGeneratorStrategy getGenerator(GeneratorStrategy strategy) {
        return switch (strategy) {
            case DISTRIBUTED -> distributedGenerator;
            case SIMPLE_COUNTER -> simpleGenerator;
        };
    }

    /**
     * Available URL generation strategies.
     */
    public enum GeneratorStrategy {
        /**
         * Distributed sequence generator based on Snowflake algorithm.
         * Suitable for distributed systems.
         */
        DISTRIBUTED,

        /**
         * Simple counter-based generator.
         * Suitable for single-node deployments.
         */
        SIMPLE_COUNTER
    }
} 