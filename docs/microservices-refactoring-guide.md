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
│   ├── auth-service/
│   │   └── src/main/java/com/urlshortener/
│   │       ├── controller/       # AuthController (from monolith)
│   │       ├── service/          # AuthService (from monolith)
│   │       ├── model/            # User.java (from monolith)
│   │       ├── repository/       # UserRepository (from monolith)
│   │       └── config/           # SecurityConfig (modified)
│   │
│   ├── url-service/
│   │   └── src/main/java/com/urlshortener/
│   │       ├── controller/       # UrlController (from monolith)
│   │       ├── service/          # UrlService (from monolith)
│   │       ├── model/            # Url.java (from monolith)
│   │       └── repository/       # UrlRepository (from monolith)
│   │
│   └── analytics-service/
│       └── src/main/java/com/urlshortener/
│           ├── controller/       # AnalyticsController (from monolith)
│           ├── service/          # AnalyticsService (from monolith)
│           └── repository/       # Analytics repositories
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
        <module>auth-service</module>
        <module>url-service</module>
        <module>analytics-service</module>
    </modules>

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

## **PHASE 4: Extract Auth Service (Day 2 - 4 hours)**

### **Step 4.1: Create Auth Service POM**

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

    <artifactId>auth-service</artifactId>
    <name>Auth Service</name>

    <dependencies>
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
</project>
```

---

### **Step 4.2: Copy Auth Code from Monolith**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner

# Create directory structure
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/controller
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/service/impl
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/model
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/repository
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/config
mkdir -p microservices/auth-service/src/main/java/com/urlshortener/util

# Copy Controller
cp backend/src/main/java/com/urlshortener/controller/AuthController.java \
   microservices/auth-service/src/main/java/com/urlshortener/controller/

# Copy Service
cp backend/src/main/java/com/urlshortener/service/AuthService.java \
   microservices/auth-service/src/main/java/com/urlshortener/service/
cp backend/src/main/java/com/urlshortener/service/impl/AuthServiceImpl.java \
   microservices/auth-service/src/main/java/com/urlshortener/service/impl/

# Copy Model
cp backend/src/main/java/com/urlshortener/model/User.java \
   microservices/auth-service/src/main/java/com/urlshortener/model/

# Copy Repository
cp backend/src/main/java/com/urlshortener/repository/UserRepository.java \
   microservices/auth-service/src/main/java/com/urlshortener/repository/

# Copy Config
cp backend/src/main/java/com/urlshortener/config/SecurityConfig.java \
   microservices/auth-service/src/main/java/com/urlshortener/config/

# Copy JWT Util
cp backend/src/main/java/com/urlshortener/util/JwtUtil.java \
   microservices/auth-service/src/main/java/com/urlshortener/util/
```

**Files copied (NO CODE CHANGES):**

```
auth-service/src/main/java/com/urlshortener/
├── controller/
│   └── AuthController.java              ✓ COPIED
├── service/
│   ├── AuthService.java                 ✓ COPIED
│   └── impl/AuthServiceImpl.java        ✓ COPIED
├── model/
│   └── User.java                        ✓ COPIED
├── repository/
│   └── UserRepository.java              ✓ COPIED
├── config/
│   └── SecurityConfig.java              ✓ COPIED (minor modification needed)
└── util/
    └── JwtUtil.java                     ✓ COPIED
```

---

### **Step 4.3: Create Auth Service Application**

```java
// microservices/auth-service/src/main/java/com/urlshortener/AuthServiceApplication.java
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

### **Step 4.4: Configure Auth Service**

```yaml
# microservices/auth-service/src/main/resources/application.yml
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

  # ⚠️ IMPORTANT: Only auth-service manages DB schema (shared database pattern)
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
# Copy all Liquibase changelogs from monolith to auth-service
cp -r backend/src/main/resources/db \
   microservices/auth-service/src/main/resources/
```

---

### **Step 4.5: Add User Lookup Endpoint for Feign Clients**

**⚠️ CRITICAL:** This endpoint is required for URL Service to validate users via Feign.

**Add to AuthController:**

```java
// microservices/auth-service/.../controller/AuthController.java
// Add this method to existing AuthController class:

@GetMapping("/users/{username}")
public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
    User user = authService.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    return ResponseEntity.ok(user);
}
```

**Add to AuthService interface:**

```java
// microservices/auth-service/.../service/AuthService.java
// Add this method to interface:

