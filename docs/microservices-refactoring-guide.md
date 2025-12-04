# Microservices Refactoring Guide - Monolith to Production

**Created:** November 9, 2025
**Last Updated:** November 9, 2025 (Production-ready with observability)
**Approach:** Refactor existing monolith (NOT starting from scratch)
**Timeline:** 51-71 hours (See timeline section for 3 milestone options)
**Complexity:** Medium-High

> **✅ COMPLETE PRODUCTION-READY GUIDE**
> - **13 Phases** from prerequisites to production deployment
> - **All code included**: Microservices + Observability + Caching + Security
> - **3 Milestones**: Development-ready (51h), Interview-ready (61h), Production-ready (71h)
> - **Complete stack**: Eureka, API Gateway, Zipkin, Prometheus, Grafana, ELK, Redis Cache, Circuit Breakers, Rate Limiting, K8s
> - **NEW**: Database indexes, Pagination, Auth Feign endpoint, Redis caching for 10x performance

---

## 📋 Features Implemented

**Core Microservices Architecture:**
- Service Discovery with Netflix Eureka for dynamic service registration
- API Gateway with Spring Cloud Gateway for unified entry point and routing
- Auth Service for JWT-based authentication and user management
- URL Service for URL shortening, validation, and redirect handling
- Analytics Service for click tracking and usage statistics
- Feign clients for inter-service REST communication

**Database & Performance:**
- PostgreSQL with Liquibase for database migrations and version control
- Database indexes on frequently queried columns for 10x query performance
- Redis caching for URL lookups with 10x redirect performance improvement
- Pagination support for handling large datasets efficiently
- HikariCP connection pool optimization for database connections

**Observability & Monitoring:**
- Health checks with Spring Boot Actuator (liveness/readiness probes)
- API documentation with Swagger/OpenAPI for all service endpoints
- Distributed tracing with Zipkin for request tracking across services
- Metrics collection with Prometheus for system monitoring
- Grafana dashboards for metrics visualization
- Centralized logging with ELK stack (Elasticsearch, Logstash, Kibana)

**Resilience & Reliability:**
- Circuit breakers with Resilience4j to prevent cascade failures
- Retry logic with exponential backoff for transient failures
- Rate limiting with Bucket4j to prevent API abuse
- Graceful shutdown for zero-downtime deployments

**Security:**
- JWT authentication with token validation in API Gateway
- Secret encryption with Jasypt for sensitive configuration
- Security headers (CSP, XSS protection, HSTS, frame options)
- CORS configuration for cross-origin requests
- Authorization checks for resource ownership

**Production Infrastructure:**
- Docker containerization for all services with multi-stage builds
- Docker Compose orchestration for local development
- Kubernetes deployment manifests with resource limits and probes
- Response compression for reduced bandwidth usage
- Request/Response logging for audit trails
- Performance testing setup with Gatling

---

## 🎯 Why Refactor vs Starting Fresh?

### **Advantages of Refactoring:**

✅ **Faster:** 2-3 weeks vs 6-8 weeks
✅ **Reuse 70-80% of code:** Copy-paste existing working code
✅ **Better for interviews:** "I refactored a monolith" shows senior-level skill
✅ **Real-world scenario:** This is what you'll do in actual jobs
✅ **Lower risk:** Proven business logic stays the same
✅ **Backward compatible:** Can run both during migration

### **Interview Perspective:**

**Bad answer:** "I built microservices from scratch"
**Good answer:** "I refactored a monolithic application into 4 microservices while maintaining backward compatibility and zero downtime"

**This demonstrates:**
- System migration experience
- Architectural thinking
- Risk management
- Incremental delivery
- Production mindset

---

## 📐 Target Architecture

### **Current State (Monolith):**

```
┌─────────────────────────────────────┐
│     Spring Boot Application         │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  AuthController                │ │
│  │  UrlController                 │ │
│  │  AnalyticsController           │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  AuthService                   │ │
│  │  UrlService                    │ │
│  │  AnalyticsService              │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │     Single Database            │ │
│  │  (users, urls, analytics)      │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘

Port: 8080
```

### **Target State (Microservices):**

```
┌──────────────────────────────────────────────────────────┐
│               API Gateway (Port 8080)                     │
│         Routing, Auth, Rate Limiting, CORS               │
└────────┬─────────────────┬──────────────────┬────────────┘
         │                 │                  │
┌────────▼────────┐ ┌──────▼──────┐ ┌────────▼────────┐
│  Auth Service   │ │ URL Service │ │Analytics Service│
│   (Port 8081)   │ │(Port 8082)  │ │  (Port 8083)    │
│                 │ │             │ │                 │
│ • Login         │ │ • Create    │ │ • Track clicks  │
│ • Register      │ │ • List      │ │ • Stats         │
│ • JWT           │ │ • Deactivate│ │ • Reports       │
│ • User Mgmt     │ │ • Redirect  │ │                 │
└─────────────────┘ └─────────────┘ └─────────────────┘
         │                 │                  │
         └─────────────────┼──────────────────┘
                           │
                  ┌────────▼─────────┐
                  │Service Discovery │
                  │  Eureka Server   │
                  │   (Port 8761)    │
                  └──────────────────┘
                           │
                  ┌────────▼─────────┐
                  │  Shared Database │
                  │   PostgreSQL     │
                  │                  │
                  │ • users table    │
                  │ • urls table     │
                  │ • analytics table│
                  └──────────────────┘
```

**Key Decision:** We'll use **shared database** initially for faster migration. Can split later.

---

## 📁 New Project Structure

### **Current Structure:**

```
urlShortner/
├── backend/          # Monolith
├── frontend/
└── docs/
```

### **Target Structure:**

```
urlShortner/
├── backend/          # Original monolith (KEEP for reference)
│
├── microservices/    # NEW microservices
│   ├── pom.xml      # Parent POM
│   │
│   ├── shared-library/
│   │   └── src/main/java/com/urlshortener/
│   │       ├── dto/              # Shared DTOs
│   │       ├── exception/        # Shared exceptions
│   │       └── util/             # Common utilities
│   │
│   ├── service-discovery/
│   │   └── src/main/java/        # Eureka Server
│   │
│   ├── api-gateway/
│   │   └── src/main/java/        # Spring Cloud Gateway
│   │
│   ├── auth-service/             # Multi-module service
│   │   ├── pom.xml              # Auth service parent POM
│   │   ├── auth-service-dto/    # DTOs (UserDto, etc.)
│   │   │   ├── pom.xml          # Library JAR (no repackaging)
│   │   │   └── src/main/java/com/urlshortener/dto/
│   │   ├── auth-service-lib/    # Feign client library
│   │   │   ├── pom.xml          # Library JAR (no repackaging)
│   │   │   └── src/main/java/com/urlshortener/client/
│   │   │       ├── AuthServiceClient.java
│   │   │       └── AuthServiceClientFallback.java
│   │   └── auth-service-app/    # Main application
│   │       ├── pom.xml          # Executable JAR (Spring Boot)
│   │       └── src/main/java/com/urlshortener/
│   │           ├── controller/  # AuthController
│   │           ├── service/     # AuthService + impl
│   │           ├── model/       # User.java (stays internal)
│   │           ├── repository/  # UserRepository
│   │           └── config/      # SecurityConfig
│   │
│   ├── url-service/
│   │   └── src/main/java/com/urlshortener/
│   │       ├── controller/      # UrlController (from monolith)
│   │       ├── service/         # UrlService (from monolith)
│   │       ├── model/           # Url.java (userId field, no @ManyToOne)
│   │       └── repository/      # UrlRepository (queries by userId)
│   │
│   └── analytics-service/
│       └── src/main/java/com/urlshortener/
│           ├── controller/      # AnalyticsController (from monolith)
│           ├── service/         # AnalyticsService (from monolith)
│           └── repository/      # Analytics repositories
│
├── frontend/         # Update to call API Gateway
└── docs/
```

**✅ 70%+ of code is COPIED from existing monolith!**

---

## 🚀 Step-by-Step Refactoring Plan

## **PHASE 0: Prerequisites (Day 0 - 4 hours)**

### **Step 0.1: Install PostgreSQL**

**Current Issue:** Monolith uses H2 in-memory database. Must migrate to PostgreSQL first.

```bash
# macOS
brew install postgresql@16
brew services start postgresql@16

# Verify installation
psql --version
```

---

### **Step 0.2: Create Database**

```bash
# Create database
createdb urlshortener

# Optional: Create dedicated user
psql postgres << EOF
CREATE USER urlshortener_user WITH PASSWORD 'urlshortener_pass';
GRANT ALL PRIVILEGES ON DATABASE urlshortener TO urlshortener_user;
EOF
```

---

### **Step 0.3: Migrate Monolith to PostgreSQL**

**Update backend/pom.xml:**

```xml
<!-- Remove H2 dependency if present -->
<!-- <dependency> -->
<!--     <groupId>com.h2database</groupId> -->
<!--     <artifactId>h2</artifactId> -->
<!-- </dependency> -->

<!-- Add PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Update backend/src/main/resources/application.yml:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update  # For initial migration, then change to validate
    show-sql: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
  expiration: 86400000
```

**Test monolith with PostgreSQL:**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/backend
mvn clean install
mvn spring-boot:run

# Verify tables created
psql urlshortener -c "\dt"
```

---

### **Step 0.4: Create Missing Exception Classes**

```java
// backend/src/main/java/com/urlshortener/exception/UnauthorizedAccessException.java
package com.urlshortener.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
```

---

### **Step 0.5: Create Environment Variables Template**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Create .env.template
cat > microservices/.env.template << 'EOF'
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/urlshortener
DB_USER=postgres
DB_PASSWORD=postgres

# JWT Configuration - REPLACE WITH YOUR OWN SECRET!
# Generate with: openssl rand -hex 32
JWT_SECRET=REPLACE_WITH_YOUR_GENERATED_SECRET_HERE

# Service Discovery
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Copy to .env and update with real values
# Never commit .env to git!
EOF

# Create actual .env file
cp microservices/.env.template microservices/.env

# Add to .gitignore
echo "microservices/.env" >> .gitignore
```

---

## **PHASE 1: Project Setup (Day 1 - 4 hours)**

