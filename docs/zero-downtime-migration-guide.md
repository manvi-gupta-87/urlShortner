# Zero-Downtime Migration & Backward Compatibility Guide

**Created:** November 10, 2025
**Purpose:** Production-grade migration strategies for monolith to microservices
**Audience:** Senior engineers preparing for system design discussions

---

## 📚 Table of Contents

1. [Overview](#overview)
2. [Current Project vs Production Reality](#current-project-vs-production-reality)
3. [Strangler Fig Pattern](#strangler-fig-pattern)
4. [Feature Flags Implementation](#feature-flags-implementation)
5. [Canary Deployments](#canary-deployments)
6. [Database Migration Strategies](#database-migration-strategies)
7. [Traffic Routing Strategies](#traffic-routing-strategies)
8. [Rollback Mechanisms](#rollback-mechanisms)
9. [Monitoring & Metrics](#monitoring--metrics)
10. [Industry Examples](#industry-examples)
11. [Interview Talking Points](#interview-talking-points)
12. [Implementation Timeline](#implementation-timeline)

---

## Overview

### What is Zero-Downtime Migration?

**Zero-downtime migration** means refactoring a monolith to microservices while:
- ✅ Users experience **no service interruption**
- ✅ Both systems run **simultaneously** during transition
- ✅ **Instant rollback** capability if issues arise
- ✅ **Gradual traffic shift** from old to new system
- ✅ **No data loss** during migration

## Current Project vs Production Reality

### What Your Project Provides

**✅ Backward Compatibility:**
```
API Gateway maintains same endpoints:
- /api/v1/auth/...
- /api/v1/urls/...
- Same port :8080
- Frontend requires zero code changes
```
 
**✅ How True Zero-Downtime achieves in production: (Strangler Fig Pattern)**
```
Week 1: Monolith handles 100% traffic
Week 2: Monolith 90%, Auth Service 10% (canary)
Week 3: Monolith 50%, Auth Service 50%
Week 4: Monolith 0%, Auth Service 100%

Downtime: 0 minutes
Risk: Low (gradual rollout)
Rollback: Feature flag toggle (instant)
```

## Strangler Fig Pattern

### Concept

Named after strangler fig trees that grow around host trees and eventually replace them.

**Strategy:** Build new microservices around the monolith, gradually routing traffic until monolith can be retired.

### Architecture Evolution

**Phase 1: Monolith Only**
```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
┌──────▼───────┐
│  Monolith    │
│  (Port 8080) │
└──────┬───────┘
       │
┌──────▼───────┐
│   Database   │
└──────────────┘
```

**Phase 2: Side-by-Side (Strangler Begins)**
```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
┌──────▼────────────────────────┐
│    Routing Layer (NGINX)      │
│  (Feature flags + % routing)  │
└──────┬────────────────────────┘
       │
   ┌───┴────┐
   │        │
┌──▼──────┐ │  ┌──────────────┐
│Monolith │ │  │ Microservices│
│(90%)    │ │  │ (10% canary) │
└──┬──────┘ │  └──┬───────────┘
   │        │     │
   │    ┌───┴─────┴───┐
   │    │             │
┌──▼────▼──┐   ┌──────▼───────┐
│ Main DB  │   │  Service DBs │
│ (shared) │◄──┤ (replicated) │
└──────────┘   └──────────────┘
```

**Phase 3: Microservices Dominant**
```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
┌──────▼────────────────────────┐
│    API Gateway                │
│  (Routes to microservices)    │
└──────┬────────────────────────┘
       │
   ┌───┴────┐
   │        │
┌──▼──────┐ │  ┌──────────────┐
│Monolith │ │  │ Microservices│
│(10%)    │ │  │ (90% traffic)│
└──┬──────┘ │  └──┬───────────┘
   │        │     │
   └────────┴─────┘
        (being retired)
```

**Phase 4: Monolith Retired**
```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
┌──────▼────────────────────────┐
│    API Gateway                │
└──────┬────────────────────────┘
       │
   ┌───┴─────┬──────────┐
   │         │          │
┌──▼─────┐ ┌─▼──────┐ ┌▼────────┐
│ Auth   │ │  URL   │ │Analytics│
│Service │ │Service │ │ Service │
└──┬─────┘ └─┬──────┘ └┬────────┘
   │         │         │
┌──▼─────┐ ┌─▼──────┐ ┌▼────────┐
│Auth DB │ │ URL DB │ │Stats DB │
└────────┘ └────────┘ └─────────┘
```

### Implementation Steps

**Step 1: Identify Strangling Points**

Choose services to extract based on:
- **Independence:** Low coupling with other features
- **Value:** High traffic or business criticality
- **Risk:** Low risk if something breaks

For URL Shortener:
1. ✅ **Auth Service** (clear boundary, JWT tokens)
2. ✅ **URL Service** (core business logic)
3. ✅ **Analytics Service** (read-only, low risk)

**Step 2: Build Microservice Alongside Monolith**

```bash
# Monolith still runs on :8080
# New Auth Service runs on :8081

# Both connected to same database initially
```

**Step 3: Add Routing Layer**

Use NGINX, Envoy, or Spring Cloud Gateway:

```yaml
# nginx.conf
upstream monolith {
    server localhost:8080;
}

upstream auth_service {
    server localhost:8081;
}

# Split traffic based on feature flag
location /api/v1/auth {
    # Check Redis for feature flag
    set $backend "monolith";

    # If user in beta group, use new service
    if ($http_x_user_id in beta_users) {
        set $backend "auth_service";
    }

    proxy_pass http://$backend;
}
```

**Step 4: Gradual Rollout**

```
Week 1: 5% of /auth traffic → Auth Service
Week 2: 10% (if metrics look good)
Week 3: 25%
Week 4: 50%
Week 5: 75%
Week 6: 100% → Retire monolith auth code
```

---

## Feature Flags Implementation

### What Are Feature Flags?

Runtime toggles that control which code path executes **without redeploying**.

### Architecture

```
┌──────────────┐
│   Request    │
└──────┬───────┘
       │
┌──────▼────────────────────────┐
│    API Gateway                │
│                               │
│  ┌─────────────────────────┐ │
│  │ Feature Flag Service    │ │
│  │ (Redis/LaunchDarkly)    │ │
│  └─────────────────────────┘ │
│           │                   │
│      Check flag               │
│      "use-auth-microservice"  │
│           │                   │
│     ┌─────┴──────┐           │
│     │ Yes  │  No │           │
└─────┼──────┼─────┼───────────┘
      │      │     │
  ┌───▼──┐   │  ┌──▼───────┐
  │ Auth │   │  │ Monolith │
  │Service   │  │   :8080  │
  └──────┘   │  └──────────┘
             │
        (fallback)
```

### Implementation with Spring Boot

**1. Add Dependencies:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.openfeature</groupId>
    <artifactId>sdk</artifactId>
    <version>1.7.0</version>
</dependency>
<dependency>
    <groupId>dev.openfeature.contrib.providers</groupId>
    <artifactId>redis</artifactId>
    <version>0.1.0</version>
</dependency>
```

**2. Create Feature Flag Service:**

```java
// config/FeatureFlagService.java
@Service
public class FeatureFlagService {

    private final RedisTemplate<String, String> redisTemplate;

    public FeatureFlagService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isEnabled(String flagName, String userId) {
        // Check user-specific override
        String userFlag = redisTemplate.opsForValue()
            .get("feature:" + flagName + ":user:" + userId);
        if (userFlag != null) {
            return Boolean.parseBoolean(userFlag);
        }

        // Check global percentage rollout
        String percentage = redisTemplate.opsForValue()
            .get("feature:" + flagName + ":percentage");
        if (percentage != null) {
            int rolloutPercent = Integer.parseInt(percentage);
            // Hash userId to get deterministic bucket 0-99
            int userBucket = Math.abs(userId.hashCode() % 100);
            return userBucket < rolloutPercent;
        }

        // Default: disabled
        return false;
    }

    public void setGlobalPercentage(String flagName, int percentage) {
        redisTemplate.opsForValue()
            .set("feature:" + flagName + ":percentage", String.valueOf(percentage));
    }

    public void setUserOverride(String flagName, String userId, boolean enabled) {
        redisTemplate.opsForValue()
            .set("feature:" + flagName + ":user:" + userId, String.valueOf(enabled));
    }
}
```

**3. Create Routing Filter:**

```java
// gateway/filters/StranglerFigRoutingFilter.java
@Component
public class StranglerFigRoutingFilter implements GlobalFilter, Ordered {

    private final FeatureFlagService featureFlagService;
    private final RestTemplate restTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String userId = extractUserId(exchange);

        // Check if this is an auth endpoint
        if (path.startsWith("/api/v1/auth")) {
            boolean useMicroservice = featureFlagService
                .isEnabled("use-auth-microservice", userId);

            if (useMicroservice) {
                // Route to Auth Service
                return routeToService(exchange, "http://auth-service:8081");
            } else {
                // Route to Monolith
                return routeToService(exchange, "http://monolith:8080");
            }
        }

        return chain.filter(exchange);
    }

    private String extractUserId(ServerWebExchange exchange) {
        // Extract from JWT token or header
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return JwtUtil.extractUserId(token.substring(7));
        }
        return "anonymous";
    }

    @Override
    public int getOrder() {
        return -100; // Run before other filters
    }
}
```

**4. Admin API to Control Flags:**

```java
// controller/FeatureFlagController.java
@RestController
@RequestMapping("/admin/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @PutMapping("/{flagName}/percentage")
    public ResponseEntity<Void> setRolloutPercentage(
            @PathVariable String flagName,
            @RequestParam int percentage) {

        if (percentage < 0 || percentage > 100) {
            return ResponseEntity.badRequest().build();
        }

        featureFlagService.setGlobalPercentage(flagName, percentage);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{flagName}/users/{userId}")
    public ResponseEntity<Void> setUserOverride(
            @PathVariable String flagName,
            @PathVariable String userId,
            @RequestParam boolean enabled) {

        featureFlagService.setUserOverride(flagName, userId, enabled);
        return ResponseEntity.ok().build();
    }
}
```

**5. Usage Example:**

```bash
# Week 1: Enable for internal testing only
curl -X PUT "http://localhost:8080/admin/feature-flags/use-auth-microservice/users/admin@company.com?enabled=true"

# Week 2: Rollout to 10% of users
curl -X PUT "http://localhost:8080/admin/feature-flags/use-auth-microservice/percentage?percentage=10"

# Week 3: Increase to 25%
curl -X PUT "http://localhost:8080/admin/feature-flags/use-auth-microservice/percentage?percentage=25"

# Week 6: Full rollout
curl -X PUT "http://localhost:8080/admin/feature-flags/use-auth-microservice/percentage?percentage=100"

# Emergency rollback (instant!)
curl -X PUT "http://localhost:8080/admin/feature-flags/use-auth-microservice/percentage?percentage=0"
```

### Benefits

✅ **Instant rollback** (no redeployment)
✅ **Gradual rollout** reduces risk
✅ **A/B testing** capability
✅ **User-specific** overrides for QA
✅ **Production testing** on subset of users

---

## Canary Deployments

### What is Canary Deployment?

Deploy new version to **small subset of servers/users** first, monitor metrics, then gradually roll out.

### Implementation with Kubernetes

**1. Create Canary Deployment:**

```yaml
# k8s/auth-service-canary.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service-canary
  labels:
    app: auth-service
    version: canary
spec:
  replicas: 1  # 10% of stable replicas (1 out of 10)
  selector:
    matchLabels:
      app: auth-service
      version: canary
  template:
    metadata:
      labels:
        app: auth-service
        version: canary
    spec:
      containers:
      - name: auth-service
        image: url-shortener/auth-service:2.0.0-canary  # NEW VERSION
        ports:
        - containerPort: 8081
        env:
        - name: VERSION
          value: "canary"
```

**2. Stable Deployment:**

```yaml
# k8s/auth-service-stable.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service-stable
  labels:
    app: auth-service
    version: stable
spec:
  replicas: 9  # 90% of traffic
  selector:
    matchLabels:
      app: auth-service
      version: stable
  template:
    metadata:
      labels:
        app: auth-service
        version: stable
    spec:
      containers:
      - name: auth-service
        image: url-shortener/auth-service:1.0.0  # OLD VERSION
        ports:
        - containerPort: 8081
```

**3. Service Routes to Both:**

```yaml
# k8s/auth-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector:
    app: auth-service  # Matches BOTH stable and canary
  ports:
  - protocol: TCP
    port: 8081
    targetPort: 8081
```

**Result:**
- Kubernetes load balancer sends ~10% traffic to canary
- ~90% traffic to stable
- Monitor metrics to decide next step

**4. Progressive Rollout:**

```bash
# Week 1: 10% canary (1 pod canary, 9 pods stable)
kubectl scale deployment auth-service-canary --replicas=1
kubectl scale deployment auth-service-stable --replicas=9

# Week 2: 50% canary
kubectl scale deployment auth-service-canary --replicas=5
kubectl scale deployment auth-service-stable --replicas=5

# Week 3: 100% canary (promote to stable)
kubectl scale deployment auth-service-stable --replicas=0
kubectl scale deployment auth-service-canary --replicas=10
# Update stable to new version
kubectl set image deployment/auth-service-stable auth-service=url-shortener/auth-service:2.0.0
```

### Automated Canary with Flagger

```yaml
# flagger-canary.yaml
apiVersion: flagger.app/v1beta1
kind: Canary
metadata:
  name: auth-service
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  service:
    port: 8081
  analysis:
    interval: 1m
    threshold: 5
    maxWeight: 50
    stepWeight: 10
    metrics:
    - name: request-success-rate
      thresholdRange:
        min: 99
      interval: 1m
    - name: request-duration
      thresholdRange:
        max: 500
      interval: 1m
```

**Flagger automatically:**
1. Deploys canary
2. Monitors metrics (success rate, latency)
3. Increases traffic 10% every minute if healthy
4. **Auto-rollback** if metrics degrade
5. Promotes to stable if all checks pass


## Feature Flags vs Canary Deployments

  Different Purposes:

  | Aspect      | Feature Flags                                 | Canary Deployments                         |
  |-------------|-----------------------------------------------|--------------------------------------------|
  | Controls    | WHERE traffic goes (monolith vs microservice) | WHICH VERSION of microservice gets traffic |
  | Layer       | Application/routing logic                     | Infrastructure/deployment                  |
  | Granularity | User-level (specific users)                   | Traffic-level (% of all users)             |
  | Rollback    | Instant (toggle flag)                         | 1-2 min (redeploy/scale)                   |
  | Use Case    | Migration, A/B testing                        | Version rollout, safety                    |

Feature flags control routing at the application layer - whether traffic goes to the monolith or microservice. They 
  provide instant rollback and user-level granularity.

  Canary deployments operate at the infrastructure layer - gradually rolling out new versions of the same service. They 
  provide automated rollback based on metrics.

  Together, they give you two safety nets:
  1. If the entire microservice is broken → toggle feature flag (instant)
  2. If just the new version has issues → scale down canary (1-2 min)

  For example, at Netflix they use Zuul with feature flags to route between services, and Red/Black deployments 
  (canary-style) to roll out new versions. This is how they can deploy hundreds of times a day with minimal incidents."



## Database Migration Strategies

### Challenge

Microservices need **separate databases**, but can't cut over instantly.

### Strategy 1: Shared Database (Transitional)

**Phase 1: Monolith & Microservices Share DB**

```
┌──────────┐    ┌────────────────┐
│ Monolith │───▶│                │
└──────────┘    │   PostgreSQL   │
                │                │
┌──────────┐    │   users table  │
│Auth Svc  │───▶│   urls table   │
└──────────┘    │   analytics    │
                └────────────────┘
```

**Pros:**
- ✅ Simple migration
- ✅ No data sync needed
- ✅ Consistent transactions

**Cons:**
- ❌ Not true microservices (shared data layer)
- ❌ Schema changes affect both systems
- ❌ Database becomes bottleneck

**Use:** During strangler fig migration (temporary)

### Strategy 2: Database Replication

**Phase 2: Replicate Data to Microservice DBs**

```
┌──────────┐    ┌────────────────┐
│ Monolith │───▶│  Main DB       │
└────┬─────┘    │  (master)      │
     │          └───┬────────────┘
     │              │
     │          Replication
     │              │
     │          ┌───▼────────────┐
     │          │  Auth DB       │
┌────▼──────┐  │  (read replica)│
│ Auth Svc  │─▶│                │
└───────────┘  └────────────────┘
```

**Implementation:**

```yaml
# docker-compose.yml
services:
  postgres-main:
    image: postgres:15
    environment:
      - POSTGRES_DB=urlshortener
    volumes:
      - ./init-replication.sh:/docker-entrypoint-initdb.d/init.sh

  postgres-auth-replica:
    image: postgres:15
    environment:
      - POSTGRES_MASTER_HOST=postgres-main
    command: >
      postgres -c 'primary_conninfo=host=postgres-main port=5432'
```

```sql
-- init-replication.sh
-- On master
CREATE PUBLICATION auth_publication FOR TABLE users;

-- On replica
CREATE SUBSCRIPTION auth_subscription
    CONNECTION 'host=postgres-main dbname=urlshortener'
    PUBLICATION auth_publication;
```

**Pros:**
- ✅ Microservice reads from own DB
- ✅ Low latency
- ✅ Gradual migration

**Cons:**
- ❌ Replication lag (eventual consistency)
- ❌ Writes still go to main DB

### Strategy 3: Dual Writes

**Phase 3: Write to Both Databases**

```
┌──────────┐
│ Monolith │
└────┬─────┘
     │
     │  Writes to Main DB
     │
┌────▼──────┐    ┌────────────────┐
│ Auth Svc  │───▶│  Main DB       │
└────┬──────┘    └────────────────┘
     │
     │  Also writes to
     │  its own DB
     │
     │           ┌────────────────┐
     └──────────▶│  Auth DB       │
                 │  (independent) │
                 └────────────────┘
```

**Implementation:**

```java
// AuthServiceImpl.java
@Transactional
public User createUser(UserDto userDto) {
    User user = new User();
    user.setUsername(userDto.getUsername());
    user.setPassword(passwordEncoder.encode(userDto.getPassword()));

    // Write to Main DB (for monolith)
    User savedInMain = mainDbUserRepository.save(user);

    // ALSO write to Auth Service DB
    try {
        authDbUserRepository.save(user);
    } catch (Exception e) {
        // Log but don't fail - eventual consistency
        log.error("Failed to sync to auth DB: {}", e.getMessage());
        // Publish to queue for retry
        syncQueue.publish(new UserSyncEvent(user));
    }

    return savedInMain;
}
```

**Pros:**
- ✅ Both systems have data
- ✅ Preparation for cutover
- ✅ Microservice can validate independently

**Cons:**
- ❌ Complex logic (2x writes)
- ❌ Risk of inconsistency
- ❌ Performance overhead

### Strategy 4: Event-Driven Sync

**Phase 4: Publish Events, Microservices Subscribe**

```
┌──────────┐
│ Monolith │
└────┬─────┘
     │
     │  Write + Publish Event
     │
┌────▼──────────────┐
│  Main DB          │
└───────────────────┘
     │
     │  Event: UserCreated
     │
┌────▼──────────────┐
│  Kafka/RabbitMQ   │
└────┬──────────────┘
     │
     │  Subscribe
     │
┌────▼──────┐    ┌────────────────┐
│ Auth Svc  │───▶│  Auth DB       │
└───────────┘    │  (sync'd)      │
                 └────────────────┘
```

**Implementation:**

```java
// Monolith - AuthController.java
@PostMapping("/register")
public ResponseEntity<User> register(@RequestBody UserDto userDto) {
    User user = authService.createUser(userDto);

    // Publish event for microservice to consume
    eventPublisher.publish(new UserCreatedEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt()
    ));

    return ResponseEntity.ok(user);
}
```

```java
// Auth Service - UserEventListener.java
@Component
public class UserEventListener {

    @KafkaListener(topics = "user-events")
    public void handleUserCreated(UserCreatedEvent event) {
        // Sync to local DB
        User localUser = new User();
        localUser.setId(event.getUserId());
        localUser.setUsername(event.getUsername());
        localUser.setEmail(event.getEmail());

        userRepository.save(localUser);
        log.info("Synced user {} to auth service DB", event.getUsername());
    }
}
```

**Pros:**
- ✅ **Decoupled** systems
- ✅ Asynchronous (no performance hit)
- ✅ Replay events if sync fails
- ✅ Audit trail of all changes

**Cons:**
- ❌ Eventual consistency (slight delay)
- ❌ Requires message queue infrastructure

### Strategy 5: Database per Service (Final State)

**Phase 5: Full Separation**

```
┌──────────┐    ┌────────────┐
│Monolith  │───▶│ Monolith DB│
│(retired) │    │ (archived) │
└──────────┘    └────────────┘

┌──────────┐    ┌────────────┐
│Auth Svc  │───▶│  Auth DB   │
└──────────┘    │  (users)   │
                └────────────┘

┌──────────┐    ┌────────────┐
│URL Svc   │───▶│  URL DB    │
└──────────┘    │  (urls)    │
                └────────────┘
```

**When:** After 100% traffic migrated and monolith retired.

### Migration Timeline

| Week | Strategy | Monolith | Microservice | Notes |
|------|----------|----------|--------------|-------|
| 1-2 | Shared DB | Write & Read | Write & Read | Both use same DB |
| 3-4 | Replication | Write (master) | Read (replica) | Microservice reads from replica |
| 5-6 | Dual Write | Write to both | Write to both | Preparation for cutover |
| 7-8 | Event-Driven | Publishes events | Consumes events | Async sync |
| 9+ | Separate DBs | (retired) | Independent | Monolith gone |

##  "What does 'separate database per microservice' mean? My company has 51 microservices sharing one database - each service owns specific tables, can read others' tables, but cannot write to them. Is this the correct approach?"

Database Separation Levels:

  | Level             | Setup                    | Your Company   | True Microservices                |
  |-------------------|--------------------------|----------------|-----------------------------------|
  | Database Instance | 1 shared PostgreSQL      | ✅ Yes          | ❌ Should be 51 separate instances |
  | Schema            | 1 shared schema          | ✅ Yes          | ❌ Should be 51 separate schemas   |
  | Tables            | Each service owns tables | ✅ Good         | ✅ Good                            |
  | Write access      | Only to owned tables     | ✅ Correct      | ✅ Correct                         |
  | Read access       | Can read any table       | ❌ Anti-pattern | ❌ Should use APIs only            |

  ---
  What You Have (Shared DB with Table Ownership):

  ┌──────────────────────────────────────┐
  │   PostgreSQL (1 instance)            │
  │                                      │
  │   auth_users      ← Auth Service     │
  │   auth_tokens                        │
  │                                      │
  │   url_mappings    ← URL Service      │
  │   url_clicks                         │
  │                                      │
  │   ... (51 services, 51 table sets)   │
  └──────────────────────────────────────┘

  Problem: URL Service can SELECT from auth_users
           (bypasses Auth Service API)

  ---
  What True Microservices Need (Separate Instances):

  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
  │ PostgreSQL 1 │  │ PostgreSQL 2 │  │ PostgreSQL 3 │
  │  auth_users  │  │ url_mappings │  │ analytics_*  │
  └──────────────┘  └──────────────┘  └──────────────┘
         ↑                  ↑                  ↑
    Auth Service       URL Service      Analytics Svc
    
    Communication: REST APIs ONLY (no direct DB access)

  ---
  Key Issues with Your Setup:

  ❌ Single point of failure: 1 DB down = all 51 services down❌ Can't scale independently: Can't scale just the URL service
   DB❌ Cross-service reads = tight coupling: Changes in auth_users break URL Service❌ Can't use different DB types: All
  stuck with PostgreSQL

  ---
  Industry Standard (Amazon's Rule):

  "No direct database access between services. All communication via APIs."

  Companies like Netflix, Uber, Amazon: 1 microservice = 1 database instance

  ---
  Your Company's Approach: Rating

  Score: 5/10 for microservices

  - ✅ Better than shared tables in monolith
  - ✅ Clear table ownership
  - ❌ Not true microservices (shared DB)
  - ❌ Direct cross-table reads (breaks encapsulation)

  ---
  Recommendation:

  Now: Acceptable for cost/complexity reasons

  Long-term migration path:
  1. Phase 1: Separate schemas (auth_db.users, url_db.urls)
  2. Phase 2: Separate instances for high-traffic services
  3. Phase 3: API-only communication (remove direct DB reads)

  **Interview Perspective**

  **Question: "Do microservices need separate databases?"**

  **Weak Answer:**
  "Yes, each microservice should have its own table."

  **Strong Answer:**
  "True microservices should have separate database instances to ensure complete isolation and independent scaling. However,
   I've worked with a pragmatic middle-ground approach where we had 51 services sharing a database with separate tables. 

  While this was better than a monolithic shared database, it had limitations:
  - Single point of failure
  - Couldn't scale databases independently
  - Cross-service reads created tight coupling

  The proper approach is separate database instances with API-based communication, following Amazon's mandate: 'No direct 
  database access between teams.' This enables independent deployments, technology choices, and scaling."
---

## Traffic Routing Strategies

### Option 1: NGINX

**Configuration:**

```nginx
# nginx.conf
upstream monolith {
    server localhost:8080 weight=90;
}

upstream auth_microservice {
    server localhost:8081 weight=10;
}

# Weighted load balancing (90/10 split)
server {
    listen 80;

    location /api/v1/auth {
        proxy_pass http://auth_microservice;
    }

    location /api/v1/urls {
        # 90% monolith, 10% url-service
        proxy_pass http://url_service_or_monolith;
    }
}
```

**Pros:**
- ✅ Simple
- ✅ Fast (C-based)
- ✅ Battle-tested

**Cons:**
- ❌ Config changes require reload
- ❌ Limited dynamic routing

### Option 2: Envoy Proxy

**Configuration:**

```yaml
# envoy.yaml
static_resources:
  listeners:
  - address:
      socket_address:
        address: 0.0.0.0
        port_value: 80
    filter_chains:
    - filters:
      - name: envoy.filters.network.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          route_config:
            name: local_route
            virtual_hosts:
            - name: backend
              domains: ["*"]
              routes:
              - match:
                  prefix: "/api/v1/auth"
                route:
                  weighted_clusters:
                    clusters:
                    - name: auth_service
                      weight: 10
                    - name: monolith
                      weight: 90
  clusters:
  - name: auth_service
    connect_timeout: 0.25s
    type: STRICT_DNS
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: auth_service
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: auth-service
                port_value: 8081
  - name: monolith
    connect_timeout: 0.25s
    type: STRICT_DNS
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: monolith
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: monolith
                port_value: 8080
```

**Pros:**
- ✅ Advanced traffic management
- ✅ Observability (metrics, tracing)
- ✅ Dynamic configuration via xDS API

**Cons:**
- ❌ Complex setup
- ❌ Steeper learning curve

### Option 3: Spring Cloud Gateway (Your Project)

**Configuration:**

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
      - id: auth-route-canary
        uri: lb://auth-service
        predicates:
        - Path=/api/v1/auth/**
        - Weight=auth-group, 10  # 10% to microservice

      - id: auth-route-stable
        uri: http://monolith:8080
        predicates:
        - Path=/api/v1/auth/**
        - Weight=auth-group, 90  # 90% to monolith
```

**Pros:**
- ✅ Spring ecosystem
- ✅ Java-based (familiar)
- ✅ Good Eureka integration

**Cons:**
- ❌ Less performant than NGINX/Envoy
- ❌ Limited routing features

### Option 4: Service Mesh (Istio)

**Configuration:**

```yaml
# istio-virtual-service.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: auth-service
spec:
  hosts:
  - auth-service
  http:
  - match:
    - uri:
        prefix: /api/v1/auth
    route:
    - destination:
        host: auth-service
        subset: canary
      weight: 10
    - destination:
        host: monolith
      weight: 90
```

**Pros:**
- ✅ Traffic management
- ✅ Security (mTLS)
- ✅ Observability built-in
- ✅ No code changes

**Cons:**
- ❌ Complex infrastructure
- ❌ Operational overhead
- ❌ Resource intensive

---

## Rollback Mechanisms

### 1. Feature Flag Rollback (Instant)

**Fastest rollback method:**

```bash
# Instant rollback - just toggle flag
curl -X PUT "http://api-gateway/admin/feature-flags/use-auth-microservice/percentage?percentage=0"

# Traffic immediately routes back to monolith
# No deployment needed
# Takes < 1 second
```

### 2. Kubernetes Rollback

```bash
# View deployment history
kubectl rollout history deployment/auth-service

# Rollback to previous version
kubectl rollout undo deployment/auth-service

# Rollback to specific revision
kubectl rollout undo deployment/auth-service --to-revision=3

# Time: 1-2 minutes (pod restart)
```

### 3. Blue-Green Deployment

**Keep both versions running:**

```
┌────────────────┐
│ Load Balancer  │
└────────┬───────┘
         │
    [Switch here]
         │
    ┌────┴─────┐
    │          │
┌───▼────┐ ┌──▼─────┐
│ Blue   │ │ Green  │
│(v1.0)  │ │(v2.0)  │
│Running │ │Running │
└────────┘ └────────┘
```

**Rollback:**
```bash
# Switch load balancer back to blue
kubectl patch service auth-service -p '{"spec":{"selector":{"version":"blue"}}}'

# Instant - just DNS/routing change
```

### 4. Database Rollback

**Most complex:**

```sql
-- If dual-write failed, replay events from queue
SELECT * FROM event_store
WHERE event_type = 'UserCreated'
  AND created_at > '2025-11-01'
ORDER BY created_at;

-- Reprocess events to sync data
```

### Rollback Decision Matrix

| Scenario | Rollback Method | Time | Risk |
|----------|----------------|------|------|
| High error rate | Feature flag | <1 sec | None |
| Performance degradation | Feature flag | <1 sec | None |
| Critical bug found | Feature flag | <1 sec | None |
| Bad deployment | K8s rollback | 1-2 min | Low |
| Data corruption | DB rollback | Hours | High |

---

## Monitoring & Metrics

### Key Metrics to Track

**1. Traffic Metrics:**
```
- Requests/sec to monolith
- Requests/sec to microservice
- Traffic split percentage (should match target)
```

**2. Error Metrics:**
```
- Error rate (4xx, 5xx) for monolith
- Error rate for microservice
- Diff in error rates (canary vs stable)
```

**3. Performance Metrics:**
```
- P50, P95, P99 latency (monolith)
- P50, P95, P99 latency (microservice)
- Latency regression (should be <10%)
```

**4. Business Metrics:**
```
- User registrations (both systems)
- URL creations (both systems)
- Revenue impact (any drops?)
```

### Prometheus Queries

```promql
# Traffic split verification
sum(rate(http_requests_total{service="auth-monolith"}[5m]))
/
sum(rate(http_requests_total{service=~"auth-.*"}[5m]))

# Error rate comparison
rate(http_requests_total{service="auth-microservice",status=~"5.."}[5m])
vs
rate(http_requests_total{service="auth-monolith",status=~"5.."}[5m])

# Latency P99 comparison
histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service="auth-microservice"}[5m]))
vs
histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service="auth-monolith"}[5m]))
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Canary Deployment - Auth Service",
    "panels": [
      {
        "title": "Traffic Split",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{service='auth-microservice'}[5m]))",
            "legendFormat": "Microservice"
          },
          {
            "expr": "sum(rate(http_requests_total{service='auth-monolith'}[5m]))",
            "legendFormat": "Monolith"
          }
        ]
      },
      {
        "title": "Error Rate Comparison",
        "targets": [
          {
            "expr": "rate(http_requests_total{service='auth-microservice',status=~'5..'}[5m])",
            "legendFormat": "Microservice Errors"
          },
          {
            "expr": "rate(http_requests_total{service='auth-monolith',status=~'5..'}[5m])",
            "legendFormat": "Monolith Errors"
          }
        ]
      },
      {
        "title": "P99 Latency",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service='auth-microservice'}[5m]))",
            "legendFormat": "Microservice P99"
          },
          {
            "expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service='auth-monolith'}[5m]))",
            "legendFormat": "Monolith P99"
          }
        ]
      }
    ]
  }
}
```

### Alerting Rules

```yaml
# prometheus-alerts.yaml
groups:
- name: canary-alerts
  rules:
  - alert: CanaryErrorRateHigh
    expr: |
      rate(http_requests_total{service="auth-microservice",status=~"5.."}[5m])
      >
      rate(http_requests_total{service="auth-monolith",status=~"5.."}[5m]) * 1.5
    for: 5m
    annotations:
      summary: "Canary error rate 50% higher than stable"
      description: "Consider rolling back canary deployment"

  - alert: CanaryLatencyRegression
    expr: |
      histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service="auth-microservice"}[5m]))
      >
      histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service="auth-monolith"}[5m])) * 1.2
    for: 10m
    annotations:
      summary: "Canary P99 latency 20% slower"
      description: "Performance regression detected"
```

---

## Industry Examples

### 1. Netflix

**Timeline:** 2009-2016 (7 years)

**Strategy:**
- Started with monolithic DVD rental app
- Strangler fig pattern with AWS services
- Built microservices alongside monolith
- Used Eureka (which they open-sourced) for service discovery
- Zuul (API Gateway) for routing
- Chaos engineering to test resilience

**Key Learnings:**
- "It took 7 years to fully decompose"
- "We ran both systems in parallel for years"
- "Feature flags were critical for gradual rollout"

### 2. Amazon

**Timeline:** 2001-2006

**Strategy:**
- Started with monolithic Obidos platform
- Created "two-pizza teams" owning services
- Service-oriented architecture (pre-microservices term)
- Mandate: All teams must expose APIs
- Used internal service mesh for routing

**Key Quote (Werner Vogels):**
> "We gave teams ownership of their services from design to operations. Anyone could call anyone else's service via API."

### 3. Uber

**Timeline:** 2012-2016

**Strategy:**
- Monolithic Python app → microservices
- Built in-house service mesh (later became open-source)
- Used feature flags for gradual rollout
- Ring deployment: Internal → Drivers → Riders

**Stats:**
- Started: 1 monolith
- 2015: 100+ microservices
- 2016: 1000+ microservices
- 2020: 2200+ microservices

### 4. Airbnb

**Timeline:** 2017-2020

**Strategy:**
- Monolithic Rails app
- Service-Oriented Architecture (SOA) first
- Then microservices with Kubernetes
- Used Envoy for traffic management
- SmartStack for service discovery

**Key Learning:**
> "We focused on high-impact services first: payments, pricing, and search. Not everything needs to be a microservice."

### 5. Twitter

**Timeline:** 2010-2013

**Strategy:**
- Ruby on Rails monolith (fail whale era)
- Migrated to Scala microservices
- Used Finagle (RPC framework)
- Zipkin for distributed tracing (open-sourced)

**Impact:**
- **Before:** 200ms tweet posting, frequent outages
- **After:** 10ms tweet posting, 3-nines availability

### Common Patterns Across All

| Company | Pattern | Timeline | Key Tech |
|---------|---------|----------|----------|
| Netflix | Strangler Fig | 7 years | Eureka, Zuul, Hystrix |
| Amazon | API Mandate | 5 years | Internal mesh |
| Uber | Ring Deployment | 4 years | Feature flags |
| Airbnb | High-impact first | 3 years | Envoy, K8s |
| Twitter | Platform Rewrite | 3 years | Finagle, Zipkin |

**Key Takeaways:**
1. **All took years**, not months
2. **All used gradual rollout**, not big bang
3. **All kept monolith running** during transition
4. **All used feature flags** for traffic control
5. **All invested in observability** (metrics, tracing)

---

## Interview Talking Points

### Question: "How do you migrate a monolith to microservices without downtime?"

**❌ Weak Answer:**
> "I'd build the microservices, test them, then switch over during a maintenance window."

**✅ Strong Answer:**
> "I'd use the strangler fig pattern with these steps:
>
> 1. **Identify** high-value, low-coupling services to extract first - for example, authentication.
>
> 2. **Build** the Auth microservice alongside the monolith, initially sharing the database.
>
> 3. **Deploy** both systems and implement a routing layer with feature flags - I'd use Redis-backed flags in the API Gateway.
>
> 4. **Canary release** starting with 5-10% of traffic to the microservice, monitoring error rates and latency in Grafana.
>
> 5. **Gradually increase** to 25%, 50%, 75%, 100% over 4-6 weeks based on metrics.
>
> 6. **Database migration** happens last - start with replication, then dual writes via event-driven sync, finally cut over to separate databases.
>
> 7. **Rollback** is instant via feature flag toggle if issues arise.
>
> This approach gave us zero downtime, instant rollback capability, and reduced risk through gradual rollout. Similar to how Netflix migrated over 7 years using strangler fig."

### Question: "What metrics do you monitor during migration?"

**❌ Weak Answer:**
> "I'd check if the service is running and look at logs."

**✅ Strong Answer:**
> "I'd track four categories:
>
> **Traffic metrics:**
> - Request rates to verify traffic split matches target (e.g., 10% canary)
> - Useful to catch routing misconfigurations
>
> **Error metrics:**
> - Compare 4xx/5xx rates between monolith and microservice
> - Alert if canary error rate exceeds stable by 50%
>
> **Performance metrics:**
> - P50, P95, P99 latency comparison
> - Alert on 20%+ regression
>
> **Business metrics:**
> - User signups, order completion rates
> - Catch issues that don't show in technical metrics
>
> I'd use Prometheus for collection, Grafana for dashboards, and PagerDuty for alerting. Set up automated rollback if error rate crosses threshold."

### Question: "How do you handle database migration?"

**❌ Weak Answer:**
> "Export data from monolith DB, import to microservice DB."

**✅ Strong Answer:**
> "Database migration is the most complex part. I'd use a phased approach:
>
> **Phase 1 (Weeks 1-2): Shared database**
> - Both monolith and microservice read/write to same DB
> - Lowest risk, but not true microservices pattern
>
> **Phase 2 (Weeks 3-4): Replication**
> - Microservice reads from a read replica
> - Monolith still handles writes
> - Validates microservice doesn't need write access yet
>
> **Phase 3 (Weeks 5-6): Dual writes**
> - Microservice writes to both main DB and its own
> - Or monolith publishes events, microservice consumes
> - Kafka/RabbitMQ for async sync
>
> **Phase 4 (Week 7+): Separate databases**
> - Microservice fully independent
> - Monolith retired for that domain
>
> Key insight: **Database separation happens last**, after all traffic migrated. It's the highest-risk change."

### Question: "What if the microservice fails in production?"

**❌ Weak Answer:**
> "I'd redeploy the old version and investigate."

**✅ Strong Answer:**
> "This is why feature flags are critical:
>
> **Immediate response (< 30 seconds):**
> - Toggle feature flag to 0% → all traffic back to monolith
> - No deployment needed, instant rollback
> - Users experience minimal impact
>
> **Within 5 minutes:**
> - Check Grafana dashboards for root cause
> - Review error logs in Kibana
> - Check Zipkin traces for failed requests
>
> **Within 30 minutes:**
> - If hotfix available, deploy to 5% canary first
> - If not, keep flag at 0% and investigate offline
>
> **Longer term:**
> - Root cause analysis (RCA) document
> - Add alerting to catch similar issues earlier
> - Improve canary metrics to detect before 100% rollout
>
> This is why we don't jump to 100% immediately - canary deployments limit blast radius."

### Question: "How long does a typical monolith to microservices migration take?"

**❌ Weak Answer:**
> "Depends on the size, maybe a few months."

**✅ Strong Answer:**
> "Based on industry examples:
>
> **Small apps (10K LOC):** 3-6 months
> **Medium apps (100K LOC):** 6-12 months
> **Large apps (1M+ LOC):** 2-7 years
>
> Examples:
> - **Netflix:** 7 years
> - **Amazon:** 5 years
> - **Uber:** 4 years
> - **Airbnb:** 3 years
>
> The key insight: **You never finish**. It's continuous evolution. Even after the monolith is retired, you're constantly:
> - Splitting microservices that grew too large
> - Merging microservices that were too granular
> - Optimizing boundaries
>
> For my URL shortener project with ~50K LOC, I'd estimate:
> - **Development-ready:** 6-8 weeks
> - **Production migration:** 12-16 weeks
> - **Full retirement of monolith:** 6 months
>
> The migration timeline is driven by risk management, not technical complexity."

---

## Implementation Timeline

### Recommended 12-Week Migration Plan

**Weeks 1-2: Infrastructure & First Service**
- ✅ Build Auth microservice
- ✅ Deploy alongside monolith
- ✅ Set up feature flags (Redis)
- ✅ Set up monitoring (Prometheus, Grafana)
- ✅ Configure routing layer (API Gateway)
- Traffic: 100% monolith

**Weeks 3-4: Auth Service Canary**
- ✅ Enable feature flag for 10% of users
- ✅ Monitor metrics daily
- ✅ Increase to 25% if healthy
- ✅ Increase to 50%
- Traffic: 50% monolith, 50% auth service

**Weeks 5-6: Auth Service Completion + URL Service Start**
- ✅ Auth service to 100%
- ✅ Build URL service
- ✅ Deploy URL service alongside monolith
- ✅ Start 10% canary for URL service
- Traffic: 0% monolith auth, 90% monolith URLs

**Weeks 7-8: URL Service Rollout**
- ✅ URL service 25% → 50% → 75% → 100%
- ✅ Monitor redirect performance (critical path)
- ✅ Validate caching works correctly
- Traffic: 0% monolith core features

**Weeks 9-10: Analytics Service + Database Separation**
- ✅ Build analytics service
- ✅ Deploy and rollout (faster, read-only)
- ✅ Set up database replication
- ✅ Start dual writes
- Traffic: All microservices

**Weeks 11-12: Final Cutover**
- ✅ Migrate to separate databases
- ✅ Retire monolith (keep as fallback)
- ✅ Full production validation
- ✅ Update documentation

### Effort Breakdown

| Phase | Hours | Notes |
|-------|-------|-------|
| Build microservices | 51h | From your guide (Phases 0-10) |
| Feature flags setup | 8h | Redis, admin API, routing |
| Monitoring setup | 6h | Dashboards, alerts |
| Canary rollouts | 20h | Gradual increase, monitoring |
| Database migration | 12h | Replication, dual writes, cutover |
| Testing & validation | 15h | Each rollout stage |
| Documentation | 8h | Runbooks, RCA templates |
| **Total** | **120h** | **~12 weeks at 10h/week** |

---

## Conclusion

### Key Principles

1. **Gradual over Big Bang** - Always prefer incremental changes
2. **Monitoring is Critical** - Can't improve what you don't measure
3. **Rollback Plans First** - Plan for failure before success
4. **Business Metrics Matter** - Technical success ≠ Business success
5. **Learn from Giants** - Netflix, Amazon, Uber did this over years

### For Your Project

**Current state:** Your guide provides excellent microservices architecture education

**Production gap:** Missing strangler fig, feature flags, canary deployments

**Interview strategy:**
- **Demo:** Show complete microservices with observability
- **Discuss:** Explain how you'd add gradual migration for production
- **Compare:** Reference Netflix/Amazon migration timelines

### Next Steps

1. **Complete Phases 0-13** from your guide (71 hours)
2. **Add this to README:** "Implements complete microservices architecture. For production zero-downtime migration, see zero-downtime-migration-guide.md"
3. **Study this document** for interview prep
4. **Practice explaining:** "How would you migrate this with zero downtime?"

---

## Additional Resources

**Books:**
- *Building Microservices* by Sam Newman (Chapter 5: Splitting the Monolith)
- *Monolith to Microservices* by Sam Newman (entire book on this topic)

**Papers:**
- Martin Fowler: "Strangler Fig Application"
- Martin Fowler: "CanaryRelease"

**Case Studies:**
- Netflix Tech Blog: "Migrating to Microservices"
- Uber Engineering: "Service-Oriented Architecture at Uber"
- Airbnb Engineering: "Building Services at Airbnb"

**Tools:**
- LaunchDarkly (feature flags SaaS)
- Flagger (automated canary deployments)
- Istio (service mesh)
- Envoy (proxy for traffic management)

---

**Document Version:** 1.0
**Last Updated:** November 10, 2025
**Next Review:** When starting production migration


**NGINX vs Envoy vs Spring Cloud Gateway**
---
  Industry Trends (2024-2025):

  | Scenario                 | Choice                | Reason                |
  |--------------------------|-----------------------|-----------------------|
  | Traditional web apps     | NGINX                 | Simple, proven, fast  |
  | Kubernetes microservices | Envoy (via Istio)     | Service mesh standard |
  | Spring Boot shops        | Spring Cloud Gateway  | Ecosystem fit         |
  | Large scale (Netflix)    | NGINX → Zuul → Custom | Evolving needs        |
  | Startups                 | NGINX initially       | Low complexity        |


**For Interviews:**
  - Know NGINX (most common)
  - Understand Envoy (shows cloud-native knowledge)
  - Mention Spring Cloud Gateway for Spring context

**For Your Project:**
  - Use Spring Cloud Gateway (since you're already in Spring Boot)
  - Mention in interviews: "In production, I'd evaluate NGINX for simplicity or Envoy for advanced service mesh features,
  but chose Spring Cloud Gateway for tight Spring integration"



## Why NOT Database for Feature Flags?

  API Gateway receives 1000 req/sec
  ↓
  Each request checks feature flag
  ↓
  1000 DB queries/sec JUST for flags
  ↓
  PostgreSQL connection pool exhausted
  ↓
  API Gateway slows down/crashes

  Real example:
  - Company uses DB for feature flags
  - Traffic spike during Black Friday
  - Feature flag queries overwhelm DB
  - Entire system goes down because of flag checks

  Solution: Move to Redis, problem solved.

   Summary Table

  | Option        | Speed  | Shared State | Real-time | Complexity | Use Case              |
  |---------------|--------|--------------|-----------|------------|-----------------------|
  | Redis         | <1ms   | ✅            | ✅         | Low        | Production (standard) |
  | Database      | 5-20ms | ✅            | ✅         | Low        | Low-traffic apps      |
  | In-Memory     | <0.1ms | ❌            | ❌         | Very Low   | Single instance       |
  | Config Server | Slow   | ✅            | ❌         | Medium     | Static flags          |
  | Hazelcast     | <1ms   | ✅            | ✅         | High       | If already using it   |


**Question: "Why use Redis for feature flags instead of a database?"**
  Strong Answer:
  "Feature flags are checked on every request, so they need sub-millisecond latency. Redis provides:

  1. Speed: <1ms vs 5-20ms for PostgreSQL
  2. Shared state: Multiple gateway instances read from one source
  3. Real-time updates: Change flag without restarting
  4. Low DB load: Offloads high-frequency reads from primary database

  In production, I'd use a hybrid approach: local cache (0.1ms) with 1-minute TTL, backed by Redis, to reduce Redis load by 
  90%. This is what LaunchDarkly and other feature flag systems do."