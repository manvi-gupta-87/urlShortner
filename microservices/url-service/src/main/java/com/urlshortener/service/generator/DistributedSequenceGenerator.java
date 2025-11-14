package com.urlshortener.service.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Problem with SimpleCounterGenerator: Why Do We Need a Distributed URL Shortener?
 * Imagine you're running a URL shortener service like bit.ly:  // 100,000 requests per minute
 * You start with one server handling 1,000 requests per minute
 * Your service becomes popular
 * Now you get 100,000 requests per minute
 * One server can't handle this load
 * Solution: Multiple Servers
 *  Let's say we set up 3 servers:
 *  Server 1: Located in USA
 *  Server 2: Located in Europe
 *  Server 3: Located in Asia
 * Scenario (Without Distributed System):
    Time: 2:00:00.123 PM
- User A (from USA) requests to shorten: google.com
- User B (from Europe) requests to shorten: facebook.com
- User C (from Asia) requests to shorten: twitter.com

Problem: If all servers just use a simple counter:
Server 1 might generate: abc123
Server 2 might generate: abc123
Server 3 might generate: abc123

Result: COLLISION! Three different long URLs map to same short URL!
 
 * Using Snowflake Algorithm
 * Example 1: Request at 2:00:00.123 PM on Server 1 (USA)
- Timestamp: 2:00:00.123 PM
- Node ID: 1
- Sequence: 0
Generated ID might be: 1679580000123-1-0
Base62 encoded to: "Kp7B2x"

Example 2: Request at SAME TIME on Server 2 (Europe)
- Timestamp: 2:00:00.123 PM
- Node ID: 2
- Sequence: 0
Generated ID might be: 1679580000123-2-0
Base62 encoded to: "Kp7C4z"

Example 3: Request at SAME TIME on Server 3 (Asia)
- Timestamp: 2:00:00.123 PM
- Node ID: 3
- Sequence: 0
Generated ID might be: 1679580000123-3-0
Base62 encoded to: "Kp7D6y"

User A (USA) → Load Balancer → Server 1
- Time: 2:00:00.123 PM
- Node: 1
- Sequence: 0
Result: short.url/Kp7B2x → google.com

User B (USA) → Load Balancer → Server 1
- Time: 2:00:00.123 PM (same millisecond!)
- Node: 1
- Sequence: 1
Result: short.url/Kp7B2y → facebook.com

User C (Europe) → Load Balancer → Server 2
- Time: 2:00:00.123 PM
- Node: 2
- Sequence: 0
Result: short.url/Kp7C4z → twitter.com
 * 
 * Distributed sequence generator implementation based on Twitter's Snowflake algorithm.
 * Thread-safe and suitable for distributed systems. AtomicInteger and synchronized are used to ensure thread safety.
 * Pros:
 * 1. Works across multiple servers 
 * 2. Uses a base62 encoding scheme to generate the short URL
 * Cons:
 * More complex
 * Requires clock synchronization across all nodes
 * Slightly longer Urls
 * 
 * How it handles concurrent requests across multiple servers:
 * 1. Each server has a unique nodeId (0-1023)
 * 2. Each server maintains its own sequence counter (0-4095)
 * 3. For requests in the same millisecond:
 *    - Server 1 (nodeId: 1) generates: timestamp-1-0, timestamp-1-1, ...
 *    - Server 2 (nodeId: 2) generates: timestamp-2-0, timestamp-2-1, ...
 *    - Each server can handle 4096 requests/ms before waiting for next millisecond
 * 4. The generated ID combines:
 *    - 42 bits for timestamp
 *    - 10 bits for nodeId (supports 1024 servers)
 *    - 12 bits for sequence (4096 requests/ms/server)
 * 
 * Example with 3 servers at same millisecond:
 * Server 1 (USA):  1679580000123-1-0 → "Kp7B2x"
 * Server 2 (Europe): 1679580000123-2-0 → "Kp7C4z"
 * Server 3 (Asia):  1679580000123-3-0 → "Kp7D6y"
 */

@Component
public class DistributedSequenceGenerator implements UrlGeneratorStrategy {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = CHARACTERS.length();
    
    private final int nodeId; // 10 bits = 1024 nodes // supports 1024 different nodes
    private final AtomicInteger sequence; // 12 bits = 4096 sequences // can handle 4096 URLS per millisecond per node
    private long lastTimestamp;

    public DistributedSequenceGenerator(@Value("${url.generator.node-id:1}") int nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("Node ID must be between 0 and 1023");
        }
        this.nodeId = nodeId;
        this.sequence = new AtomicInteger(0);
        this.lastTimestamp = 0L;
    }

    @Override
    public synchronized String generateShortUrl() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp == lastTimestamp) {
            sequence.incrementAndGet();
            if (sequence.get() >= 4096) {
                // Wait until next millisecond
                while (timestamp == lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
                sequence.set(0);
            }
        } else {
            sequence.set(0);
        }
        
        lastTimestamp = timestamp;
        
        // Combine bits: timestamp (42) + nodeId (10) + sequence (12) = 64 bits
        long id = ((timestamp & 0x1FFFFFFFFFFFL) << 22) | 
                 ((nodeId & 0x3FF) << 12) | 
                 (sequence.get() & 0xFFF);
                 
        return base62Encode(id);
    }

    private String base62Encode(long number) {
        if (number == 0) {
            return String.valueOf(CHARACTERS.charAt(0));
        }

        StringBuilder shortUrl = new StringBuilder();
        while (number > 0) {
            shortUrl.insert(0, CHARACTERS.charAt((int)(number % (long)BASE)));
            number = number / BASE;
        }
        return shortUrl.toString();
    }
} 