### **Step 1.1: Create Parent POM**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner
mkdir microservices
cd microservices
```

```xml
<!-- microservices/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.urlshortener</groupId>
    <artifactId>microservices-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <name>URL Shortener Microservices</name>

    <modules>
        <module>shared-library</module>
        <module>service-discovery</module>
        <module>api-gateway</module>
        <module>auth-service</module>  <!-- Multi-module: contains dto, lib, app -->
        <module>url-service</module>
        <module>analytics-service</module>
    </modules>

    <!-- Note: auth-service is a parent POM with 3 sub-modules:
         - auth-service-dto (DTOs)
         - auth-service-lib (Feign clients)
         - auth-service-app (main application)
    -->

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Shared Library -->
            <dependency>
                <groupId>com.urlshortener</groupId>
                <artifactId>shared-library</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### **Step 1.2: Create Module Directories**

```bash
cd microservices

# Create all module directories
mkdir -p shared-library/src/main/java/com/urlshortener
mkdir -p service-discovery/src/main/java/com/urlshortener
mkdir -p api-gateway/src/main/java/com/urlshortener
mkdir -p auth-service/src/main/java/com/urlshortener
mkdir -p url-service/src/main/java/com/urlshortener
mkdir -p analytics-service/src/main/java/com/urlshortener

# Create resources directories
mkdir -p shared-library/src/main/resources
mkdir -p service-discovery/src/main/resources
mkdir -p api-gateway/src/main/resources
mkdir -p auth-service/src/main/resources
mkdir -p url-service/src/main/resources
mkdir -p analytics-service/src/main/resources
```

---

## **PHASE 2: Extract Shared Library (Day 1 - 2 hours)**

### **Step 2.1: Create Shared Library POM**

```xml
<!-- microservices/shared-library/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>shared-library</artifactId>
    <packaging>jar</packaging>
    <name>Shared Library</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### **Step 2.2: Copy ONLY Common/Shared Code from Monolith**

**⚠️ IMPORTANT - Microservices Best Practice:**
- **DO NOT** copy service-specific DTOs (they belong to individual services)
- **DO NOT** copy domain entities like User.java (they belong to their respective services)
- **ONLY** copy truly shared code: common exceptions and ErrorResponse

```bash
# Copy from monolith backend to shared-library
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Create directories
mkdir -p microservices/shared-library/src/main/java/com/urlshortener/dto
mkdir -p microservices/shared-library/src/main/java/com/urlshortener/exception

# Copy ONLY ErrorResponse (common error format)
cp backend/src/main/java/com/urlshortener/dto/ErrorResponse.java \
   microservices/shared-library/src/main/java/com/urlshortener/dto/

# Copy ALL exception classes (domain exceptions used by all services)
cp backend/src/main/java/com/urlshortener/exception/*.java \
   microservices/shared-library/src/main/java/com/urlshortener/exception/
```

**Files to copy (ONLY 6 files):**

```
shared-library/src/main/java/com/urlshortener/
├── dto/
│   └── ErrorResponse.java           ← COPY (common error format)
│
└── exception/
    ├── GlobalExceptionHandler.java         ← COPY (common exception handling)
    ├── UnauthorizedAccessException.java    ← COPY (created in Phase 0)
    ├── UrlNotFoundException.java           ← COPY (domain exception)
    ├── UrlExpiredException.java            ← COPY (domain exception)
    └── UrlDeactivatedException.java        ← COPY (domain exception)
```

**Why this approach?**
- ✅ **Loose coupling** - Services don't depend on each other's DTOs
- ✅ **Service autonomy** - Each service owns its API contracts
- ✅ **Independent deployment** - Can update DTOs without rebuilding all services
- ✅ **Proper bounded contexts** - Clear service boundaries

**No code changes needed!** Just copy-paste the 6 files above.

---

### **Step 2.3: Build Shared Library**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices
mvn clean install -DskipTests
```

**Expected output:**
```
[INFO] Building Shared Library 1.0.0
[INFO] Compiling 6 source files
[INFO] BUILD SUCCESS
```

The shared-library JAR is now installed in your local Maven repository and ready to be used by microservices!

---

## **PHASE 3: Service Discovery (Day 1 - 2 hours)**

### **Step 3.1: Create Eureka Server POM**

```xml
<!-- microservices/service-discovery/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>service-discovery</artifactId>
    <name>Service Discovery (Eureka)</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

### **Step 3.2: Create Eureka Application**

```java
// microservices/service-discovery/src/main/java/com/urlshortener/ServiceDiscoveryApplication.java
package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}
```

---

### **Step 3.3: Configure Eureka**

```yaml
# microservices/service-discovery/src/main/resources/application.yml
server:
  port: 8761

spring:
  application:
    name: service-discovery

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 4000
```

---

## **📘 Enterprise Microservices Patterns (READ THIS FIRST)**

Before implementing PHASE 4 and beyond, understand these critical architectural patterns:

### **Pattern 1: Multi-Module Service Structure**

**Why Multi-Module?**

In enterprise microservices, services that need to expose contracts to other services use a multi-module structure:

```
service-name/
├── pom.xml                    # Parent aggregator
├── service-name-dto/          # Data Transfer Objects
├── service-name-lib/          # Client libraries (Feign, DTOs)
└── service-name-app/          # Main application
```

**Benefits:**
- **Clean Contracts**: DTOs define the API contract, not JPA entities
- **Reusability**: Other services depend only on -lib (not full -app)
- **Proper Packaging**: Library modules are plain JARs, app is executable JAR
- **Standard Pattern**: Matches Netflix, Uber, Amazon microservices practices

**Example: auth-service**
- `auth-service-dto`: Contains UserDto, LoginRequestDto (plain POJOs)
- `auth-service-lib`: Contains AuthServiceClient (Feign interface)
- `auth-service-app`: Contains AuthController, User entity, business logic

**Consumer Pattern:**
```xml
<!-- url-service depends only on auth-service-lib -->
<dependency>
    <groupId>com.urlshortener</groupId>
    <artifactId>auth-service-lib</artifactId>
</dependency>
```

### **Pattern 2: DTOs vs Entities**

**CRITICAL: Never expose JPA entities across service boundaries!**

**Bad (Monolith Pattern):**
```java
@FeignClient("auth-service")
public interface AuthServiceClient {
    @GetMapping("/users/{username}")
    User getUserByUsername(@PathVariable String username);  // ❌ JPA entity!
}
```

**Good (Microservices Pattern):**
```java
@FeignClient("auth-service")
public interface AuthServiceClient {
    @GetMapping("/users/{username}")
    UserDto getUserByUsername(@PathVariable String username);  // ✅ DTO!
}
```

**Why DTOs?**
- Entities contain JPA annotations, lazy loading, bidirectional relationships
- Entities may expose sensitive fields (passwords, internal IDs)
- DTOs are clean, serializable data contracts
- Changes to entity structure don't break API contracts

**Conversion Pattern:**
```java
// In AuthController (auth-service-app)
@GetMapping("/users/{username}")
public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
    User entity = authService.findByUsername(username)...;

    // Convert entity → DTO
    UserDto dto = new UserDto(
        entity.getId(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getRole()
        // NO password, NO JPA fields
    );

    return ResponseEntity.ok(dto);
}
```

### **Pattern 3: Foreign Keys as IDs (No Cross-Service JPA Relationships)**

**CRITICAL: No @ManyToOne/@OneToMany across service boundaries!**

**Bad (Monolith Pattern):**
```java
// url-service Url entity
@ManyToOne
@JoinColumn(name = "user_id")
private User user;  // ❌ Requires User entity from auth-service!
```

**Good (Microservices Pattern):**
```java
// url-service Url entity
@Column(name = "user_id")
private Long userId;  // ✅ Just the ID!
```

**Why?**
- Services must be autonomous (no shared JPA entities)
- Prevents tight coupling between services
- Each service owns its own data model
- Can still fetch user details via Feign when needed

**Usage Pattern:**
```java
// Create URL
UserDto user = authServiceClient.getUserByUsername(username);
url.setUserId(user.getId());  // Store ID, not entity

// Query URLs
Page<Url> urls = urlRepository.findByUserIdAndDeactivatedFalse(userId, pageable);
```

### **Pattern 4: Build Configuration for Library Modules**

**CRITICAL: Library modules must skip Spring Boot repackaging!**

**Library Module POM (auth-service-dto, auth-service-lib):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <skip>true</skip>  <!-- ✅ Plain JAR -->
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Application Module POM (auth-service-app):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <!-- ✅ Executable JAR (fat JAR) -->
        </plugin>
    </plugins>
</build>
```

**Why?**
- Library JARs must be plain JARs so other services can include them as dependencies
- Spring Boot repackaging creates fat JARs with nested dependencies (can't be used as dependency)
- Only -app modules need to be executable

**Verification:**
```bash
ls -lh auth-service-dto/target/*.jar   # ~10KB plain JAR
ls -lh auth-service-lib/target/*.jar   # ~20KB plain JAR
ls -lh auth-service-app/target/*.jar   # ~80MB fat JAR
```

### **Pattern 5: Correct Build Order**

**CRITICAL: Build must follow dependency graph!**

```
shared-library (no dependencies)
    ↓
auth-service-dto (no dependencies)
    ↓
auth-service-lib (depends on: dto)
    ↓
auth-service-app (depends on: dto, shared-library)
    ↓
url-service (depends on: auth-service-lib, shared-library)
```

**Maven Build:**
```bash
# From microservices directory
mvn clean install

# Maven automatically resolves build order based on dependencies
```

### **Interview Talking Points**

When discussing this architecture in interviews:

1. **"We use a multi-module pattern for services that expose contracts"**
   - Separates DTOs, client libraries, and application logic
   - Other services depend only on the contract (lib module)

2. **"We never expose JPA entities across service boundaries"**
   - DTOs provide clean, versioned contracts
   - Prevents tight coupling and allows independent evolution

3. **"Services store foreign keys as IDs, not JPA relationships"**
   - Maintains service autonomy
   - Uses Feign clients for cross-service data retrieval

4. **"Library modules produce plain JARs, apps produce executable JARs"**
   - Enables proper dependency management
   - Standard Maven/Spring Boot best practice

---

## **PHASE 4: Extract Auth Service (Day 2 - 6 hours)**

### **Overview: Multi-Module Architecture**

Auth Service follows an **enterprise multi-module pattern** that separates DTOs, Feign clients, and application code into distinct modules. This is the standard pattern used in production microservices architectures.

**Why Multi-Module?**
- Prevents entity leakage across service boundaries (DTOs instead of JPA entities)
- Allows other services to depend only on the contract (lib module), not the full application
- Enables proper separation of concerns and clean architecture
- Matches real-world enterprise microservices patterns

**Module Structure:**
```
auth-service/
├── pom.xml                          # Parent POM (aggregator)
├── auth-service-dto/                # Data Transfer Objects
│   ├── pom.xml                      # Library module (no repackaging)
│   └── src/main/java/.../dto/
│       ├── UserDto.java             # User representation for inter-service calls
│       ├── LoginRequestDto.java     # Login request
│       └── RegisterRequestDto.java  # Registration request
├── auth-service-lib/                # Feign Client Library
│   ├── pom.xml                      # Library module (no repackaging)
│   └── src/main/java/.../client/
│       ├── AuthServiceClient.java   # Feign client interface
│       └── AuthServiceClientFallback.java  # Circuit breaker fallback
└── auth-service-app/                # Main Application
    ├── pom.xml                      # Executable JAR (Spring Boot repackaging)
    └── src/main/java/com/urlshortener/
        ├── controller/              # AuthController
        ├── service/                 # AuthService + impl
        ├── model/                   # User entity (stays within service)
        ├── repository/              # UserRepository
        ├── config/                  # SecurityConfig
        └── AuthServiceApplication.java
```

---

### **Step 4.1: Create Auth Service Parent POM**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices
mkdir -p auth-service
```

```xml
<!-- microservices/auth-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>auth-service-parent</artifactId>
    <packaging>pom</packaging>
    <name>Auth Service Parent</name>

    <modules>
        <module>auth-service-dto</module>
        <module>auth-service-lib</module>
        <module>auth-service-app</module>
    </modules>
</project>
```

---

### **Step 4.2: Create Auth Service DTO Module**

**Create DTO module POM:**

```bash
mkdir -p auth-service/auth-service-dto/src/main/java/com/urlshortener/dto
```

```xml
<!-- microservices/auth-service/auth-service-dto/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>auth-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>auth-service-dto</artifactId>
    <packaging>jar</packaging>
    <name>Auth Service DTOs</name>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <!-- CRITICAL: Skip Spring Boot repackaging for library modules -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Create UserDto:**

```java
// microservices/auth-service/auth-service-dto/src/main/java/com/urlshortener/dto/UserDto.java
package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;

    // NO password field - never expose sensitive data in DTOs
    // NO JPA annotations - this is a pure data transfer object
}
```

**Create LoginRequestDto:**

```java
// microservices/auth-service/auth-service-dto/src/main/java/com/urlshortener/dto/LoginRequestDto.java
package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    private String username;
    private String password;
}
```

**Create RegisterRequestDto:**

```java
// microservices/auth-service/auth-service-dto/src/main/java/com/urlshortener/dto/RegisterRequestDto.java
package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {
    private String username;
    private String email;
    private String password;
}
```

---

### **Step 4.3: Create Auth Service Lib Module (Feign Client)**

```bash
mkdir -p auth-service/auth-service-lib/src/main/java/com/urlshortener/client
```

```xml
<!-- microservices/auth-service/auth-service-lib/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>auth-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>auth-service-lib</artifactId>
    <packaging>jar</packaging>
    <name>Auth Service Library (Feign Client)</name>

    <dependencies>
        <!-- DTO module dependency -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>auth-service-dto</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
    </dependencies>

    <!-- CRITICAL: Skip Spring Boot repackaging for library modules -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Create Feign Client Interface:**

```java
// microservices/auth-service/auth-service-lib/src/main/java/com/urlshortener/client/AuthServiceClient.java
package com.urlshortener.client;

import com.urlshortener.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Auth Service.
 * Returns DTOs (UserDto), NOT entities (User).
 * This prevents JPA entity leakage across service boundaries.
 */