Optional<User> findByUsername(String username);
```

**Add to AuthServiceImpl:**

```java
// microservices/auth-service/.../service/impl/AuthServiceImpl.java
// Add this method to implementation:

@Override
public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
}
```

**Why Needed:**
- URL Service calls this via Feign to validate user exists before creating URLs
- Without this, URL Service will fail with 404 Not Found

**Test:**
```bash
# After auth-service is running:
curl http://localhost:8081/api/v1/auth/users/testuser \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### **Step 4.6: Add Database Indexes for Performance**

**⚠️ CRITICAL:** Without indexes, queries will be slow under load.

**Create new Liquibase changelog:**

```bash
# Create new changelog file
cat > microservices/auth-service/src/main/resources/db/changelog/db.changelog-002-indexes.yaml << 'EOF'
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
# Edit: microservices/auth-service/src/main/resources/db/changelog/db.changelog-master.yaml
# Add after existing include:

databaseChangeLog:
  - include:
      file: db/changelog/db.changelog-001-initial-schema.yaml
  - include:
      file: db/changelog/db.changelog-002-indexes.yaml  # ADD THIS
```

**Impact:**
- Faster user lookup by username/email
- Faster URL lookup by short code
- Faster user's URLs retrieval
- Reduced database load under high traffic

---

## **PHASE 5: Extract URL Service (Day 2-3 - 4 hours)**

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

        <!-- For calling Auth Service -->
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

**⚠️ Important:** RedirectController belongs in URL Service since it handles URL redirection.

---

### **Step 5.3: Add Feign Client for Auth Service**

**This is NEW code (not from monolith):**

```java
// microservices/url-service/src/main/java/com/urlshortener/client/AuthServiceClient.java
package com.urlshortener.client;

import com.urlshortener.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{username}")
    User getUserByUsername(@PathVariable String username);
}
```

---

### **Step 5.4: Modify UrlServiceImpl to Use Feign**

```java
// microservices/url-service/.../service/impl/UrlServiceImpl.java
// BEFORE (in monolith):
User user = userRepository.findByUsername(userName)
    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

// AFTER (in microservice):
@Autowired
private AuthServiceClient authServiceClient;

User user = authServiceClient.getUserByUsername(userName);
if (user == null) {
    throw new UsernameNotFoundException("User not found in auth service");
}
```

---

### **Step 5.5: Add Feign Error Handling**

```java
// microservices/url-service/src/main/java/com/urlshortener/client/AuthServiceClientFallback.java
package com.urlshortener.client;

import com.urlshortener.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public User getUserByUsername(String username) {
        throw new RuntimeException("Auth service is unavailable. Please try again later.");
    }
}
```

**Update AuthServiceClient with fallback:**

```java
// microservices/url-service/src/main/java/com/urlshortener/client/AuthServiceClient.java
package com.urlshortener.client;

import com.urlshortener.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{username}")
    User getUserByUsername(@PathVariable String username);
}
```

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
@EnableFeignClients
@EntityScan(basePackages = {"com.urlshortener.model"})
@EnableJpaRepositories(basePackages = {"com.urlshortener.repository"})
public class UrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlServiceApplication.class, args);
    }
}
```

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

### **Step 5.8: Add Pagination Support**

**⚠️ IMPORTANT:** Without pagination, fetching all URLs for users with thousands of URLs will cause memory issues.

**Update UrlController:**

```java
// microservices/url-service/.../controller/UrlController.java
// Replace the existing getUserUrls method with:

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@GetMapping
public ResponseEntity<Page<UrlResponseDto>> getUserUrls(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String direction) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ?
        Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    Page<UrlResponseDto> urls = urlService.getUserUrls(username, pageable);
    return ResponseEntity.ok(urls);
}
```

**Update UrlService interface:**

```java
// microservices/url-service/.../service/UrlService.java

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UrlService {
    // ... existing methods

    Page<UrlResponseDto> getUserUrls(String username, Pageable pageable);
}
```

**Update UrlServiceImpl:**

```java
// microservices/url-service/.../service/impl/UrlServiceImpl.java

