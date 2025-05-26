package com.urlshortener.service.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SimpleCounterGeneratorTest {

    private SimpleCounterGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SimpleCounterGenerator();
    }

    @Test
    void whenGeneratingUrls_thenTheyAreUnique() {
        Set<String> urls = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String url = generator.generateShortUrl();
            assertTrue(urls.add(url), "Generated URL should be unique: " + url);
        }
    }

    @Test
    void whenGeneratingUrls_thenTheyAreSequential() {
        String url1 = generator.generateShortUrl();
        String url2 = generator.generateShortUrl();
        String url3 = generator.generateShortUrl();

        assertNotEquals(url1, url2, "URLs should be different");
        assertNotEquals(url2, url3, "URLs should be different");
        assertNotEquals(url1, url3, "URLs should be different");
    }

    @Test
    void whenMultipleThreadsGenerateUrls_thenNoCollisions() throws InterruptedException {
        int threadCount = 10;
        int urlsPerThread = 100;
        Set<String> allUrls = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < urlsPerThread; j++) {
                        String url = generator.generateShortUrl();
                        synchronized (allUrls) {
                            assertTrue(allUrls.add(url), "Generated URL should be unique: " + url);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(threadCount * urlsPerThread, allUrls.size(), 
            "Total number of unique URLs should match the number of generated URLs");
    }

    @Test
    void whenBase62Encoding_thenOutputIsValid() {
        String url = generator.generateShortUrl();
        assertNotNull(url);
        assertTrue(url.matches("^[0-9A-Za-z]+$"), "URL should only contain alphanumeric characters");
        assertTrue(url.length() > 0, "URL should not be empty");
    }
} 