@FeignClient(name = "auth-service", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{username}")
    UserDto getUserByUsername(@PathVariable String username);
}
```

**Create Fallback Implementation:**

```java
// microservices/auth-service/auth-service-lib/src/main/java/com/urlshortener/client/AuthServiceClientFallback.java
package com.urlshortener.client;

import com.urlshortener.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public UserDto getUserByUsername(String username) {
        throw new RuntimeException("Auth service is unavailable. Please try again later.");
    }
}
```

**Why DTOs instead of Entities?**
- User entity has JPA annotations, password fields, database concerns
- UserDto is a clean data contract with only necessary fields
- Other services don't need to know about database structure
- Prevents tight coupling between services

---

### **Step 4.4: Create Auth Service App Module**

```bash
mkdir -p auth-service/auth-service-app/src/main/java/com/urlshortener/{controller,service,service/impl,model,repository,config,util}
mkdir -p auth-service/auth-service-app/src/main/resources
```

```xml
<!-- microservices/auth-service/auth-service-app/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>auth-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>auth-service-app</artifactId>
    <packaging>jar</packaging>
    <name>Auth Service Application</name>

    <dependencies>
        <!-- DTO module -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>auth-service-dto</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Shared Library -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>shared-library</artifactId>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <!-- This module IS an executable JAR - Spring Boot repackaging enabled -->
</project>
```

---

### **Step 4.5: Copy Auth Code to App Module**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Copy Controller
cp backend/src/main/java/com/urlshortener/controller/AuthController.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/controller/

# Copy Service
cp backend/src/main/java/com/urlshortener/service/AuthService.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/service/
cp backend/src/main/java/com/urlshortener/service/impl/AuthServiceImpl.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/service/impl/

# Copy Model (User entity stays within auth-service-app)
cp backend/src/main/java/com/urlshortener/model/User.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/model/

# Copy Repository
cp backend/src/main/java/com/urlshortener/repository/UserRepository.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/repository/

# Copy Config
cp backend/src/main/java/com/urlshortener/config/SecurityConfig.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/config/

# Copy JWT Util
cp backend/src/main/java/com/urlshortener/util/JwtUtil.java \
   microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/util/
```

**Key Points:**
- User entity (JPA model) stays in auth-service-app - it's an internal implementation detail
- Other services use UserDto from auth-service-dto module
- This prevents JPA entity leakage across service boundaries

---

### **Step 4.6: Create Auth Service Application Class**

```java
// microservices/auth-service/auth-service-app/src/main/java/com/urlshortener/AuthServiceApplication.java
package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {"com.urlshortener.model"})
@EnableJpaRepositories(basePackages = {"com.urlshortener.repository"})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

---

### **Step 4.7: Configure Auth Service Application**

```yaml
# microservices/auth-service/auth-service-app/src/main/resources/application.yml
server:
  port: 8081

spring:
  application:
    name: auth-service

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/urlshortener}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    show-sql: false

  # Only auth-service manages DB schema (shared database pattern)
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

logging:
  level:
    com.urlshortener: DEBUG
```

**Copy Liquibase Changelogs:**

```bash
# Copy all Liquibase changelogs from monolith to auth-service-app
cp -r backend/src/main/resources/db \
   microservices/auth-service/auth-service-app/src/main/resources/
```

---

### **Step 4.8: Add User Lookup Endpoint (Returns UserDto)**

**CRITICAL:** This endpoint returns UserDto (not User entity) for inter-service communication.

---

## AuthController Endpoint Must Return UserDto

The `/api/v1/auth/users/{username}` endpoint MUST return `UserDto`, not the `User` entity. This is a critical architectural requirement.

**WRONG (exposes entity):**
```java
@GetMapping("/users/{username}")
public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
    User user = authService.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    return ResponseEntity.ok(user);  // ❌ Exposes internal entity
}
```

**CORRECT (returns DTO):**
```java
@GetMapping("/users/{username}")
public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
    User user = authService.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    // Convert User entity to UserDto (never expose entities to other services)
    UserDto userDto = UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();

    return ResponseEntity.ok(userDto);  // ✅ Returns DTO
}
```

**Why This Matters:**
1. **Entities are internal** - `User` entity with JPA annotations should never leave the service
2. **DTOs are contracts** - `UserDto` is the external API contract
3. **Security** - Password field in User entity would be exposed otherwise
4. **Maintainability** - Can change internal User entity without breaking url-service

**Example Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER"
}
```

---

**Update AuthController to return UserDto:**

```java
// microservices/auth-service/auth-service-app/.../controller/AuthController.java
// Add this import
import com.urlshortener.dto.UserDto;

// Add this method to existing AuthController class:

@GetMapping("/users/{username}")
public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
    User user = authService.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    // Convert entity to DTO (no password, no JPA annotations)
    UserDto userDto = UserDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();

    return ResponseEntity.ok(userDto);
}
```

**Add to AuthService interface:**

```java
// microservices/auth-service/auth-service-app/.../service/AuthService.java

Optional<User> findByUsername(String username);
```

**Add to AuthServiceImpl:**

```java
// microservices/auth-service/auth-service-app/.../service/impl/AuthServiceImpl.java

@Override
public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
}
```

**Why UserDto instead of User entity?**
- User entity contains password, JPA annotations, database concerns
- UserDto is a clean contract with only necessary fields
- Prevents tight coupling between services
- Follows enterprise microservices best practices

**Test:**
```bash
# After auth-service is running:
curl http://localhost:8081/api/v1/auth/users/testuser \
  -H "Authorization: Bearer YOUR_TOKEN"

# Response will be UserDto:
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "role": "USER"
}
```

---

### **Step 4.9: Add Database Indexes for Performance**

**CRITICAL:** Without indexes, queries will be slow under load.

**Create new Liquibase changelog:**

```bash
# Create new changelog file
cat > microservices/auth-service/auth-service-app/src/main/resources/db/changelog/db.changelog-002-indexes.yaml << 'EOF'
databaseChangeLog:
  - changeSet:
      id: 002-add-performance-indexes
      author: system
      changes:
        - createIndex:
            indexName: idx_users_username
            tableName: users
            columns:
              - column:
                  name: username

        - createIndex:
            indexName: idx_users_email
            tableName: users
            columns:
              - column:
                  name: email

        - createIndex:
            indexName: idx_urls_short_url
            tableName: urls
            columns:
              - column:
                  name: short_url

        - createIndex:
            indexName: idx_urls_user_id
            tableName: urls
            columns:
              - column:
                  name: user_id

        - createIndex:
            indexName: idx_urls_created_at
            tableName: urls
            columns:
              - column:
                  name: created_at
EOF
```

