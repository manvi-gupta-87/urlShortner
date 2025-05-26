package com.urlshortener.service.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlGeneratorFactoryTest {

    @Mock
    private DistributedSequenceGenerator distributedGenerator;

    @Mock
    private SimpleCounterGenerator simpleGenerator;

    private UrlGeneratorFactory factory;

    @BeforeEach
    void setUp() {
        factory = new UrlGeneratorFactory(distributedGenerator, simpleGenerator);
    }

    @Test
    void whenGettingDistributedGenerator_thenReturnsCorrectInstance() {
        UrlGeneratorStrategy generator = factory.getGenerator(UrlGeneratorFactory.GeneratorStrategy.DISTRIBUTED);
        assertSame(distributedGenerator, generator);
    }

    @Test
    void whenGettingSimpleCounterGenerator_thenReturnsCorrectInstance() {
        UrlGeneratorStrategy generator = factory.getGenerator(UrlGeneratorFactory.GeneratorStrategy.SIMPLE_COUNTER);
        assertSame(simpleGenerator, generator);
    }

    @Test
    void whenGettingGenerator_thenGeneratesUniqueUrls() {
        // Test distributed generator
        when(distributedGenerator.generateShortUrl())
            .thenReturn("url1")
            .thenReturn("url2");

        UrlGeneratorStrategy distributed = factory.getGenerator(UrlGeneratorFactory.GeneratorStrategy.DISTRIBUTED);
        assertEquals("url1", distributed.generateShortUrl());
        assertEquals("url2", distributed.generateShortUrl());

        // Test simple counter generator
        when(simpleGenerator.generateShortUrl())
            .thenReturn("url3")
            .thenReturn("url4");

        UrlGeneratorStrategy simple = factory.getGenerator(UrlGeneratorFactory.GeneratorStrategy.SIMPLE_COUNTER);
        assertEquals("url3", simple.generateShortUrl());
        assertEquals("url4", simple.generateShortUrl());

        verify(distributedGenerator, times(2)).generateShortUrl();
        verify(simpleGenerator, times(2)).generateShortUrl();
    }
} 