@Override
public Page<UrlResponseDto> getUserUrls(String username, Pageable pageable) {
    User user = authServiceClient.getUserByUsername(username);
    if (user == null) {
        throw new UsernameNotFoundException("User not found");
    }

    Page<Url> urlPage = urlRepository.findByUserIdAndDeactivatedFalse(
        user.getId(), pageable);

    return urlPage.map(url -> new UrlResponseDto(
        url.getShortUrl(),
        url.getOriginalUrl(),
        url.getCreatedAt(),
        url.getExpiresAt(),
        url.getClickCount()
    ));
}
```

**Update UrlRepository:**

```java
// microservices/url-service/.../repository/UrlRepository.java

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<Url, Long> {
    // ... existing methods

    Page<Url> findByUserIdAndDeactivatedFalse(Long userId, Pageable pageable);
}
```

**Test Pagination:**

```bash
# Get first page (20 URLs)
curl http://localhost:8080/api/v1/urls?page=0&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get second page
curl http://localhost:8080/api/v1/urls?page=1&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Sort by click count descending
curl http://localhost:8080/api/v1/urls?page=0&size=10&sortBy=clickCount&direction=DESC \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Benefits:**
- Reduced memory usage for users with many URLs
- Faster API response times
- Better frontend performance
- Database query optimization via indexes

---

## **PHASE 6: Extract Analytics Service (Day 3 - 3 hours)**

### **Similar to URL Service - Copy these files:**

```bash
# Copy Analytics Controller
cp backend/src/main/java/com/urlshortener/controller/AnalyticsController.java \
   microservices/analytics-service/src/main/java/com/urlshortener/controller/

# Copy Analytics Service
cp backend/src/main/java/com/urlshortener/service/AnalyticsService.java \
   microservices/analytics-service/src/main/java/com/urlshortener/service/
cp backend/src/main/java/com/urlshortener/service/impl/AnalyticsServiceImpl.java \
   microservices/analytics-service/src/main/java/com/urlshortener/service/impl/

# Copy Analytics Model (if exists)
# cp backend/src/main/java/com/urlshortener/model/ClickEvent.java ...

# Copy Analytics Repository
cp backend/src/main/java/com/urlshortener/repository/*Analytics*.java \
   microservices/analytics-service/src/main/java/com/urlshortener/repository/
```

**Port: 8083**

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

## **PHASE 9: Docker Setup (Day 4 - 4 hours)**

### **Step 9.1: Create Dockerfiles for Each Service**

**Template Dockerfile for all services:**

```dockerfile
# microservices/service-discovery/Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/service-discovery-1.0.0.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Create Dockerfiles:**

```bash
cd /Users/manvigupta/Downloads/manvi/manvi-projects/urlShortner/microservices

# Service Discovery
cat > service-discovery/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/service-discovery-1.0.0.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# API Gateway
cat > api-gateway/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Auth Service
cat > auth-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/auth-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# URL Service
cat > url-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/url-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Analytics Service
cat > analytics-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/analytics-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
```

---

### **Step 9.2: Update docker-compose.yml**

The docker-compose.yml from the guide already references these Dockerfiles.

---

## **PHASE 10: Building & Testing (Day 4-5 - 6 hours)**

### **Step 10.1: Build Services in Correct Order**

**⚠️ CRITICAL: Build order matters! Shared-library must be built first.**

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

# Verify JARs created
find . -name "*.jar" -type f | grep -E "(auth|url|analytics|api-gateway|service-discovery)"
```

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

- [ ] **Phase 0:** Prerequisites complete
- [ ] **Phase 1:** Parent POM and module structure created
- [ ] **Phase 2:** Shared library with common error DTOs and exceptions
- [ ] **Phase 3:** Eureka server running on :8761
- [ ] **Phase 4:** Auth service running on :8081
- [ ] **Phase 5:** URL service running on :8082
- [ ] **Phase 6:** Analytics service running on :8083
- [ ] **Phase 7:** API Gateway running on :8080 with authentication
- [ ] **Phase 8:** Database strategy confirmed
- [ ] **Phase 9:** Dockerfiles created for all services
- [ ] **Phase 10:** All services built and tested
- [ ] **Phase 11:** Documentation complete

---

**Ready to start? Begin with Phase 0: Prerequisites!** 🚀

**📖 Related Documents:**
- `gap-analysis.md` - Overall project gaps (36 items across P0-P3)
- `implementation-roadmap.md` - Security fixes roadmap (8-week plan)
- `interview-readiness-assessment.md` - Interview preparation guide