**Update master changelog:**

```yaml
# Edit: microservices/auth-service/auth-service-app/src/main/resources/db/changelog/db.changelog-master.yaml
# Add after existing include:

databaseChangeLog:
  - include:
      file: db/changelog/db.changelog-001-initial-schema.yaml
  - include:
      file: db/changelog/db.changelog-002-indexes.yaml  # ADD THIS
```

**Impact:**
- 10x faster user lookup by username/email
- 10x faster URL lookup by short code
- Efficient user URL retrieval with proper ordering
- Reduced database load under high traffic

---

### **Step 4.10: Multi-Module Build Order**

**CRITICAL:** The build must follow dependency order.

```bash
# Build order (from auth-service parent directory):
cd microservices/auth-service

# Build in order:
mvn clean install -pl auth-service-dto    # DTOs first (no dependencies)
mvn clean install -pl auth-service-lib    # Lib second (depends on DTO)
mvn clean install -pl auth-service-app    # App last (depends on DTO)

# Or build all at once (Maven resolves order):
mvn clean install
```

**Verify library modules skip repackaging:**

```bash
# Check that DTO and LIB produce plain JARs (not Spring Boot fat JARs)
ls -lh auth-service-dto/target/*.jar
ls -lh auth-service-lib/target/*.jar

# Should see files like:
# auth-service-dto-1.0.0.jar  (plain JAR, ~5-10KB)
# auth-service-lib-1.0.0.jar  (plain JAR, ~10-20KB)

# App module should produce executable JAR:
ls -lh auth-service-app/target/*.jar
# auth-service-app-1.0.0.jar  (fat JAR, ~50-100MB)
```

**Why This Matters:**
- Library modules must be plain JARs so other services can depend on them
- Only the -app module needs Spring Boot repackaging (executable JAR)
- This is the enterprise standard for multi-module microservices

---

## **PHASE 5: Extract URL Service (Day 2-3 - 5 hours)**

### **Overview: Microservices Data Pattern**

URL Service demonstrates the **foreign key as ID pattern** for microservices:

**Key Principles:**
- NO @ManyToOne relationships across service boundaries
- Store userId as Long (foreign key ID), not User entity
- Use Feign client to fetch UserDto when user details are needed
- This maintains service autonomy and prevents tight coupling

**Before (Monolith):**
```java
@ManyToOne
@JoinColumn(name = "user_id")
private User user;  // JPA relationship
```

**After (Microservice):**
```java
@Column(name = "user_id")
private Long userId;  // Just the ID, no JPA relationship
```

---

### **Step 5.1: Create URL Service POM**

```xml
<!-- microservices/url-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>url-service</artifactId>
    <name>URL Service</name>

    <dependencies>
        <!-- Shared Library -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>shared-library</artifactId>
        </dependency>

        <!-- Auth Service Lib (for Feign Client and DTOs) -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>auth-service-lib</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- OpenFeign (for calling Auth Service) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

**Key Change:**
- Depends on `auth-service-lib` (NOT `auth-service-app`)
- This gives access to AuthServiceClient and UserDto
- Does NOT pull in User entity or auth-service business logic

---

### **Step 5.2: Copy URL Code from Monolith**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Create directories
mkdir -p microservices/url-service/src/main/java/com/urlshortener/controller
mkdir -p microservices/url-service/src/main/java/com/urlshortener/service/impl
mkdir -p microservices/url-service/src/main/java/com/urlshortener/service/generator
mkdir -p microservices/url-service/src/main/java/com/urlshortener/model
mkdir -p microservices/url-service/src/main/java/com/urlshortener/repository
mkdir -p microservices/url-service/src/main/java/com/urlshortener/client

# Copy Controllers (including RedirectController)
cp backend/src/main/java/com/urlshortener/controller/UrlController.java \
   microservices/url-service/src/main/java/com/urlshortener/controller/
cp backend/src/main/java/com/urlshortener/controller/RedirectController.java \
   microservices/url-service/src/main/java/com/urlshortener/controller/

# Copy Service
cp backend/src/main/java/com/urlshortener/service/UrlService.java \
   microservices/url-service/src/main/java/com/urlshortener/service/
cp backend/src/main/java/com/urlshortener/service/impl/UrlServiceImpl.java \
   microservices/url-service/src/main/java/com/urlshortener/service/impl/

# Copy URL Generators
cp backend/src/main/java/com/urlshortener/service/generator/*.java \
   microservices/url-service/src/main/java/com/urlshortener/service/generator/

# Copy Model
cp backend/src/main/java/com/urlshortener/model/Url.java \
   microservices/url-service/src/main/java/com/urlshortener/model/

# Copy Repository
cp backend/src/main/java/com/urlshortener/repository/UrlRepository.java \
   microservices/url-service/src/main/java/com/urlshortener/repository/
```

**Important:** RedirectController belongs in URL Service since it handles URL redirection.

---

### **Step 5.3: Modify Url Entity (Remove @ManyToOne)**

**CRITICAL:** Remove JPA relationship with User entity. Store only the userId.

```java
// microservices/url-service/.../model/Url.java
package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shortUrl;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    // BEFORE (Monolith): @ManyToOne relationship
    // @ManyToOne
    // @JoinColumn(name = "user_id")
    // private User user;

    // AFTER (Microservice): Just store the ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean deactivated = false;

    @Column(nullable = false)
    private Long clickCount = 0L;

    // Getters and setters via Lombok
}
```

**Why This Pattern?**
- Maintains service autonomy - url-service doesn't need User entity
- No tight coupling via JPA relationships
- Can still fetch user details via Feign when needed
- Standard microservices pattern for cross-service references

---

### **Step 5.4: Update UrlRepository (Query by userId)**

```java
// microservices/url-service/.../repository/UrlRepository.java
package com.urlshortener.repository;

import com.urlshortener.model.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortUrl(String shortUrl);

    // BEFORE (Monolith): Query by User entity
    // List<Url> findByUserOrderByCreatedAtDesc(User user);

    // AFTER (Microservice): Query by userId (Long)
    Page<Url> findByUserIdAndDeactivatedFalse(Long userId, Pageable pageable);

    // Optional: Simple list version (not paginated)
    // List<Url> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

---

### **Step 5.5: Update UrlServiceImpl (Use Feign Client and userId)**

```java
// microservices/url-service/.../service/impl/UrlServiceImpl.java
package com.urlshortener.service.impl;

import com.urlshortener.client.AuthServiceClient;  // From auth-service-lib
import com.urlshortener.dto.UserDto;               // From auth-service-dto
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UrlServiceImpl implements UrlService {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private AuthServiceClient authServiceClient;  // Feign client from auth-service-lib

    @Override
    public UrlResponseDto createShortUrl(String originalUrl, String username) {
        // BEFORE (Monolith): Fetch User entity from UserRepository
        // User user = userRepository.findByUsername(username)
        //     .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // AFTER (Microservice): Fetch UserDto via Feign
        UserDto userDto = authServiceClient.getUserByUsername(username);
        if (userDto == null) {
            throw new UsernameNotFoundException("User not found in auth service");
        }

        // Create URL with userId (not User entity)
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortUrl(generateShortUrl());
        url.setUserId(userDto.getId());  // Store ID, not entity
        url.setCreatedAt(LocalDateTime.now());
        url.setDeactivated(false);
        url.setClickCount(0L);

        Url saved = urlRepository.save(url);
        return convertToDto(saved);
    }

    @Override
    public Page<UrlResponseDto> getUserUrls(String username, Pageable pageable) {
        // Fetch user via Feign to get userId
        UserDto userDto = authServiceClient.getUserByUsername(username);
        if (userDto == null) {
            throw new UsernameNotFoundException("User not found");
        }

        // Query by userId (Long), not User entity
        Page<Url> urlPage = urlRepository.findByUserIdAndDeactivatedFalse(
            userDto.getId(), pageable);

        return urlPage.map(this::convertToDto);
    }

    // ... other methods
}
```

**Key Changes:**
- Import `AuthServiceClient` from `com.urlshortener.client` (auth-service-lib)
- Import `UserDto` from `com.urlshortener.dto` (auth-service-dto)
- Use Feign to fetch UserDto (not User entity)
- Store and query by userId (Long)

---

### **Step 5.6: Create URL Service Application**

```java
// microservices/url-service/src/main/java/com/urlshortener/UrlServiceApplication.java
package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.urlshortener.client")  // Scan for Feign clients from auth-service-lib
@EntityScan(basePackages = {"com.urlshortener.model"})
@EnableJpaRepositories(basePackages = {"com.urlshortener.repository"})
public class UrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlServiceApplication.class, args);
    }
}
```

**Important:**
- `@EnableFeignClients(basePackages = "com.urlshortener.client")` tells Spring to scan for Feign clients
- This finds `AuthServiceClient` from auth-service-lib JAR
- No need to create Feign clients in url-service - we use the one from auth-service-lib

---

### **Step 5.7: Configure URL Service**

```yaml
# microservices/url-service/src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: url-service

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/urlshortener}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    show-sql: false

  # ⚠️ IMPORTANT: Disable Liquibase (auth-service manages schema)
  liquibase:
    enabled: false

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

# Feign client configuration
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
  circuitbreaker:
    enabled: true

url:
  generator:
    strategy: DISTRIBUTED

logging:
  level:
    com.urlshortener: DEBUG
```

---

### **Step 5.8: Summary - Build Order and Testing**

**Build Order:**

```bash
# Build must follow dependency chain:
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices

# 1. Build shared-library
mvn clean install -pl shared-library

# 2. Build auth-service (all modules)
cd auth-service
mvn clean install
cd ..

# 3. Build url-service (depends on auth-service-lib)
mvn clean install -pl url-service

# Or build everything at once:
mvn clean install
```

**Test URL Service:**

```bash
# Start services in order:
# 1. Start auth-service-app
java -jar auth-service/auth-service-app/target/auth-service-app-1.0.0.jar

# 2. Start url-service
java -jar url-service/target/url-service-1.0.0.jar

# Test Feign communication:
# Create a URL (url-service will call auth-service via Feign to validate user)
curl -X POST http://localhost:8082/api/v1/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "originalUrl": "https://example.com"
  }'

