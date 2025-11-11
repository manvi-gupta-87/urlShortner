# Microservices Interview Questions & Answers

**Created:** November 10, 2025
**Purpose:** Quick reference for microservices interview preparation
**Level:** Senior Engineer (10 YOE)

---

## 📚 Table of Contents

1. [Service Discovery & Configuration](#service-discovery--configuration)
2. [Zero-Downtime Migration](#zero-downtime-migration)
3. [Database Strategies](#database-strategies)
4. [Resilience Patterns](#resilience-patterns)
5. [Traffic Management](#traffic-management)
6. [Monitoring & Observability](#monitoring--observability)
7. [Build & Deployment](#build--deployment)
8. [Real Interview Scenarios](#real-interview-scenarios)
9. [Quick Interview Tips](#quick-interview-tips)

---

## Service Discovery & Configuration

### Q1: Is Eureka still used in industry?

**Short Answer:**

Eureka is **declining** in usage. Most companies now use:

| Technology | Market Share | Status |
|------------|-------------|--------|
| **Kubernetes DNS** | 70% | ✅ Standard |
| **Consul** | 15% | ✅ Active |
| **Eureka** | 10% | ⚠️ Maintenance |
| **Service Mesh (Istio)** | 5% | ✅ Enterprise |

**Why declining?**
- ❌ Requires separate server to manage
- ❌ Not cloud-native (designed for VMs)
- ❌ Kubernetes has built-in DNS-based discovery
- ❌ Netflix stopped active development

**Strong Interview Answer:**
> "Eureka was popular in 2015-2018 when Netflix open-sourced it, but modern microservices primarily use Kubernetes native service discovery. K8s DNS resolves service names automatically without needing a separate registry. For my learning project, I used Eureka because it's educational and well-documented, but in production I'd recommend Kubernetes DNS for cloud deployments or Consul for multi-cloud environments."

---

### Q2: What is the difference between Config Server and Eureka?

**Short Answer:**

They solve different problems:

| Aspect | Config Server | Eureka |
|--------|--------------|--------|
| **Purpose** | Store static configuration | Dynamic service discovery |
| **Contains** | DB URLs, API keys, feature flags | Service locations and health |
| **Updates** | Manual (requires restart) | Automatic (real-time) |
| **Registration** | Define URLs in config files | Services self-register |
| **Health checks** | ❌ No | ✅ Yes (heartbeats) |
| **Load balancing** | ❌ Manual | ✅ Automatic |

**Strong Interview Answer:**
> "Config Server stores centralized configuration like database URLs and API keys in a Git repository. It's static - you need to update config and restart services. Eureka is for dynamic service discovery - services automatically register their location and health status at runtime. Modern systems use Kubernetes ConfigMaps for configuration and K8s DNS for service discovery, eliminating the need for both."

---

### Q3: What's the difference between Eureka, Consul, and Kubernetes DNS?

**Detailed Comparison:**

| Feature | Eureka | Consul | Kubernetes DNS |
|---------|--------|--------|----------------|
| **Released** | 2012 | 2014 | 2015+ |
| **Status** | ⚠️ Maintenance mode | ✅ Active development | ✅ Standard |
| **Setup complexity** | Medium (separate server) | Medium (separate server) | Low (built-in) |
| **Multi-datacenter** | ❌ Poor support | ✅ Excellent | ⚠️ Complex (federation) |
| **Health checks** | ✅ Heartbeat only | ✅ HTTP/TCP/Script | ✅ Liveness/Readiness |
| **Service mesh** | ❌ No | ✅ Yes (Connect) | ✅ Yes (Istio/Linkerd) |
| **Key-value store** | ❌ No | ✅ Yes | ✅ Yes (etcd) |
| **Use case** | Legacy Java apps | Multi-cloud | Kubernetes-native |

**Strong Interview Answer:**
> "Eureka is Netflix's legacy solution from 2012, now in maintenance mode since 2018. Consul by HashiCorp is more modern with excellent multi-datacenter support and is popular for multi-cloud deployments. Kubernetes DNS is the current industry standard - it's built-in, requires no separate server, and uses native K8s Service objects. For new projects on Kubernetes, I'd use native DNS. For bare-metal or multi-cloud, Consul is the better choice."

---

## Zero-Downtime Migration

### Q4: How do you migrate a monolith to microservices without downtime?

**Short Answer:**

Use **Strangler Fig Pattern** with gradual rollout:

**Steps:**

1. **Build** microservice alongside monolith (both running)
2. **Deploy** routing layer with feature flags (Redis-backed)
3. **Canary release** starting at 10% traffic
4. **Monitor** error rates and latency in Grafana
5. **Gradually increase** to 25% → 50% → 100% over 4-6 weeks
6. **Database migration** last (replication → dual writes → separate DBs)
7. **Rollback** instant via feature flag toggle

**Architecture Evolution:**

```
Week 1: Monolith (90%) + Auth Service (10%)
Week 4: Monolith (0% - retired) + Auth Service (100%)
Total: Zero downtime
```

**Strong Interview Answer:**
> "I'd use the strangler fig pattern to incrementally migrate functionality. First, deploy the Auth microservice alongside the monolith with feature flags in the API Gateway for traffic routing. Start with a 10% canary deployment, monitoring error rates and latency in Prometheus/Grafana. Gradually increase to 25%, 50%, 75%, 100% over 4-6 weeks based on metrics. Database separation happens last - start with replication, then dual writes via event-driven sync with Kafka, finally cut over to separate databases. This approach provides zero downtime and instant rollback capability via feature flag toggles, similar to Netflix's 7-year migration."

---

### Q5: Can you use feature flags AND canary deployments together?

**Short Answer:**

**YES!** They're complementary and provide defense in depth.

**Different Purposes:**

| Aspect | Feature Flags | Canary Deployments |
|--------|--------------|-------------------|
| **Controls** | WHERE traffic goes (monolith vs microservice) | WHICH VERSION (v1.0 vs v2.0) |
| **Layer** | Application routing logic | Infrastructure/deployment |
| **Granularity** | User-level (specific users) | Traffic-level (% of all traffic) |
| **Rollback speed** | Instant (<1 sec) | 1-2 minutes |
| **Use case** | Migration, A/B testing | Version rollout, safety |

**Two Safety Nets:**

1. **Feature flag rollback:** If entire microservice is broken → toggle to 0% (instant)
2. **Canary rollback:** If just the new version has bugs → scale down canary (1-2 min)

**Strong Interview Answer:**
> "Absolutely, and it's industry best practice for defense in depth. Feature flags control application-layer routing - whether traffic goes to the monolith or microservice, with instant rollback and user-level granularity. Canary deployments operate at the infrastructure layer - gradually rolling out new versions of the same service with automated rollback based on metrics. Together they give two safety nets: if the entire microservice fails, toggle the feature flag. If just the new version has issues, scale down the canary. Netflix uses this exact approach - Zuul with feature flags plus Red/Black deployments to deploy hundreds of times daily."

---

### Q6: How do feature flags control 10% / 90% traffic split?

**Short Answer:**

**Algorithm:** Consistent hash-based bucketing

```java
public boolean isEnabled(String flagName, String userId) {
    // Get target percentage from Redis (e.g., 10)
    int targetPercentage = redis.get("feature:" + flagName + ":percentage");

    // Hash userId to get bucket 0-99
    int userBucket = Math.abs(userId.hashCode() % 100);

    // If bucket < percentage, enable
    return userBucket < targetPercentage;
}
```

**Example:**
- Flag = 10%
- user_grace → hash = 8 → 8 < 10 → ✅ Microservice
- user_alice → hash = 90 → 90 < 10 → ❌ Monolith

**Key Properties:**

1. **Deterministic:** Same user always gets same route (not random)
2. **Gradual rollout:** Increase percentage over time (10% → 50% → 100%)
3. **No user lists:** Hash function automatically distributes evenly
4. **Instant changes:** Update percentage in Redis, no restart needed

**Strong Interview Answer:**
> "Feature flags use consistent hashing to bucket users. I hash the userId modulo 100 to get a bucket from 0-99, then compare it to the target percentage stored in Redis. For example, at 10%, users in buckets 0-9 go to the microservice, 10-99 go to the monolith. This is deterministic - the same user always gets the same route, providing consistent UX. When I increase the percentage to 50%, buckets 0-49 now route to the microservice. The hash function distributes users evenly without maintaining explicit user lists, and I can change the percentage in Redis for instant rollout without restarting services."

---

## Database Strategies

### Q7: Do microservices need separate databases?

**Short Answer:**

**True microservices:** 1 service = 1 database instance

**Reality:** Many companies use pragmatic middle-ground approaches.

**Database Separation Levels:**

| Level | Setup | Your Company | True Microservices | Rating |
|-------|-------|--------------|-------------------|--------|
| **Instance** | 1 shared PostgreSQL | ✅ Common | ❌ Should be N separate | 5/10 |
| **Schema** | 1 shared schema | ✅ Common | ❌ Should be N schemas | 5/10 |
| **Tables** | Each service owns tables | ✅ Good | ✅ Good | 8/10 |
| **Write access** | Only to owned tables | ✅ Correct | ✅ Correct | 10/10 |
| **Read access** | Can read any table | ❌ Anti-pattern | ❌ Should use APIs | 3/10 |

**Shared DB Issues:**

❌ **Single point of failure:** 1 DB down = all services down
❌ **Can't scale independently:** Can't scale just the URL service DB
❌ **Cross-service reads = tight coupling:** Schema changes break other services
❌ **Can't use different DB types:** All stuck with same technology

**Strong Interview Answer:**
> "True microservices should have separate database instances to ensure complete isolation and independent scaling. However, I've worked with a pragmatic middle-ground approach where 51 services shared one database with separate tables. While this was better than a monolithic shared database, it had significant limitations: single point of failure, couldn't scale databases independently, and cross-service reads created tight coupling that defeated the purpose of microservices. The proper approach follows Amazon's mandate: 'No direct database access between teams' - all communication via APIs. This enables independent deployments, technology choices per service, and true scaling independence."

---

### Q8: How do you handle database migration in microservices?

**Short Answer:**

**Phased approach (never "big bang"):**

| Week | Strategy | Monolith | Microservice | Notes |
|------|----------|----------|--------------|-------|
| 1-2 | **Shared DB** | Write & Read | Write & Read | Both use same DB |
| 3-4 | **Replication** | Write (master) | Read (replica) | Test read-only access |
| 5-6 | **Dual Write** | Write to both | Write to both | Preparation for cutover |
| 7-8 | **Event-Driven** | Publishes events | Consumes events | Async sync via Kafka |
| 9+ | **Separate DBs** | (retired) | Independent | Monolith gone |

**Strong Interview Answer:**
> "Database migration is the most complex part of microservices migration. I'd use a phased approach over 8-10 weeks. Start with both services sharing the database for lowest risk. Then set up replication so the microservice reads from a replica, validating it doesn't need write access yet. Next, implement dual writes - the microservice writes to both databases, or better yet, the monolith publishes events to Kafka that the microservice consumes for async synchronization. Finally, after all traffic is migrated and stable for several weeks, cut over to fully separate databases. Database separation happens last because it's the highest-risk, most irreversible change. This approach gives you multiple rollback points along the way."

---

## Resilience Patterns

### Q9: Is Hystrix still used in industry?

**Short Answer:**

**NO.** Hystrix was **deprecated by Netflix in November 2018** and is in maintenance mode only.

**Timeline:**

| Year | Status |
|------|--------|
| 2012 | Netflix releases Hystrix |
| 2016 | Peak popularity |
| **Nov 2018** | **Netflix announces deprecation** |
| 2020+ | Maintenance mode, security patches only |
| 2023+ | Legacy, incompatible with Spring Boot 3 |

**Replacement:** **Resilience4j** (modern, lightweight, active development)

**Comparison:**

| Feature | Hystrix (Deprecated) | Resilience4j (Modern) |
|---------|---------------------|----------------------|
| **Status** | ❌ Deprecated (2018) | ✅ Active development |
| **Architecture** | Thread pool isolation (heavy) | Decorator pattern (lightweight) |
| **Memory usage** | High (separate thread pools) | Low (no extra threads) |
| **Spring Boot 3** | ❌ Incompatible | ✅ Fully supported |
| **Metrics** | Hystrix Dashboard | Micrometer (Prometheus) |
| **Circuit breaker** | ✅ Yes | ✅ Yes |
| **Retry** | Limited | ✅ Advanced (exponential backoff) |
| **Rate limiting** | ❌ No | ✅ Yes (built-in) |
| **Bulkhead** | Thread pools | ✅ Semaphore-based |

**Strong Interview Answer:**
> "My current company still uses Hystrix, but I'm aware Netflix deprecated it in November 2018. For personal projects, I use Resilience4j - the modern replacement. Resilience4j is more lightweight, using decorator patterns instead of Hystrix's heavyweight thread pool isolation, resulting in significantly better performance and lower memory usage. It also integrates natively with Micrometer for Prometheus metrics, which is the industry standard for observability. Additionally, Resilience4j is compatible with Spring Boot 3, while Hystrix is not. For companies still on Hystrix, I'd recommend planning a migration to Resilience4j to avoid future compatibility and security issues."

---

## Traffic Management

### Q10: What metrics do you monitor during migration?

**Short Answer:**

**Four Critical Categories:**

**1. Traffic Metrics**
- Request rate to verify traffic split matches target

**2. Error Metrics**
- Compare 4xx/5xx rates between stable and canary
- Alert if canary error rate exceeds stable by 50%

**3. Performance Metrics**
- P50, P95, P99 latency comparison
- Alert on 20%+ latency regression

**4. Business Metrics**
- User registrations, order completion rates, revenue

**Strong Interview Answer:**
> "I'd track four categories: Traffic metrics to verify the split matches the target - if I set 10% canary, I need to confirm 10% of requests actually hit it. Error metrics comparing 4xx/5xx rates between canary and stable, with alerts if canary errors exceed stable by 50%. Performance metrics tracking P50, P95, and P99 latency, alerting on 20% regression. And critically, business metrics like user signups and order completion rates to catch issues that don't appear in technical metrics. I'd use Prometheus for collection, Grafana for dashboards, and PagerDuty for alerting with automated rollback triggers if error rate crosses thresholds."

---

### Q11: What if the microservice fails in production?

**Short Answer:**

**Immediate Response (< 30 seconds):**

```bash
# Toggle feature flag to 0% → instant rollback
curl -X PUT "http://gateway/admin/feature-flags/use-auth-microservice/percentage?percentage=0"
```

**Within 5 Minutes:**
- Check **Grafana** dashboards for metrics
- Review **Kibana** logs for error messages
- Analyze **Zipkin** distributed traces

**Within 30 Minutes:**
- If hotfix available: Deploy to 5% canary first
- If not: Keep flag at 0% and investigate offline

**Strong Interview Answer:**
> "This is why feature flags are critical for production safety. My immediate response would be to toggle the feature flag to 0% to route all traffic back to the monolith - no deployment needed, instant rollback in under 1 second. Then I'd investigate using our observability stack: Grafana for metrics to identify when errors started, Kibana for logs to see error messages, and Zipkin for distributed traces to understand which service in the chain failed. Within 5 minutes I'd have the blast radius contained and root cause identified. If a hotfix is available, I'd deploy it to a 5% canary first to validate. This is exactly why we don't jump to 100% immediately - canary deployments with feature flags provide multiple rollback mechanisms and limit blast radius."

---

## Monitoring & Observability

### Q12: What's the difference between monitoring and observability?

**Short Answer:**

**Monitoring:** Known unknowns - tracking predefined metrics
**Observability:** Unknown unknowns - investigating unexpected issues

**Three Pillars of Observability:**

| Pillar | Purpose | Tools | Example |
|--------|---------|-------|---------|
| **Metrics** | Numbers over time | Prometheus, Grafana | CPU 80%, latency P99 |
| **Logs** | Event records | ELK, Splunk | "User 123 failed login" |
| **Traces** | Request flow | Zipkin, Jaeger | Request through 5 services |

**Strong Interview Answer:**
> "Monitoring is for known unknowns - I track predefined metrics like CPU, memory, error rates, and alert when they exceed thresholds. Observability is for unknown unknowns - when a complex, unexpected issue occurs, I need to investigate and understand the system's internal state. The three pillars are: metrics for aggregate trends, logs for detailed event records, and distributed traces for understanding request flow across services. In my URL shortener project, I use Prometheus for metrics, ELK for logs, and Zipkin for traces, giving me full observability to debug issues I couldn't have predicted."

---

## Build & Deployment

### Q13: What's the difference between executable JAR and library JAR?

**Short Answer:**

**Library JAR:** Contains reusable code for dependencies
**Executable JAR:** Standalone application that can be run directly

**Comparison:**

| Aspect | Library JAR | Executable JAR (Spring Boot) |
|--------|-------------|------------------------------|
| **Purpose** | Provide reusable code | Run standalone application |
| **Structure** | Standard JAR with classes | Fat JAR with nested dependencies |
| **Main class** | ❌ None | ✅ Required (e.g., AuthServiceApplication) |
| **Usage** | Added as dependency in POM | Run with `java -jar app.jar` |
| **Spring Boot plugin** | Skip repackaging (`<skip>true</skip>`) | Repackage as fat JAR |
| **Example** | shared-library-1.0.0.jar | auth-service-1.0.0.jar |
| **Size** | Small (~50KB) | Large (~50MB with all deps) |

**Library JAR Structure:**
```
shared-library-1.0.0.jar
├── com/urlshortener/dto/ErrorResponse.class
├── com/urlshortener/exception/...
└── META-INF/MANIFEST.MF (no Main-Class)
```

**Executable JAR Structure:**
```
auth-service-1.0.0.jar
├── BOOT-INF/
│   ├── classes/           # Your compiled code
│   └── lib/               # All dependencies (including shared-library)
├── org/springframework/boot/loader/  # Spring Boot loader
└── META-INF/MANIFEST.MF
    Main-Class: org.springframework.boot.loader.JarLauncher
    Start-Class: com.urlshortener.AuthServiceApplication
```

**Maven Configuration:**

```xml
<!-- Library JAR (shared-library) -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <skip>true</skip>  <!-- Skip repackaging -->
    </configuration>
</plugin>

<!-- Executable JAR (auth-service) -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <!-- Default: repackage into executable JAR -->
</plugin>
```

**Common Build Error:**

When you forget `<skip>true</skip>` on a library:
```bash
[ERROR] Unable to find main class
```

**Why?** Spring Boot plugin tries to create executable JAR but library has no main class.

**Strong Interview Answer:**
> "Library JARs contain reusable code for other services to depend on - they're lightweight, contain only compiled classes, and can't be executed directly. Executable JARs, created by Spring Boot's repackaging plugin, are fat JARs that bundle your application code plus all dependencies in BOOT-INF/lib, making them standalone and runnable with `java -jar`. In our microservices project, the shared-library module is a library JAR that we add as a dependency to auth-service and url-service. Those service modules are executable JARs that can be deployed and run independently. The key is configuring the Spring Boot Maven plugin with `<skip>true</skip>` for library modules to prevent it from trying to create an executable JAR without a main class."

---

## Real Interview Scenarios

### Scenario 1: "Tell me about a microservices migration you've worked on"

**Weak Answer:**
> "I built microservices using Spring Boot and deployed them."

**Strong Answer:**
> "I refactored a monolithic URL shortener into 3 microservices - Auth, URL, and Analytics - using the strangler fig pattern. Started by building the Auth service alongside the monolith, both sharing the database initially. Implemented feature flags in the API Gateway backed by Redis for gradual traffic routing. Rolled out from 10% to 100% over 4 weeks, monitoring error rates and latency in Prometheus. Used Resilience4j circuit breakers to prevent cascade failures. The database migration happened last - started with replication, then dual writes via Kafka events, finally separate PostgreSQL instances. This provided zero downtime and instant rollback capability via feature flag toggles."

---

### Scenario 2: "How do you handle failures in distributed systems?"

**Weak Answer:**
> "I use try-catch blocks and return error messages."

**Strong Answer:**
> "I implement multiple layers of resilience. At the application level, I use Resilience4j circuit breakers to prevent cascade failures - if Auth service is down, I open the circuit and return cached data or fail fast rather than overwhelming it with retries. I implement retry with exponential backoff for transient failures, and rate limiting with Bucket4j to prevent abuse. At the infrastructure level, I use health checks with Kubernetes liveness and readiness probes to automatically restart unhealthy pods. For gradual deployments, I use canary releases with automated rollback based on error rate metrics in Prometheus. Feature flags provide instant rollback for critical failures. This defense-in-depth approach ensures the system degrades gracefully rather than complete failure."

---

### Scenario 3: "When would you NOT use microservices?"

**Strong Answer:**
> "Microservices add significant complexity - distributed tracing, network latency, data consistency challenges. I wouldn't use them for:
>
> 1. **Small teams** (<10 engineers) - coordination overhead exceeds benefits
> 2. **Unclear domain boundaries** - premature splitting leads to tight coupling
> 3. **Low traffic applications** - monolith is simpler and adequate
> 4. **Tight latency requirements** - network calls add 5-20ms overhead
> 5. **Strong consistency needs** - distributed transactions are complex
>
> Martin Fowler's advice: 'Start with a monolith, extract microservices when you feel pain.' The pain signals are: deployments block each other, teams stepping on each other's code, different scaling needs per module. Microservices are a tool for organizational scaling, not a goal in themselves."

---

## Quick Interview Tips

### Do's ✅

1. **Always mention modern alternatives** when asked about legacy tech
   - "Eureka → Kubernetes DNS"
   - "Hystrix → Resilience4j"

2. **Give timeframes** to show you keep up with industry
   - "Hystrix deprecated in November 2018"
   - "Netflix migrated over 7 years using strangler fig"

3. **Reference big tech** to demonstrate industry awareness
   - Netflix, Uber, Amazon, Airbnb examples

4. **Acknowledge pragmatic approaches** while knowing the ideal
   - "Shared DB is acceptable for transitional phase, but true microservices need separate instances"

5. **Provide metrics and numbers**
   - "10x performance improvement"
   - "50% error rate threshold for rollback"
   - "P99 latency < 500ms"

6. **Explain trade-offs**, not just solutions
   - "Eureka is simpler to learn but K8s DNS is production-standard"

---

### Don'ts ❌

1. **Don't claim something is "best" without context**
   - ❌ "Microservices are best"
   - ✅ "Microservices are best when you need independent scaling and teams"

2. **Don't memorize buzzwords without understanding**
   - Know WHY strangler fig works, not just that it exists

3. **Don't ignore your company's tech stack**
   - Use it as learning experience: "We use X, but I know Y is modern alternative"

4. **Don't oversimplify production complexity**
   - Acknowledge challenges: "Zero-downtime migration takes months, not days"

5. **Don't fake knowledge**
   - "I haven't used Istio in production, but I understand it provides service mesh capabilities"

---

## Interview Question Categories

### Architecture & Design (35%)
- Monolith to microservices migration
- Service boundaries
- Database strategies
- Communication patterns (sync vs async)

### Resilience & Reliability (25%)
- Circuit breakers (Resilience4j)
- Retry strategies
- Timeout handling
- Graceful degradation

### Deployment & Operations (20%)
- Feature flags
- Canary deployments
- Blue-green deployments
- Rollback strategies

### Build & Deployment (10%)
- Executable vs library JARs
- Maven multi-module projects
- Spring Boot plugin configuration
- Artifact packaging

### Observability & Monitoring (10%)
- Metrics (Prometheus)
- Logging (ELK)
- Distributed tracing (Zipkin)
- Alerting (PagerDuty)

---

## Additional Resources

**Books:**
- *Building Microservices* by Sam Newman
- *Monolith to Microservices* by Sam Newman
- *Release It!* by Michael Nygard

**Articles:**
- Martin Fowler: "Strangler Fig Application"
- Martin Fowler: "CircuitBreaker Pattern"
- Netflix Tech Blog: "Microservices Journey"

**Practice:**
- Draw architecture diagrams during interviews
- Use real project examples from your experience
- Prepare questions about their microservices challenges

---

**Document Version:** 1.1
**Last Updated:** November 11, 2025
**Next Review:** Before each interview

---

**Pro Tip:** Read this document 2-3 times, then practice explaining answers out loud. Focus on WHY, not just WHAT.