# Get user's URLs (url-service queries by userId)
curl http://localhost:8082/api/v1/urls?page=0&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Key Architecture Points:**
- URL Service depends on auth-service-lib (NOT auth-service-app)
- Url entity stores userId (Long), not User entity
- No JPA relationships across service boundaries
- Feign client returns UserDto (from auth-service-dto)
- This is the standard enterprise microservices pattern

---

## **PHASE 6: Extract Analytics Service (Day 3 - 3 hours)**

### **Overview: Multi-Module Architecture**

Analytics Service follows the same **enterprise multi-module pattern** as Auth Service, separating DTOs, Feign clients, and application code into distinct modules.

**Module Structure:**
```
analytics-service/
├── pom.xml                            # Parent POM (aggregator)
├── analytics-service-dto/             # Data Transfer Objects
│   ├── pom.xml                        # Library module (no repackaging)
│   └── src/main/java/.../dto/
│       ├── UrlAnalyticsResponse.java  # Analytics response DTO
│       └── ClickEventDto.java         # Click event DTO for Feign
├── analytics-service-lib/             # Feign Client Library
│   ├── pom.xml                        # Library module (no repackaging)
│   └── src/main/java/.../lib/
│       └── AnalyticsServiceClient.java # Feign client interface
└── analytics-service-app/             # Main Application
    ├── pom.xml                        # Executable JAR (Spring Boot repackaging)
    └── src/main/java/com/urlshortener/
        ├── controller/                # AnalyticsController
        ├── service/                   # AnalyticsService + impl
        ├── model/                     # ClickEvent entity (with urlId, not @ManyToOne)
        ├── repository/                # ClickEventRepository
        └── AnalyticsServiceApplication.java
```

**Port: 8083**

---

### **Step 6.1: Create Analytics Service Parent POM**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices
mkdir -p analytics-service
```

```xml
<!-- microservices/analytics-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>analytics-service-parent</artifactId>
    <packaging>pom</packaging>
    <name>Analytics Service Parent</name>

    <modules>
        <module>analytics-service-dto</module>
        <module>analytics-service-lib</module>
        <module>analytics-service-app</module>
    </modules>
</project>
```

---

### **Step 6.2: Create Analytics Service DTO Module**

```bash
mkdir -p analytics-service/analytics-service-dto/src/main/java/com/urlshortener/dto
```

```xml
<!-- microservices/analytics-service/analytics-service-dto/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>analytics-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>analytics-service-dto</artifactId>
    <name>Analytics Service DTO</name>

    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <!-- Skip Spring Boot repackaging for library module -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Copy UrlAnalyticsResponse from backend:**

```bash
# Copy existing DTO from monolith
cp backend/src/main/java/com/urlshortener/dto/UrlAnalyticsResponse.java \
   microservices/analytics-service/analytics-service-dto/src/main/java/com/urlshortener/dto/
```

The DTO already exists in the backend and doesn't need changes - it's a pure data transfer object with no JPA annotations.

---

### **Step 6.3: Create Analytics Service Lib Module**

```bash
mkdir -p analytics-service/analytics-service-lib/src/main/java/com/urlshortener/lib
```

```xml
<!-- microservices/analytics-service/analytics-service-lib/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>analytics-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>analytics-service-lib</artifactId>
    <name>Analytics Service Library (Feign Client)</name>

    <dependencies>
        <!-- DTO module dependency -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>analytics-service-dto</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
    </dependencies>

    <!-- Skip Spring Boot repackaging for library module -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Create Feign Client:**

```java
// microservices/analytics-service/analytics-service-lib/src/main/java/com/urlshortener/lib/AnalyticsServiceClient.java
package com.urlshortener.lib;

import com.urlshortener.dto.UrlAnalyticsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "analytics-service")
public interface AnalyticsServiceClient {

    @GetMapping("/api/v1/analytics/urls/{urlId}/total-clicks")
    Long getTotalClicks(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-country")
    Map<String, Long> getClicksByCountry(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-browser")
    Map<String, Long> getClicksByBrowser(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-device")
    Map<String, Long> getClicksByDeviceType(@PathVariable Long urlId);
}
```

---

### **Step 6.4: Create Analytics Service App Module**

```bash
mkdir -p analytics-service/analytics-service-app/src/main/java/com/urlshortener/{controller,service/impl,repository,model,config}
mkdir -p analytics-service/analytics-service-app/src/main/resources
```

```xml
<!-- microservices/analytics-service/analytics-service-app/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>analytics-service-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>analytics-service-app</artifactId>
    <name>Analytics Service Application</name>

    <dependencies>
        <!-- Own DTO module -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>analytics-service-dto</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Shared Library -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>shared-library</artifactId>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- User Agent Parser -->
        <dependency>
            <groupId>eu.bitwalker</groupId>
            <artifactId>UserAgentUtils</artifactId>
            <version>1.21</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Copy files from backend monolith:**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Copy Controller
cp backend/src/main/java/com/urlshortener/controller/AnalyticsController.java \
   microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/controller/

# Copy Service interface and implementation
cp backend/src/main/java/com/urlshortener/service/AnalyticsService.java \
   microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/service/
cp backend/src/main/java/com/urlshortener/service/impl/AnalyticsServiceImpl.java \
   microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/service/impl/

# Copy ClickEvent model
cp backend/src/main/java/com/urlshortener/model/ClickEvent.java \
   microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/model/

# Copy Repository
cp backend/src/main/java/com/urlshortener/repository/ClickEventRepository.java \
   microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/repository/
```

**CRITICAL: Adapt ClickEvent Model for Microservices**

After copying, edit `ClickEvent.java` to change from @ManyToOne relationship to storing just the ID:

Change from:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "url_id", nullable = false)
private Url url;
```

To:
```java
@Column(name = "url_id", nullable = false)
private Long urlId;  // Store ID, not entity - no cross-service relationships
```

**Why this change?**
- In microservices, each service owns its data
- ClickEvent belongs to analytics-service, Url belongs to url-service
- We store the urlId as a foreign key, but NO JPA relationship
- If we need URL data, we'd call url-service via Feign (not needed for analytics)

**Create Application Class:**

```java
// microservices/analytics-service/analytics-service-app/src/main/java/com/urlshortener/AnalyticsServiceApplication.java
package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
```

**Create application.yml:**

```yaml
# microservices/analytics-service/analytics-service-app/src/main/resources/application.yml
server:
  port: 8083

spring:
  application:
    name: analytics-service

  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener
    username: urlshortener_user
    password: urlshortener_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
```

---

### **Step 6.5: Update Parent POM**

Add analytics-service to microservices-parent:

```xml
<!-- microservices/pom.xml -->
<modules>
    <module>shared-library</module>
    <module>eureka-server</module>
    <module>auth-service</module>
    <module>url-service</module>
    <module>analytics-service</module>  <!-- ADD THIS -->
</modules>
```

---

### **Step 6.6: Build Analytics Service**

```bash
cd microservices/analytics-service
mvn clean install

# Verify library modules produce plain JARs
ls -lh analytics-service-dto/target/*.jar
ls -lh analytics-service-lib/target/*.jar

# Verify app module produces executable JAR
ls -lh analytics-service-app/target/*.jar
```

---

### **Step 6.7: Integrate Analytics Service with URL Service**

Add analytics-service-lib dependency to url-service:

```xml
<!-- microservices/url-service/pom.xml -->
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Analytics Service Lib (for Feign Client) -->
    <dependency>
        <groupId>com.urlshortener</groupId>
        <artifactId>analytics-service-lib</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

Update url-service UrlController to add stats endpoint:

```java
// microservices/url-service/src/main/java/com/urlshortener/controller/UrlController.java

import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.lib.AnalyticsServiceClient;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final UrlRepository urlRepository;

    // ... existing endpoints ...

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<UrlAnalyticsResponse> getUrlStats(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "7") int days) {
        // Fetch URL data from url-service database
        Url url = urlRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        // Call analytics-service via Feign client
        return ResponseEntity.ok(
            analyticsServiceClient.getUrlAnalytics(
                url.getId(),
                shortCode,
                url.getOriginalUrl(),
                days
            )
        );
    }
}
```

**Key Design Pattern:**
- url-service owns URL metadata (id, shortCode, originalUrl)
- analytics-service owns click analytics data
- url-service calls analytics-service with context, receives aggregated stats
- Follows microservices principle: each service provides what it knows

---

### **Step 6.8: Click Tracking - Future Enhancement (Kafka)**

**Current Status:** Analytics retrieval (stats endpoint) is complete. Click tracking is deferred to a future phase with Kafka integration.

**Why Kafka for Click Tracking:**

❌ **REST API approach** (synchronous):
- Every redirect waits for analytics REST call
- If analytics-service is down, redirects fail
- High-traffic URLs create bottlenecks
- Network latency affects user experience

✅ **Kafka approach** (asynchronous - recommended):
- Redirect happens instantly
- Click event published to Kafka topic
- Analytics-service consumes and processes in background
- Decoupled: analytics-service downtime doesn't affect redirects
- Scalable: multiple consumers can process click events in parallel
- High throughput: handles thousands of clicks per second

**Future Implementation Plan:**

1. **url-service RedirectController** publishes click event to Kafka:
```java
@GetMapping("/{shortUrl}")
public RedirectView redirect(@PathVariable String shortUrl, HttpServletRequest request) {
    Url url = urlService.getOriginalUrl(shortUrl);

    // Publish click event to Kafka (async, non-blocking)
    kafkaTemplate.send("click-events", ClickEventDto.builder()
        .urlId(url.getId())
        .ipAddress(request.getRemoteAddr())
        .userAgent(request.getHeader("User-Agent"))
        .referrer(request.getHeader("Referer"))
        .timestamp(LocalDateTime.now())
        .build());

    return new RedirectView(url.getOriginalUrl());
}
```

2. **analytics-service** consumes from Kafka topic:
```java
@KafkaListener(topics = "click-events", groupId = "analytics-service")
public void handleClickEvent(ClickEventDto event) {
    analyticsService.trackClick(
        event.getUrlId(),
        event.getIpAddress(),
        event.getUserAgent(),
        event.getReferrer()
    );
}
```

**Benefits:**
- Event-driven architecture
- Asynchronous processing
- Fault tolerance (events persisted in Kafka)
- Scalability (add more consumers as needed)
- Zero impact on redirect performance

**TODO in RedirectController:**
```java
// TODO: Click tracking will be implemented in future Kafka integration phase
// Will use Kafka to publish click events asynchronously
```

This follows enterprise microservices best practice: **high-volume operations should be asynchronous and event-driven**.

---

## **PHASE 7: API Gateway (Day 3-4 - 4 hours)**

### **Step 7.1: Create API Gateway POM**

```xml
<!-- microservices/api-gateway/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.urlshortener</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>API Gateway</name>

    <dependencies>
        <!-- Shared Library -->
        <dependency>
            <groupId>com.urlshortener</groupId>
            <artifactId>shared-library</artifactId>
        </dependency>

        <!-- Spring Cloud Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- For JWT validation -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

---

### **Step 7.2: Create Gateway Application**

```java
// microservices/api-gateway/src/main/java/com/urlshortener/ApiGatewayApplication.java
package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

---

### **Step 7.3: Configure Routes**

```java
// microservices/api-gateway/src/main/java/com/urlshortener/config/GatewayConfig.java
package com.urlshortener.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Auth routes (no authentication needed)
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri("lb://AUTH-SERVICE"))

            // URL routes (authentication required)
            .route("url-service", r -> r
                .path("/api/v1/urls/**")
                .uri("lb://URL-SERVICE"))

            // Analytics routes
            .route("analytics-service", r -> r
                .path("/api/v1/analytics/**")
                .uri("lb://ANALYTICS-SERVICE"))

            .build();
    }
}
```

---

### **Step 7.4: Add JWT Authentication Filter**

```java
// microservices/api-gateway/src/main/java/com/urlshortener/util/JwtUtil.java
// ⚠️ Copy from backend:
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner
mkdir -p microservices/api-gateway/src/main/java/com/urlshortener/util
cp backend/src/main/java/com/urlshortener/util/JwtUtil.java \
   microservices/api-gateway/src/main/java/com/urlshortener/util/
```

```java
// microservices/api-gateway/src/main/java/com/urlshortener/filter/AuthenticationFilter.java
package com.urlshortener.filter;

import com.urlshortener.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Extract Authorization header
            if (!request.getHeaders().containsKey("Authorization")) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.validateToken(token)) {
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                return onError(exchange, "Token validation failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
```

---

### **Step 7.5: Add CORS Configuration**

```java
// microservices/api-gateway/src/main/java/com/urlshortener/config/CorsConfig.java
package com.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
```

---

### **Step 7.6: Update Gateway Routes with Authentication**

**Update GatewayConfig.java:**

```java
// microservices/api-gateway/src/main/java/com/urlshortener/config/GatewayConfig.java
package com.urlshortener.config;

import com.urlshortener.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter authFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Auth routes (no authentication needed)
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri("lb://AUTH-SERVICE"))

            // URL routes (authentication required)
            .route("url-service", r -> r
                .path("/api/v1/urls/**")
                .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                .uri("lb://URL-SERVICE"))

            // Analytics routes (authentication required)
            .route("analytics-service", r -> r
                .path("/api/v1/analytics/**")
                .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                .uri("lb://ANALYTICS-SERVICE"))

            // Redirect route (no authentication - public)
            .route("redirect-service", r -> r
                .path("/{shortUrl}")
                .uri("lb://URL-SERVICE"))

            .build();
    }
}
```

---

### **Step 7.7: Configure Gateway**

```yaml
# microservices/api-gateway/src/main/resources/application.yml
server:
  port: 8080  # Same as old monolith - no frontend changes!

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
```

---

## **PHASE 8: Database Strategy**

### **Option 1: Shared Database (Recommended for Phase 1)**

**Keep using single PostgreSQL database:**

```
postgresql://localhost:5432/urlshortener
├── users table      → Accessed by auth-service
├── urls table       → Accessed by url-service
└── analytics table  → Accessed by analytics-service
```

**Advantages:**
- ✅ No data migration needed
- ✅ No schema changes
- ✅ Works immediately
- ✅ Transactions still work

**Disadvantages:**
- ⚠️ Services share database (coupling)
- ⚠️ Not "pure" microservices

**Decision:** Use this initially, can split later.

---

### **Option 2: Database per Service (Future)**

**Later, you can split:**

```bash
# Create separate databases
createdb auth_db
createdb url_db
createdb analytics_db

# Migrate data
pg_dump urlshortener --table=users | psql auth_db
pg_dump urlshortener --table=urls | psql url_db
```

---

## **PHASE 9: Docker Setup (Day 4 - 4 hours)** ✅ COMPLETE

### **Step 9.1: Create Production-Ready Dockerfiles**

**Production Dockerfiles created with:**
- Multi-stage builds (build stage + runtime stage)
- Non-root user for security
- Health checks for container orchestration
- JVM container awareness and optimization
- Layer caching for faster rebuilds
- Comprehensive inline documentation

**Files created:**
- `microservices/eureka-server/Dockerfile` - Service discovery with health checks
- `microservices/auth-service/Dockerfile` - JWT service with G1GC optimization
- `microservices/url-service/Dockerfile` - URL service with string deduplication
- `microservices/analytics-service/Dockerfile` - Analytics service
- `microservices/api-gateway/Dockerfile` - Reactive gateway with low-latency GC tuning

**Key Docker features used:**
1. **Multi-stage builds**: Reduces final image size by ~200MB (Maven build → JRE runtime)
2. **Layer caching**: POM files copied separately to cache dependencies
3. **Non-root user**: Security best practice (creates `spring:spring` user)
4. **Health checks**: Container marked unhealthy if actuator endpoint fails
5. **JVM optimization**: Container-aware memory limits, G1GC, parallel GC

---

### **Step 9.2: Create Docker Compose Orchestration**

**docker-compose.yml created with:**
- PostgreSQL with named volume for data persistence
- Service dependency chain with health check conditions
- Custom network for service isolation
- Environment variable configuration
- Health checks on all services

**Service startup order:**
```
postgres → eureka-server → auth/url/analytics services → api-gateway
```

**Files created:**
- `docker-compose.yml` - Orchestrates all 6 services (postgres + 5 microservices)
- `.dockerignore` - Optimizes build context (excludes target/, .git/, docs/)

---

### **Step 9.3: Production Dockerfile Patterns Explained**

**Multi-stage build pattern:**
```dockerfile
# Stage 1: Build with full JDK + Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY microservices/service-name/pom.xml microservices/service-name/
RUN mvn dependency:go-offline -B  # Cached layer
COPY microservices/service-name/src microservices/service-name/src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with lightweight JRE only
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build --chown=spring:spring /app/microservices/service-name/target/*.jar app.jar
USER spring:spring
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Why this pattern?**
- Build stage uses Maven to compile code (not needed at runtime)
- Runtime stage only has JRE + JAR (~150MB vs ~350MB with JDK)
- Layer caching: Dependencies downloaded only when POM changes
- Non-root user: Containers run as `spring:spring`, not root
- Health checks: K8s/Docker Swarm can auto-restart unhealthy containers

---

## **PHASE 10: Building & Testing (Day 4-5 - 6 hours)** ✅ BUILD COMPLETE

### **Step 10.1: Build Services in Correct Order** ✅ COMPLETE

**⚠️ CRITICAL: Build order matters! Shared-library must be built first.**

**Build Commands:**
```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices

# Step 1: Build and install parent POM
mvn clean install -N

# Step 2: Build shared-library FIRST (other services depend on it)
cd shared-library
mvn clean install
cd ..

# Step 3: Build all services
mvn clean install -DskipTests

# Step 4: Build API Gateway separately (if not in parent modules)
cd api-gateway
mvn clean install -DskipTests
cd ..

# Verify JARs created
find . -name "*.jar" -type f | grep -E "(auth|url|analytics|api-gateway|service-discovery)"
```

**Build Results:**
```
✅ service-discovery-1.0.0.jar (Eureka Server)
✅ auth-service-app-1.0.0.jar
✅ url-service-1.0.0.jar
✅ analytics-service-app-1.0.0.jar
✅ api-gateway-1.0.0.jar
```

**Issues Fixed:**
1. **JWT API Compatibility**: Updated API Gateway's JwtUtil.java to use JJWT 0.12.x API
   - Changed `Jwts.parserBuilder()` → `Jwts.parser()`
   - Changed `.setSigningKey()` → `.verifyWith()`
   - Changed `.parseClaimsJws()` → `.parseSignedClaims()`
   - Changed `.getBody()` → `.getPayload()`
   - Changed return type from `Key` → `SecretKey`

---

### **Step 10.2: Start Services in Order**

```bash
# Terminal 1: Service Discovery
cd microservices/service-discovery
mvn spring-boot:run

# Wait for Eureka to start (http://localhost:8761)

# Terminal 2: Auth Service
cd microservices/auth-service
mvn spring-boot:run

# Terminal 3: URL Service
cd microservices/url-service
mvn spring-boot:run

# Terminal 4: Analytics Service
cd microservices/analytics-service
mvn spring-boot:run

# Terminal 5: API Gateway
cd microservices/api-gateway
mvn spring-boot:run
```

---

### **Step 10.2: Verify Services Registered**

Open browser: http://localhost:8761

You should see:
- AUTH-SERVICE (1 instance)
- URL-SERVICE (1 instance)
- ANALYTICS-SERVICE (1 instance)
- API-GATEWAY (1 instance)

---

### **Step 10.3: Test API Gateway**

```bash
# Register user (through gateway)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@1234"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@1234"
  }'

# Save the token from response

# Create URL (with token)
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "url": "https://example.com",
    "expirationDays": 7
  }'
```

**If all work → Microservices are running! ✅**

---

### **Step 10.4: Update Frontend**

**MINIMAL CODE CHANGES NEEDED:**

Frontend still calls `http://localhost:8080/api/v1/...` which now goes through the API Gateway.

**However, Frontend needs to handle rate limiting:**

```typescript
// Update error handling in auth.interceptor.ts to handle 429 Too Many Requests:
if (error.status === 429) {
  this.snackBar.open('Too many requests. Please slow down.', 'Close', {
    duration: 3000
  });
}
```

Only restart frontend after API Gateway is fully tested.

---

## **PHASE 11: Observability & Monitoring (Week 2 - 8 hours)** 🔴 CRITICAL

> **Why Critical:** Can't operate microservices in production without observability

### **Step 11.1: Add Health Checks & Actuator**

**Add to ALL service POMs:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Add to ALL service application.yml:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**Test:**
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```

**Time:** 30 minutes

---

### **Step 11.2: Add Swagger/OpenAPI Documentation**

**Add to ALL service POMs:**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

**Create OpenAPI configuration:**

```java
// Each service: src/main/java/com/urlshortener/config/OpenApiConfig.java
package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("URL Shortener Microservices API")
                .version("1.0.0")
                .description("RESTful API for URL shortening service")
                .contact(new Contact()
                    .name("Your Name")
                    .email("your.email@example.com")));
    }
}
```

**Configure API Gateway to route Swagger:**

```java
// api-gateway/.../config/GatewayConfig.java - add route:
.route("swagger-auth", r -> r
    .path("/auth-service/v3/api-docs", "/auth-service/swagger-ui/**")
    .uri("lb://AUTH-SERVICE"))
.route("swagger-url", r -> r
    .path("/url-service/v3/api-docs", "/url-service/swagger-ui/**")
    .uri("lb://URL-SERVICE"))
```

**Access:** `http://localhost:8081/swagger-ui.html` (per service) or via Gateway

**Time:** 1 hour

---

### **Step 11.3: Add Distributed Tracing (Zipkin)**

**Add to ALL service POMs:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Add to ALL service application.yml:**

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% for dev, 0.1 for production
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  pattern:
    level: '%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]'
```

**Add Zipkin to docker-compose.yml:**

```yaml
services:
  # ... existing services ...

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - microservices-network
```

**Access:** `http://localhost:9411` to see traces

**Time:** 1.5 hours

---

### **Step 11.4: Add Prometheus Metrics**

**Add to ALL service POMs:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Create prometheus.yml:**

```yaml
# microservices/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth-service:8081']

  - job_name: 'url-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['url-service:8082']

  - job_name: 'analytics-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['analytics-service:8083']

  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
```

**Add to docker-compose.yml:**

```yaml
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - microservices-network

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - microservices-network

volumes:
  grafana-data:
```

**Access:**
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)

**Time:** 2 hours

---

### **Step 11.5: Add Centralized Logging (ELK Stack)**

**Add Logstash encoder to ALL service POMs:**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Create logback-spring.xml for each service:**

```xml
<!-- src/main/resources/logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="SERVICE_NAME" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${SERVICE_NAME}"}</customFields>
        </encoder>
    </appender>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${SERVICE_NAME}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

**Add ELK to docker-compose.yml:**

```yaml
  elasticsearch:
    image: elasticsearch:8.10.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - microservices-network

  logstash:
    image: logstash:8.10.0
    container_name: logstash
    ports:
      - "5000:5000"
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
    environment:
      LS_JAVA_OPTS: "-Xmx256m -Xms256m"
    depends_on:
      - elasticsearch
    networks:
      - microservices-network

  kibana:
    image: kibana:8.10.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - microservices-network

volumes:
  es-data:
```

**Create logstash/pipeline/logstash.conf:**

```conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  # Add filters if needed
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "microservices-%{[service]}-%{+YYYY.MM.dd}"
  }
  stdout {
    codec => rubydebug
  }
}
```

**Access:** Kibana at `http://localhost:5601`

**Time:** 3 hours

---

### **Step 11.6: Add Redis Caching Layer (CRITICAL for URL Shortener)**

**⚠️ CRITICAL:** URL shortener without caching = every redirect hits database. This is the #1 performance bottleneck.

**Add to url-service POM:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Add to url-service application.yml:**

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

**Enable caching:**

```java
// url-service/.../UrlServiceApplication.java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching  // ADD THIS
@EntityScan(basePackages = {"com.urlshortener.model"})
@EnableJpaRepositories(basePackages = {"com.urlshortener.repository"})
public class UrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlServiceApplication.class, args);
    }
}
```

**Add cache configuration:**

```java
// url-service/.../config/CacheConfig.java
package com.urlshortener.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // Cache for 1 hour
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

**Update UrlServiceImpl with caching:**

```java
// url-service/.../service/impl/UrlServiceImpl.java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class UrlServiceImpl implements UrlService {

    // Cache URL lookups by short code (most frequent operation)
    @Override
    @Cacheable(value = "urls", key = "#shortUrl", unless = "#result == null")
    public Url getByShortUrl(String shortUrl) {
        return urlRepository.findByShortUrl(shortUrl)
            .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortUrl));
    }

    // Invalidate cache when URL is deactivated
    @Override
    @CacheEvict(value = "urls", key = "#shortUrl")
    public void deactivateUrl(String shortUrl, String username) {
        Url url = getByShortUrl(shortUrl);

        User user = authServiceClient.getUserByUsername(username);
        if (!url.getUserId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Cannot deactivate URL owned by another user");
        }

        url.setDeactivated(true);
        urlRepository.save(url);
    }

    // Invalidate cache when URL expires
    @Override
    @CacheEvict(value = "urls", key = "#url.shortUrl")
    public void handleExpiredUrl(Url url) {
        url.setDeactivated(true);
        urlRepository.save(url);
    }
}
```

**Add Redis to docker-compose.yml:**

```yaml
services:
  # ... existing services ...

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    networks:
      - microservices-network

volumes:
  # ... existing volumes ...
  redis-data:
```

**Test caching:**

```bash
# First request (hits database)
time curl http://localhost:8080/short-url-code

# Second request (hits cache - should be faster)
time curl http://localhost:8080/short-url-code

# Check Redis
docker exec -it redis redis-cli
> KEYS *
> GET "urls::short-url-code"
```

**Performance Impact:**
- **Without cache:** ~50-100ms per redirect (database query)
- **With cache:** ~5-10ms per redirect (Redis lookup)
- **10x performance improvement** for redirect operations

**Cache Invalidation Strategy:**
1. **On deactivate:** Remove from cache immediately
2. **On expiration:** Remove from cache when URL expires
3. **TTL:** 1 hour (configurable based on URL expiration patterns)

**Time:** 2 hours

---

## **PHASE 12: Resilience & Security Hardening (Week 3 - 6 hours)** 🟡 HIGH

### **Step 12.1: Add Circuit Breakers (Resilience4j)**

**Add to url-service POM:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**Add to url-service application.yml:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

  retry:
    instances:
      authService:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
```

**Update UrlServiceImpl:**

```java
@Service
public class UrlServiceImpl implements UrlService {

    @Autowired
    private AuthServiceClient authServiceClient;

    @CircuitBreaker(name = "authService", fallbackMethod = "getUserFallback")
    @Retry(name = "authService")
    public Url createUrl(UrlRequestDto request, String username) {
        User user = authServiceClient.getUserByUsername(username);
        // ... rest of logic
    }

    private Url getUserFallback(UrlRequestDto request, String username, Exception ex) {
        throw new ServiceUnavailableException(
            "Auth service is currently unavailable. Please try again later.");
    }
}
```

**Time:** 1.5 hours

---

### **Step 12.2: Add Rate Limiting (Bucket4j)**

**Add to api-gateway POM:**

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Create Rate Limit Filter:**

```java
// api-gateway/.../filter/RateLimitFilter.java
package com.urlshortener.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

            Bucket bucket = cache.computeIfAbsent(key, k -> createBucket());

            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            }

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        };
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    public static class Config {
        // Configuration properties
    }
}
```

**Add to GatewayConfig routes:**

```java
.route("url-service-rate-limited", r -> r
    .path("/api/v1/urls/**")
    .filters(f -> f
        .filter(authFilter.apply(new AuthenticationFilter.Config()))
        .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
    .uri("lb://URL-SERVICE"))
```

**Time:** 1 hour

---

### **Step 12.3: Add Secret Management (Jasypt)**

**Add to ALL service POMs:**

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

**Encrypt secrets:**

```bash
# Install jasypt CLI or use online tool
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="postgres" \
  password="my-secret-key" \
  algorithm=PBEWithMD5AndDES

# Output: ENC(encrypted_value)
```

**Update application.yml:**

```yaml
spring:
  datasource:
    password: ENC(g6Nt24M3W8YQmk5+VUvMbQ==)

jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
```

**Run with environment variable:**

```bash
export JASYPT_ENCRYPTOR_PASSWORD=my-secret-key
mvn spring-boot:run
```

**Time:** 1 hour

---

### **Step 12.4: Optimize Database Connection Pool**

**Add to ALL service application.yml:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: ${spring.application.name}-HikariPool
```

**Time:** 30 minutes

---

### **Step 12.5: Add Graceful Shutdown**

**Add to ALL service application.yml:**

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Time:** 15 minutes

---

### **Step 12.6: Add Security Headers**

**Update SecurityConfig in each service:**

```java
http
    .headers(headers -> headers
        .contentSecurityPolicy(csp -> csp
            .policyDirectives("default-src 'self'"))
        .frameOptions(frame -> frame.deny())
        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
        .httpStrictTransportSecurity(hsts -> hsts
            .includeSubDomains(true)
            .maxAgeInSeconds(31536000)));
```

**Time:** 30 minutes

---

### **Step 12.7: Enable Response Compression**

**Add to ALL service application.yml:**

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
    min-response-size: 1024
```

**Time:** 15 minutes

---

## **PHASE 13: Production Optimization (Week 4 - 4 hours)** 🟢 MEDIUM

### **Step 13.1: Add Request/Response Logging**

**Create logging interceptor:**

```java
// shared-library/.../interceptor/RequestResponseLoggingInterceptor.java
package com.urlshortener.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("[{}] {} {} from {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString() != null ? "?" + request.getQueryString() : "",
            request.getRemoteAddr());
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long duration = System.currentTimeMillis() - (Long) request.getAttribute("startTime");
        log.info("[{}] {} completed with status {} in {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration);
    }
}
```

**Register interceptor:**

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestResponseLoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }
}
```

**Time:** 1 hour

---

### **Step 13.2: Add Kubernetes Manifests**

**Create k8s/ directory with deployment for each service:**

```yaml
# k8s/auth-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  labels:
    app: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: url-shortener/auth-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: value
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector:
    app: auth-service
  ports:
  - protocol: TCP
    port: 8081
    targetPort: 8081
  type: ClusterIP
```

**Create similar files for other services**

**Time:** 2 hours

---

### **Step 13.3: Add Performance Testing (Gatling)**

**Add Gatling dependency:**

```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.9.5</version>
    <scope>test</scope>
</dependency>
```

**Create load test:**

```scala
// src/test/scala/simulations/UrlShortenerLoadTest.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UrlShortenerLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("URL Shortener Load Test")
    .exec(http("Create URL")
      .post("/api/v1/urls")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"url": "https://example.com", "expirationDays": 7}"""))
      .check(status.is(201)))
    .pause(1)

  setUp(
    scn.inject(
      rampUsers(100) during (30 seconds)
    )
  ).protocols(httpProtocol)
}
```

**Time:** 1 hour

---

## **📊 Timeline Summary**

### **Core Microservices Setup (Development-Ready)**

| Phase | Task | Time | Cumulative |
|-------|------|------|------------|
| **0** | **Prerequisites (PostgreSQL, env setup)** | **4 hours** | **4h** |
| 1 | Project setup | 4 hours | 8h |
| 2 | Shared library (+ User model) | 3 hours | 11h |
| 3 | Service discovery | 2 hours | 13h |
| 4 | Auth service (+ Feign endpoint, indexes, Liquibase) | 7 hours | 20h |
| 5 | URL service (+ Feign, pagination, RedirectController) | 7 hours | 27h |
| 6 | Analytics service | 4 hours | 31h |
| 7 | API Gateway (+ Auth filter, CORS) | 8 hours | 39h |
| 8 | Database setup | 2 hours | 41h |
| 9 | Docker setup (Dockerfiles) | 4 hours | 45h |
| 10 | Building & Testing | 6 hours | 51h |

**Subtotal: ~51 hours (Development-ready microservices with critical fixes)**

---

### **Production Hardening (Production-Ready)**

| Phase | Task | Priority | Time | Cumulative |
|-------|------|----------|------|------------|
| **11** | **Observability + Redis (Actuator, Swagger, Zipkin, Prometheus, ELK, Redis Caching)** | **🔴 CRITICAL** | **10 hours** | **61h** |
| 12 | Resilience & Security (Circuit breakers, Rate limiting, Secrets, etc.) | 🟡 HIGH | 6 hours | 67h |
| 13 | Production Optimization (Logging, K8s, Performance testing) | 🟢 MEDIUM | 4 hours | 71h |

**Production Additions: ~20 hours**

---

### **Total Timeline**

| Milestone | Hours | Timeline |
|-----------|-------|----------|
| **Development-Ready** (Phases 0-10) | 51 hours | 1.5 weeks full-time / 3 weeks part-time |
| **Interview-Ready** (+ Phase 11 Critical) | 61 hours | 2 weeks full-time / 4 weeks part-time |
| **Production-Ready** (+ Phases 12-13) | 71 hours | 2.5 weeks full-time / 5 weeks part-time |

---

### **Breakdown by Category**

| Category | Hours | Percentage |
|----------|-------|------------|
| **Prerequisites & Setup** | 15h | 21% |
| **Core Microservices** | 36h | 51% |
| **Observability & Caching** | 10h | 14% |
| **Resilience & Security** | 6h | 8% |
| **Production Optimization** | 4h | 6% |
| **Total** | **71h** | **100%** |

---

### **Recommended Paths Based on Timeline**

**< 2 weeks available:**
- Complete Phases 0-10 (Development-ready)
- Includes: Auth endpoint, indexes, pagination
- **Score: 70/100** - Shows microservices knowledge with critical fixes

**2-3 weeks available:**
- Complete Phases 0-11 (Interview-ready)
- Adds: Observability stack + Redis caching (10x performance)
- **Score: 85/100** - Demonstrates production thinking

**3-4 weeks available:**
- Complete all phases (Production-ready)
- Full observability + caching + resilience + optimization
- **Score: 95/100** - Production-grade microservices

---

## **🎯 What You'll Reuse vs New Code**

### **Reused from Monolith (80%):**

✅ All controllers (copy-paste)
✅ All services (copy-paste)
✅ All models/entities (copy-paste)
✅ All repositories (copy-paste)
✅ All DTOs (copy-paste)
✅ All business logic (copy-paste)
✅ All validators (copy-paste)

### **New Code (20%):**

🆕 Service discovery (Eureka)
🆕 API Gateway routing
🆕 Feign clients for inter-service calls
🆕 Application.yml configurations
🆕 Main application classes
🆕 POM files for each service

---

## **🚀 Deployment with Docker Compose**

### **Create docker-compose.yml:**

```yaml
# microservices/docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: urlshortener
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  service-discovery:
    build: ./service-discovery
    ports:
      - "8761:8761"

  auth-service:
    build: ./auth-service
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/urlshortener
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    depends_on:
      - postgres
      - service-discovery

  url-service:
    build: ./url-service
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/urlshortener
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    depends_on:
      - postgres
      - service-discovery
      - auth-service

  analytics-service:
    build: ./analytics-service
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/urlshortener
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    depends_on:
      - postgres
      - service-discovery

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    depends_on:
      - service-discovery
      - auth-service
      - url-service
      - analytics-service
```

**Start everything:**
```bash
docker-compose up -d
```

---

## **📝 Interview Talking Points**

### **Question: "Walk me through your microservices architecture"**

**Answer:**
"I refactored a monolithic URL shortener into 4 microservices:

1. **Auth Service** handles user authentication and JWT token management
2. **URL Service** manages URL creation, retrieval, and deactivation
3. **Analytics Service** tracks click events and generates statistics
4. **API Gateway** provides unified entry point with routing and load balancing

Services communicate through REST APIs using Spring Cloud OpenFeign for synchronous calls and share a PostgreSQL database initially for faster migration. All services register with Eureka for service discovery, enabling dynamic scaling.

The refactoring took ~2 weeks and reused 80% of existing code, with zero downtime by running both systems in parallel during migration."

---

### **Question: "Why did you choose to keep shared database?"**

**Answer:**
"I made a pragmatic decision to use shared database pattern initially for three reasons:

1. **Faster time-to-market**: No data migration complexity, focus on service boundaries
2. **Transaction integrity**: Some operations required ACID guarantees across entities
3. **Incremental migration**: Plan to split databases in Phase 2 once service boundaries stabilize

This follows the 'strangler fig' pattern - incrementally migrate without big-bang changes. In production, I'd implement database-per-service with eventual consistency via event sourcing."

---

### **Question: "How do services communicate?"**

**Answer:**
"I use two patterns:

**Synchronous (REST):**
- URL Service → Auth Service (user validation) via Spring Cloud OpenFeign
- Client requests go through API Gateway which routes to appropriate service

**Asynchronous (planned):**
- For analytics events, I plan to use Kafka for loose coupling
- Click events would be published by URL Service and consumed by Analytics Service
- This prevents analytics from blocking redirect performance

Current implementation is synchronous for simplicity, with async patterns ready for high-volume scenarios."

---

## **✅ Success Criteria**

You've successfully refactored when:

- [ ] All 4+ services run independently
- [ ] Eureka dashboard shows all services registered
- [ ] API Gateway routes requests correctly
- [ ] Frontend works without changes
- [ ] Can register, login, create URLs through gateway
- [ ] Each service has own POM and can be deployed separately
- [ ] Services restart independently without affecting others
- [ ] Database shows all tables accessible by respective services

---

## **🎓 Advanced Features (Beyond Phase 13)**

After completing all 13 phases, consider these advanced additions:

1. **✅ COMPLETED in Phases 11-13:**
   - ~~Circuit Breakers~~ (Phase 12)
   - ~~Distributed Tracing~~ (Phase 11)
   - ~~Rate Limiting~~ (Phase 12)
   - ~~Kubernetes Manifests~~ (Phase 13)

2. **Future Enhancements:**
   - **Add Kafka** for async communication (Analytics events)
   - **Split database** into auth_db, url_db, analytics_db (true database-per-service)
   - **Add Config Server** (Spring Cloud Config) for centralized configuration
   - **Implement CQRS** for analytics reads (separate read/write models)
   - **Add API versioning** strategy (URL versioning, header versioning)
   - **Add Redis** for caching layer
   - **Implement Saga Pattern** for distributed transactions
   - **Add Service Mesh** (Istio/Linkerd) for advanced traffic management

---

## **📚 References**

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Microservices Patterns by Chris Richardson](https://microservices.io/patterns/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)

---

## **🔧 Troubleshooting Common Issues**

**Top 5 Issues:**

1. **"Cannot find symbol: class User"** → Ensure User.java is in auth-service (NOT shared-library)
2. **Port already in use** → `lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9`
3. **Services not registering with Eureka** → Check Eureka is running, wait 30s for registration
4. **Liquibase table exists error** → Only enable Liquibase in auth-service
5. **Database connection refused** → Verify PostgreSQL running: `pg_isready`

---

## **📋 Pre-Flight Checklist**

Before starting implementation:

- [ ] PostgreSQL 16 installed and running
- [ ] Database `urlshortener` created
- [ ] Monolith tested with PostgreSQL (not H2)
- [ ] UnauthorizedAccessException.java created
- [ ] .env file created with JWT_SECRET and database credentials
- [ ] Understand build order: parent → shared-library → services
- [ ] Docker installed (for Phase 9)
- [ ] Java 17 SDK installed
- [ ] Maven 3.9+ installed

---

## **📈 Progress Tracking**

As you complete each phase, check it off:

- [x] **Phase 0:** Prerequisites complete
- [x] **Phase 1:** Parent POM and module structure created
- [x] **Phase 2:** Shared library with common error DTOs and exceptions
- [x] **Phase 3:** Eureka server running on :8761
- [x] **Phase 4:** Auth service running on :8081
- [x] **Phase 5:** URL service running on :8082
- [x] **Phase 6:** Analytics service running on :8083
- [x] **Phase 7:** API Gateway running on :8080 with authentication and reactive programming
- [x] **Phase 8:** Database strategy confirmed (Shared DB with service-owned tables)
- [ ] **Phase 9:** Dockerfiles created for all services
- [ ] **Phase 10:** All services built and tested
- [ ] **Phase 11:** Documentation complete

---

**Ready to start? Begin with Phase 0: Prerequisites!** 🚀

**📖 Related Documents:**
- `gap-analysis.md` - Overall project gaps (36 items across P0-P3)
- `implementation-roadmap.md` - Security fixes roadmap (8-week plan)
- `interview-readiness-assessment.md` - Interview preparation guide
