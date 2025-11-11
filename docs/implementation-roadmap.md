# URL Shortener - Implementation Roadmap for Interview Preparation

**Target Role:** Senior Full-Stack Java Developer (10 YOE, Microservices)
**Created:** November 9, 2025
**Total Gaps:** 36 identified + Microservices Architecture Gap

---

## 🎯 Executive Decision: Choose Your Path

### **Option A: Fix Current Project (Recommended if < 1 month)**
- **Time:** 4-6 weeks for interview-ready state
- **Pros:** Already invested, demonstrates completion
- **Cons:** Retrofitting microservices is harder
- **Best for:** Quick job search, already started applying

### **Option B: Build New Microservices Project**
- **Time:** 6-8 weeks for complete implementation
- **Pros:** Clean microservices from start, better showcase
- **Cons:** Starting from scratch, more time required
- **Best for:** Have 2+ months, want perfect portfolio

**This roadmap covers Option A (fixing current project)**

---

## 📊 Current State Assessment

| Aspect | Current | Target | Gap |
|--------|---------|--------|-----|
| **Architecture** | Monolith | Microservices | Critical |
| **Testing** | 19 tests (~5%) | 80%+ coverage | Critical |
| **Security** | 6 P0 issues | Production-ready | Critical |
| **Infrastructure** | None | Docker/K8s/CI-CD | High |
| **Observability** | None | Metrics/Tracing/Logging | High |
| **Interview Readiness** | 25/100 | 80+/100 | Critical |

---

## 🚀 IMPLEMENTATION ROADMAP

## **SPRINT 0: Critical Decision (Day 0-1)**

### **Make This Decision First:**

**Will you convert to microservices or keep as monolith?**

#### **Path 1: Keep as Monolith (Faster)**
- ⏱️ Time to Interview-Ready: 2-3 weeks
- ✅ Pros: Faster, simpler
- ❌ Cons: Doesn't match "microservices experience" claim
- 📌 **Recommendation:** Only if you have < 2 weeks

#### **Path 2: Convert to Microservices (Better)**
- ⏱️ Time to Interview-Ready: 4-6 weeks
- ✅ Pros: Matches resume, impressive for senior role
- ❌ Cons: More complex, takes longer
- 📌 **Recommendation:** If you have 1+ month

**For this roadmap, we'll follow Path 2 (Microservices)**

---

## **SPRINT 1: Critical Security Fixes (Week 1 - Days 1-5)**

**Goal:** Fix all P0 security issues that would cause immediate rejection

**Estimated Effort:** 5 days (full-time) or 10 days (part-time)

### **Day 1: Environment Setup & Security Foundation**

#### Task 1.1: Move JWT Secret to Environment Variables (2 hours)
**Files:** `application.yml`, `SecurityConfig.java`, `JwtUtil.java`

```yaml
# application.yml - REMOVE hardcoded secret
jwt:
  secret: ${JWT_SECRET}  # Now from env var
  expiration: 86400000

# Create .env.example
JWT_SECRET=your-secret-key-here-min-256-bits
DATABASE_URL=jdbc:postgresql://localhost:5432/urlshortener
REDIS_HOST=localhost
REDIS_PORT=6379
```

```java
// Update JwtUtil.java
@Value("${JWT_SECRET:#{null}}")
private String jwtSecret;

@PostConstruct
public void validateSecret() {
    if (jwtSecret == null || jwtSecret.length() < 32) {
        throw new IllegalStateException(
            "JWT_SECRET must be set and at least 32 characters"
        );
    }
}
```

**Commit:** `feat: move JWT secret to environment variables`

---

#### Task 1.2: Fix Authorization Bug on URL Deactivation (2 hours)
**Files:** `UrlController.java`, `UrlServiceImpl.java`, `UnauthorizedAccessException.java`

```java
// Create UnauthorizedAccessException.java
package com.urlshortener.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

// Update UrlController.java
@DeleteMapping("/{shortUrl}")
public ResponseEntity<Void> deactivateUrl(
        @PathVariable String shortUrl,
        Principal principal) {
    urlService.deactivateUrl(shortUrl, principal.getName());
    return ResponseEntity.noContent().build();
}

// Update UrlServiceImpl.java
@Override
@Transactional
public void deactivateUrl(String shortUrl, String username) {
    Url url = urlRepository.findByShortUrl(shortUrl)
        .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortUrl));

    // CRITICAL: Check ownership
    if (!url.getUser().getUsername().equals(username)) {
        throw new UnauthorizedAccessException(
            "You do not have permission to deactivate this URL"
        );
    }

    url.setDeactivated(true);
    urlRepository.save(url);
}

// Update GlobalExceptionHandler.java
@ExceptionHandler(UnauthorizedAccessException.class)
public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
        UnauthorizedAccessException ex) {
    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.FORBIDDEN.value())
        .error("Forbidden")
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

**Test:**
```java
// Add test in UrlServiceImplTest.java
@Test
void deactivateUrl_ShouldThrowException_WhenUserDoesNotOwnUrl() {
    // Given
    User owner = new User();
    owner.setUsername("owner");

    User attacker = new User();
    attacker.setUsername("attacker");

    Url url = new Url();
    url.setShortUrl("abc123");
    url.setUser(owner);

    when(urlRepository.findByShortUrl("abc123")).thenReturn(Optional.of(url));

    // When / Then
    assertThrows(UnauthorizedAccessException.class, () ->
        urlService.deactivateUrl("abc123", "attacker")
    );
}
```

**Commit:** `fix: add authorization check for URL deactivation`

---

### **Day 2: Rate Limiting & CORS (6 hours)**

#### Task 1.3: Implement Rate Limiting with Bucket4j (4 hours)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

```java
// Create RateLimitFilter.java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = getClientKey(request);
        Bucket bucket = resolveBucket(key);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write(
                "{\"error\": \"Too many requests. Please try again later.\"}"
            );
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        // 100 requests per minute per user/IP
        Bandwidth limit = Bandwidth.classic(
            100,
            Refill.intervally(100, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private String getClientKey(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        }
        return request.getRemoteAddr(); // Fallback to IP
    }
}
```

**Test:**
```java
// Add to UrlControllerIntegrationTest.java
@Test
void createUrl_ShouldReturn429_WhenRateLimitExceeded() throws Exception {
    // Make 101 requests quickly
    for (int i = 0; i <= 100; i++) {
        mockMvc.perform(post("/api/v1/urls")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"url\": \"https://example.com/" + i + "\"}"))
            .andExpect(i < 100 ?
                status().isOk() :
                status().isTooManyRequests()
            );
    }
}
```

**Commit:** `feat: add rate limiting to prevent abuse`

---

#### Task 1.4: Fix CORS Configuration (1 hour)

```java
// Update SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // BEFORE: configuration.setAllowedOrigins(Arrays.asList("*"));
    // AFTER: Specific origins only
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",           // Local dev
        "https://yourdomain.com",          // Production
        "https://staging.yourdomain.com"   // Staging
    ));

    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**Commit:** `fix: restrict CORS to specific origins`

---

### **Day 3: CSRF & Frontend Bug Fixes (6 hours)**

#### Task 1.5: Enable CSRF Protection (3 hours)

```java
// Update SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers("/api/v1/auth/**") // Only public endpoints
        )
        // ... rest of config

    return http.build();
}
```

```typescript
// Update auth.interceptor.ts in frontend
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let clonedReq = req;

    // Add JWT token
    const token = this.authService.getToken();
    if (token && !this.isPublicEndpoint(req.url)) {
        clonedReq = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });
    }

    // Add CSRF token
    const csrfToken = this.getCsrfToken();
    if (csrfToken) {
        clonedReq = clonedReq.clone({
            setHeaders: {
                'X-XSRF-TOKEN': csrfToken
            }
        });
    }

    return next.handle(clonedReq);
}

private getCsrfToken(): string | null {
    const name = 'XSRF-TOKEN=';
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        cookie = cookie.trim();
        if (cookie.startsWith(name)) {
            return cookie.substring(name.length);
        }
    }
    return null;
}
```

**Commit:** `feat: enable CSRF protection`

---

#### Task 1.6: Fix Frontend Auth Interceptor Bug (1 hour)

```typescript
// Fix in auth.interceptor.ts
// BEFORE:
// if (publicEndpoints.Contains(url)) {

// AFTER:
private isPublicEndpoint(url: string): boolean {
    const publicEndpoints = ['/api/v1/auth/login', '/api/v1/auth/register'];
    return publicEndpoints.includes(url) ||
           publicEndpoints.some(endpoint => url.includes(endpoint));
}
```

**Commit:** `fix: correct JavaScript method from Contains to includes`

---

### **Day 4-5: Comprehensive Testing Setup (16 hours)**

#### Task 1.7: Set Up Test Infrastructure (4 hours)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- JaCoCo for coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```java
// Create BaseIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

#### Task 1.8: Write Comprehensive Unit Tests (8 hours)

**Target: 80%+ coverage**

```java
// UrlServiceImplTest.java - Comprehensive coverage
@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock private UrlRepository urlRepository;
    @Mock private UserRepository userRepository;
    @Mock private UrlGeneratorFactory urlGeneratorFactory;
    @Mock private UrlGenerator urlGenerator;

    @InjectMocks private UrlServiceImpl urlService;

    private User testUser;
    private Url testUrl;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();

        testUrl = Url.builder()
            .id(1L)
            .originalUrl("https://example.com")
            .shortUrl("abc123")
            .user(testUser)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .clickCount(0)
            .deactivated(false)
            .build();
    }

    @Test
    void createShortUrl_Success() {
        // Given
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://example.com");
        request.setExpirationDays(7);

        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(testUser));
        when(urlGeneratorFactory.getGenerator(any()))
            .thenReturn(urlGenerator);
        when(urlGenerator.generateShortUrl())
            .thenReturn("abc123");
        when(urlRepository.existsByShortUrl("abc123"))
            .thenReturn(false);
        when(urlRepository.save(any(Url.class)))
            .thenReturn(testUrl);

        // When
        UrlResponseDto result = urlService.createShortUrl(request, "testuser");

        // Then
        assertNotNull(result);
        assertEquals("abc123", result.getShortUrl());
        assertEquals("https://example.com", result.getOriginalUrl());
        assertEquals(0, result.getClickCount());
        assertFalse(result.isDeactivated());

        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void createShortUrl_HandlesCollision() {
        // Test collision handling with do-while loop
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://example.com");

        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(testUser));
        when(urlGeneratorFactory.getGenerator(any()))
            .thenReturn(urlGenerator);
        when(urlGenerator.generateShortUrl())
            .thenReturn("abc123", "def456");
        when(urlRepository.existsByShortUrl("abc123"))
            .thenReturn(true);  // First attempt collides
        when(urlRepository.existsByShortUrl("def456"))
            .thenReturn(false); // Second attempt succeeds
        when(urlRepository.save(any(Url.class)))
            .thenReturn(testUrl);

        // When
        urlService.createShortUrl(request, "testuser");

        // Then
        verify(urlGenerator, times(2)).generateShortUrl();
    }

    @Test
    void getOriginalUrl_Success_IncrementsClickCount() {
        // Given
        when(urlRepository.findByShortUrl("abc123"))
            .thenReturn(Optional.of(testUrl));
        when(urlRepository.save(any(Url.class)))
            .thenReturn(testUrl);

        // When
        UrlResponseDto result = urlService.getOriginalUrl("abc123");

        // Then
        assertEquals(1, testUrl.getClickCount());
        verify(urlRepository).save(testUrl);
    }

    @Test
    void getOriginalUrl_ThrowsException_WhenNotFound() {
        // Given
        when(urlRepository.findByShortUrl("nonexistent"))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(UrlNotFoundException.class, () ->
            urlService.getOriginalUrl("nonexistent")
        );
    }

    @Test
    void getOriginalUrl_ThrowsException_WhenDeactivated() {
        // Given
        testUrl.setDeactivated(true);
        when(urlRepository.findByShortUrl("abc123"))
            .thenReturn(Optional.of(testUrl));

        // When / Then
        assertThrows(UrlDeactivatedException.class, () ->
            urlService.getOriginalUrl("abc123")
        );
    }

    @Test
    void getOriginalUrl_ThrowsException_WhenExpired() {
        // Given
        testUrl.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(urlRepository.findByShortUrl("abc123"))
            .thenReturn(Optional.of(testUrl));

        // When / Then
        assertThrows(UrlExpiredException.class, () ->
            urlService.getOriginalUrl("abc123")
        );
    }

    @Test
    void deactivateUrl_Success() {
        // Given
        when(urlRepository.findByShortUrl("abc123"))
            .thenReturn(Optional.of(testUrl));
        when(urlRepository.save(any(Url.class)))
            .thenReturn(testUrl);

        // When
        urlService.deactivateUrl("abc123", "testuser");

        // Then
        assertTrue(testUrl.getDeactivated());
        verify(urlRepository).save(testUrl);
    }

    @Test
    void deactivateUrl_ThrowsException_WhenNotOwner() {
        // Given
        when(urlRepository.findByShortUrl("abc123"))
            .thenReturn(Optional.of(testUrl));

        // When / Then
        assertThrows(UnauthorizedAccessException.class, () ->
            urlService.deactivateUrl("abc123", "attacker")
        );

        verify(urlRepository, never()).save(any());
    }

    @Test
    void getAllUserUrls_ReturnsEmptyList_WhenUsernameEmpty() {
        // When
        List<UrlResponseDto> result = urlService.getAllUserUrls("");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllUserUrls_ReturnsUrls_OrderedByCreatedAtDesc() {
        // Given
        Url url1 = createUrl(1L, "abc", LocalDateTime.now().minusDays(2));
        Url url2 = createUrl(2L, "def", LocalDateTime.now().minusDays(1));
        Url url3 = createUrl(3L, "ghi", LocalDateTime.now());

        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(testUser));
        when(urlRepository.findByUserOrderByCreatedAtDesc(testUser))
            .thenReturn(Arrays.asList(url3, url2, url1));

        // When
        List<UrlResponseDto> result = urlService.getAllUserUrls("testuser");

        // Then
        assertEquals(3, result.size());
        assertEquals("ghi", result.get(0).getShortUrl()); // Most recent first
    }

    private Url createUrl(Long id, String shortUrl, LocalDateTime createdAt) {
        return Url.builder()
            .id(id)
            .shortUrl(shortUrl)
            .originalUrl("https://example.com/" + shortUrl)
            .user(testUser)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusDays(7))
            .clickCount(0)
            .deactivated(false)
            .build();
    }
}
```

**Similar comprehensive tests needed for:**
- `AuthServiceImplTest.java`
- `UrlControllerTest.java` (with MockMvc)
- `AuthControllerTest.java` (with MockMvc)
- `AnalyticsServiceImplTest.java`

---

#### Task 1.9: Write Integration Tests (4 hours)

```java
// UrlControllerIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class UrlControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("$2a$10$hashedpassword")
            .build();
        testUser = userRepository.save(testUser);

        authToken = jwtUtil.generateToken(testUser.getUsername());
    }

    @Test
    void fullUrlCreationAndRetrievalFlow() throws Exception {
        // Create URL
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://example.com/very/long/url");
        request.setExpirationDays(7);

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortUrl").exists())
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/very/long/url"))
            .andExpect(jsonPath("$.clickCount").value(0))
            .andReturn();

        UrlResponseDto created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            UrlResponseDto.class
        );

        // Retrieve URL (simulates redirect)
        mockMvc.perform(get("/api/v1/urls/" + created.getShortUrl()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clickCount").value(1)); // Incremented
    }

    @Test
    void deactivateUrl_RequiresOwnership() throws Exception {
        // Create URL as testuser
        Url url = createTestUrl(testUser);

        // Create another user
        User otherUser = User.builder()
            .username("otheruser")
            .email("other@example.com")
            .password("password")
            .build();
        otherUser = userRepository.save(otherUser);
        String otherToken = jwtUtil.generateToken(otherUser.getUsername());

        // Try to deactivate as otheruser - should fail
        mockMvc.perform(delete("/api/v1/urls/" + url.getShortUrl())
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());

        // Verify URL is still active
        Url stillActive = urlRepository.findByShortUrl(url.getShortUrl()).get();
        assertFalse(stillActive.getDeactivated());

        // Deactivate as owner - should succeed
        mockMvc.perform(delete("/api/v1/urls/" + url.getShortUrl())
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isNoContent());

        // Verify URL is deactivated
        Url deactivated = urlRepository.findByShortUrl(url.getShortUrl()).get();
        assertTrue(deactivated.getDeactivated());
    }

    private Url createTestUrl(User user) {
        Url url = Url.builder()
            .originalUrl("https://example.com")
            .shortUrl("test123")
            .user(user)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .clickCount(0)
            .deactivated(false)
            .build();
        return urlRepository.save(url);
    }
}
```

**Commit:** `test: add comprehensive test coverage (80%+)`

---

### **Sprint 1 Deliverables (End of Week 1)**

✅ All P0 security issues fixed
✅ 80%+ test coverage achieved
✅ Authorization bug fixed
✅ Rate limiting implemented
✅ CORS properly configured
✅ CSRF enabled
✅ Frontend bug fixed

**Interview Impact:** Project now "acceptable" for senior role

---

## **SPRINT 2: Database Migration & Infrastructure (Week 2 - Days 6-10)**

**Goal:** Move from H2 to production database and set up Docker

### **Day 6: PostgreSQL Migration (8 hours)**

#### Task 2.1: Set Up PostgreSQL (2 hours)

```yaml
# docker-compose.yml (create new file)
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: urlshortener-postgres
    environment:
      POSTGRES_DB: urlshortener
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

```xml
<!-- Update pom.xml - Replace H2 with PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
# Update application.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/urlshortener}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Changed from create-drop
    properties:
      hibernate:
        format_sql: true
        show_sql: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    drop-first: false  # CRITICAL: Changed from true!
```

**Commit:** `feat: migrate from H2 to PostgreSQL`

---

#### Task 2.2: Add Database Indexes (2 hours)

```xml
<!-- Create new Liquibase changelog: db/changelog/changes/003-add-indexes.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="003-add-performance-indexes" author="manvi">
        <comment>Add indexes for better query performance</comment>

        <!-- Index on user_id for user's URLs query -->
        <createIndex indexName="idx_url_user_id" tableName="urls">
            <column name="user_id"/>
        </createIndex>

        <!-- Index on created_at for sorting -->
        <createIndex indexName="idx_url_created_at" tableName="urls">
            <column name="created_at"/>
        </createIndex>

        <!-- Composite index on user_id and created_at -->
        <createIndex indexName="idx_url_user_created" tableName="urls">
            <column name="user_id"/>
            <column name="created_at" descending="true"/>
        </createIndex>

        <!-- Index on expires_at for cleanup jobs -->
        <createIndex indexName="idx_url_expires_at" tableName="urls">
            <column name="expires_at"/>
        </createIndex>

        <!-- Composite index for active URLs -->
        <createIndex indexName="idx_url_active" tableName="urls">
            <column name="deactivated"/>
            <column name="expires_at"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

```xml
<!-- Update db.changelog-master.xml -->
<databaseChangeLog>
    <include file="db/changelog/changes/001-create-users-table.xml"/>
    <include file="db/changelog/changes/002-create-urls-table.xml"/>
    <include file="db/changelog/changes/003-add-indexes.xml"/>
</databaseChangeLog>
```

**Commit:** `perf: add database indexes for improved query performance`

---

### **Day 7-8: Docker Setup (12 hours)**

#### Task 2.3: Create Multi-Stage Dockerfile (4 hours)

```dockerfile
# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Production image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
```

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=production

# Production image with Nginx
FROM nginx:alpine

COPY --from=build /app/dist/url-shortener-ui /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

```nginx
# frontend/nginx.conf
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    server {
        listen 80;
        server_name localhost;
        root /usr/share/nginx/html;
        index index.html;

        # Enable gzip compression
        gzip on;
        gzip_types text/plain text/css application/json application/javascript;

        # Angular routing - serve index.html for all routes
        location / {
            try_files $uri $uri/ /index.html;
        }

        # API proxy
        location /api {
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

**Commit:** `build: add multi-stage Dockerfiles for backend and frontend`

---

#### Task 2.4: Complete Docker Compose Setup (4 hours)

```yaml
# docker-compose.yml (complete version)
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: urlshortener-postgres
    environment:
      POSTGRES_DB: ${DB_NAME:-urlshortener}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - urlshortener-network

  redis:
    image: redis:7-alpine
    container_name: urlshortener-redis
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - urlshortener-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: urlshortener-backend
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE:-dev}
      DATABASE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-urlshortener}
      DB_USER: ${DB_USER:-postgres}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be set}
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - urlshortener-network

  frontend:
    build:
      context: ./frontend/url-shortener-ui
      dockerfile: Dockerfile
    container_name: urlshortener-frontend
    ports:
      - "${FRONTEND_PORT:-80}:80"
    depends_on:
      - backend
    networks:
      - urlshortener-network

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  urlshortener-network:
    driver: bridge
```

```bash
# .env.example
# Database Configuration
DB_NAME=urlshortener
DB_USER=postgres
DB_PASSWORD=your_secure_password_here
DB_PORT=5432

# Redis Configuration
REDIS_PORT=6379

# Application Configuration
SPRING_PROFILE=prod
BACKEND_PORT=8080
FRONTEND_PORT=80

# Security (REQUIRED)
JWT_SECRET=your-super-secret-jwt-key-min-256-bits-long-change-this-in-production

# Optional: For production deployment
# DATABASE_URL=jdbc:postgresql://production-db:5432/urlshortener
```

```markdown
# README.md update

## Quick Start with Docker

**Prerequisites:** Docker and Docker Compose installed

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/url-shortener
cd url-shortener
```

### 2. Set up environment variables
```bash
cp .env.example .env
# Edit .env and set JWT_SECRET to a secure value
```

### 3. Start all services
```bash
docker-compose up -d
```

### 4. Access the application
- Frontend: http://localhost
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

### 5. Stop services
```bash
docker-compose down
```

### 6. Stop and remove all data
```bash
docker-compose down -v
```
```

**Commit:** `build: complete Docker Compose setup with all services`

---

### **Day 9-10: Monitoring & Observability (12 hours)**

#### Task 2.5: Add Spring Boot Actuator (2 hours)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# Update application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
  info:
    env:
      enabled: true
    java:
      enabled: true
    os:
      enabled: true
```

```java
// Create CustomHealthIndicator.java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("connection", "active")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

**Commit:** `feat: add Spring Boot Actuator with health checks`

---

#### Task 2.6: Add Prometheus & Grafana (4 hours)

```yaml
# Update docker-compose.yml - add these services

  prometheus:
    image: prom/prometheus:latest
    container_name: urlshortener-prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - urlshortener-network

  grafana:
    image: grafana/grafana:latest
    container_name: urlshortener-grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin}
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    depends_on:
      - prometheus
    networks:
      - urlshortener-network

volumes:
  prometheus_data:
  grafana_data:
```

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'url-shortener'
```

```yaml
# grafana/datasources/datasource.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

**Commit:** `feat: add Prometheus and Grafana for monitoring`

---

#### Task 2.7: Add Structured Logging (2 hours)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- Create logback-spring.xml in src/main/resources -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="springAppName" source="spring.application.name"/>

    <!-- Console Appender with JSON formatting -->
    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${springAppName}"}</customFields>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
            </fieldNames>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON"/>
        <appender-ref ref="FILE"/>
    </root>

    <logger name="com.urlshortener" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate" level="WARN"/>
</configuration>
```

```java
// Add correlation ID filter
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}

// Update controllers to use MDC logging
@Slf4j
@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    @GetMapping("/{shortUrl}")
    public ResponseEntity<UrlResponseDto> getOriginalUrl(
            @PathVariable String shortUrl) {

        log.info("Redirect request received for shortUrl: {}", shortUrl);

        try {
            UrlResponseDto url = urlService.getOriginalUrl(shortUrl);
            log.info("Redirect successful for shortUrl: {}, originalUrl: {}",
                shortUrl, url.getOriginalUrl());
            return ResponseEntity.ok(url);
        } catch (UrlNotFoundException e) {
            log.warn("Redirect failed - URL not found: {}", shortUrl);
            throw e;
        } catch (UrlExpiredException e) {
            log.warn("Redirect failed - URL expired: {}", shortUrl);
            throw e;
        } catch (UrlDeactivatedException e) {
            log.warn("Redirect failed - URL deactivated: {}", shortUrl);
            throw e;
        }
    }
}
```

**Commit:** `feat: add structured JSON logging with correlation IDs`

---

### **Sprint 2 Deliverables (End of Week 2)**

✅ PostgreSQL database configured
✅ Database indexes added
✅ Docker & Docker Compose setup
✅ One-command local startup
✅ Health checks implemented
✅ Prometheus metrics exposed
✅ Grafana dashboards ready
✅ Structured logging with correlation IDs

**Interview Impact:** Project shows "production thinking"

---

## **SPRINT 3: Microservices Architecture (Week 3-4 - Days 11-20)**

**Goal:** Convert monolith to microservices - MOST IMPORTANT FOR INTERVIEW

### **Day 11-12: Plan Microservices Split (8 hours)**

#### Task 3.1: Define Service Boundaries (4 hours)

Create architecture document:

```markdown
# docs/microservices-architecture.md

## Service Split Strategy

### 1. Auth Service
**Responsibilities:**
- User registration
- Login/logout
- JWT token issuance and validation
- User profile management

**Database:** users table
**Port:** 8081

---

### 2. URL Service
**Responsibilities:**
- Create short URLs
- List user's URLs
- Deactivate URLs
- URL validation

**Database:** urls table
**Port:** 8082

---

### 3. Analytics Service
**Responsibilities:**
- Click tracking
- Statistics generation
- Analytics reports

**Database:** analytics table (time-series)
**Port:** 8083

---

### 4. API Gateway
**Responsibilities:**
- Routing
- Authentication
- Rate limiting
- CORS handling
- Load balancing

**Port:** 8080 (external facing)

---

### 5. Service Discovery (Eureka)
**Port:** 8761

---

### 6. Config Server
**Port:** 8888

---

## Communication Patterns

### Synchronous (REST):
- API Gateway → Auth Service (token validation)
- API Gateway → URL Service (CRUD operations)
- API Gateway → Analytics Service (read stats)

### Asynchronous (Kafka):
- URL Service → Analytics Service (click events)
- Auth Service → All Services (user events)

---

## Database Strategy

### Option 1: Database per Service (Recommended)
- auth_db: PostgreSQL (user data)
- url_db: PostgreSQL (URL data)
- analytics_db: TimescaleDB (time-series analytics)

### Option 2: Shared Database (Simpler for MVP)
- Keep single PostgreSQL
- Strict service boundaries (each service only accesses its tables)

**Decision: Option 2 for MVP, can migrate to Option 1 later**
```

**Commit:** `docs: add microservices architecture design`

---

#### Task 3.2: Set Up Project Structure (4 hours)

```bash
# Restructure project
urlShortner/
├── api-gateway/
│   ├── src/
│   └── pom.xml
├── auth-service/
│   ├── src/
│   └── pom.xml
├── url-service/
│   ├── src/
│   └── pom.xml
├── analytics-service/
│   ├── src/
│   └── pom.xml
├── service-discovery/
│   ├── src/
│   └── pom.xml
├── config-server/
│   ├── src/
│   └── pom.xml
├── shared-library/  # Common DTOs, exceptions
│   ├── src/
│   └── pom.xml
├── frontend/
├── docker-compose.yml
└── pom.xml (parent POM)
```

```xml
<!-- Parent pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.urlshortener</groupId>
    <artifactId>url-shortener-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>shared-library</module>
        <module>service-discovery</module>
        <module>config-server</module>
        <module>api-gateway</module>
        <module>auth-service</module>
        <module>url-service</module>
        <module>analytics-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
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
        </dependencies>
    </dependencyManagement>
</project>
```

---

### **Day 13-15: Implement Service Discovery & Config Server (12 hours)**

#### Task 3.3: Eureka Service Discovery (4 hours)

```xml
<!-- service-discovery/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>
```

```java
// ServiceDiscoveryApplication.java
@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}
```

```yaml
# service-discovery/application.yml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

---

#### Task 3.4: Config Server (4 hours)

```xml
<!-- config-server/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-server</artifactId>
    </dependency>
</dependencies>
```

```java
// ConfigServerApplication.java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

---

### **Day 16-18: Implement Individual Microservices (16 hours)**

#### Task 3.5: Auth Service (6 hours)

Extract auth functionality from monolith into auth-service

```xml
<!-- auth-service/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>com.urlshortener</groupId>
        <artifactId>shared-library</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

#### Task 3.6: URL Service (6 hours)

Extract URL functionality

---

#### Task 3.7: Analytics Service (4 hours)

Extract analytics functionality

---

### **Day 19-20: API Gateway & Integration (12 hours)**

#### Task 3.8: Spring Cloud Gateway (8 hours)

```xml
<!-- api-gateway/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

```java
// GatewayConfig.java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri("lb://AUTH-SERVICE"))
            .route("url-service", r -> r
                .path("/api/v1/urls/**")
                .filters(f -> f.filter(authenticationFilter.apply(new Config())))
                .uri("lb://URL-SERVICE"))
            .route("analytics-service", r -> r
                .path("/api/v1/analytics/**")
                .filters(f -> f.filter(authenticationFilter.apply(new Config())))
                .uri("lb://ANALYTICS-SERVICE"))
            .build();
    }
}
```

---

### **Sprint 3 Deliverables (End of Week 3-4)**

✅ Microservices architecture implemented
✅ Service discovery (Eureka)
✅ API Gateway (Spring Cloud Gateway)
✅ 4 independent microservices
✅ Inter-service communication
✅ Kafka for async events

**Interview Impact:** NOW you can talk about microservices!

---

## **SPRINT 4: Caching & Performance (Week 5 - Days 21-25)**

### **Day 21-22: Redis Caching (12 hours)**

#### Task 4.1: Add Redis to URL Service (6 hours)

```xml
<!-- url-service/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration())
            .transactionAware()
            .build();
    }
}

// Update UrlServiceImpl
@Service
public class UrlServiceImpl implements UrlService {

    @Cacheable(value = "urls", key = "#shortUrl")
    public UrlResponseDto getOriginalUrl(String shortUrl) {
        // This hits DB only on cache miss
    }

    @CacheEvict(value = "urls", key = "#shortUrl")
    public void deactivateUrl(String shortUrl, String username) {
        // Invalidates cache on update
    }
}
```

**Commit:** `feat: add Redis caching for URL lookups`

---

### **Day 23-25: Performance Testing & Optimization (16 hours)**

#### Task 4.2: Add Gatling Load Tests (8 hours)

```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>4.7.0</version>
</plugin>
```

```scala
// src/test/scala/simulations/RedirectSimulation.scala
class RedirectSimulation extends Simulation {

    val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")

    val scn = scenario("Redirect Load Test")
        .exec(
            http("Get Short URL")
                .get("/api/v1/urls/abc123")
                .check(status.is(200))
        )

    setUp(
        scn.inject(
            nothingFor(5 seconds),
            atOnceUsers(10),
            rampUsers(100) during (30 seconds),
            constantUsersPerSec(200) during (60 seconds),
            rampUsers(500) during (30 seconds)
        )
    ).protocols(httpProtocol)
    .assertions(
        global.responseTime.max.lt(100),
        global.responseTime.mean.lt(10),
        global.successfulRequests.percent.gt(99)
    )
}
```

**Run and document results:**
```bash
mvn gatling:test
# Results in target/gatling/redirectsimulation-*/index.html
```

**Commit:** `test: add Gatling performance tests`

---

### **Sprint 4 Deliverables (End of Week 5)**

✅ Redis caching implemented
✅ Cache hit rate: 80%+
✅ Load testing completed
✅ Performance benchmarks documented
✅ Query optimization done

**Interview Talking Point:** "I optimized redirect latency from 50ms to <10ms using Redis caching"

---

## **SPRINT 5: CI/CD & Cloud Deployment (Week 6 - Days 26-30)**

### **Day 26-27: GitHub Actions CI/CD (12 hours)**

#### Task 5.1: Create CI Pipeline (6 hours)

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Tests
        run: mvn clean test

      - name: Generate Coverage Report
        run: mvn jacoco:report

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml

      - name: SonarQube Analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  security:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: OWASP Dependency Check
        run: mvn dependency-check:check

      - name: Upload Dependency Check Report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: target/dependency-check-report.html

  build:
    needs: [test, security]
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker Images
        run: |
          docker build -t urlshortener-backend:${{ github.sha }} ./backend
          docker build -t urlshortener-frontend:${{ github.sha }} ./frontend

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Push Docker Images
        run: |
          docker push urlshortener-backend:${{ github.sha }}
          docker push urlshortener-frontend:${{ github.sha }}
```

**Commit:** `ci: add GitHub Actions CI pipeline`

---

### **Day 28-30: Cloud Deployment (16 hours)**

#### Task 5.2: Deploy to Cloud (Choose one)

**Option A: Heroku (Simplest)**
**Option B: AWS (Best for resume)**
**Option C: Railway/Render (Free tier)**

For AWS:

```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Deploy to ECS
        # ECS deployment steps
```

---

### **Sprint 5 Deliverables (End of Week 6)**

✅ CI/CD pipeline working
✅ Automated testing on every PR
✅ Code coverage reporting
✅ Security scanning
✅ Deployed to cloud with live URL

---

## **FINAL SPRINT: Documentation & Polish (Week 7-8)**

### **Day 31-35: Documentation (20 hours)**

#### Create/Update:
1. ✅ Professional README with architecture diagram
2. ✅ API documentation (Swagger)
3. ✅ Architecture Decision Records (ADRs)
4. ✅ Deployment guide
5. ✅ Performance benchmarks document

### **Day 36-40: Polish & Prepare Interview Talking Points (20 hours)**

1. ✅ Record demo video
2. ✅ Prepare system design answers
3. ✅ Document technical challenges & solutions
4. ✅ Create presentation slides
5. ✅ Practice explaining architecture

---

## 📊 FINAL CHECKLIST - Interview Ready

### **Must Have (Will be asked)**

- [ ] Live deployed URL
- [ ] 80%+ test coverage
- [ ] Microservices architecture (3+ services)
- [ ] Docker & Docker Compose
- [ ] Redis caching
- [ ] Database indexes
- [ ] All P0 security issues fixed
- [ ] CI/CD pipeline
- [ ] Monitoring (Prometheus/Grafana)
- [ ] Excellent README

### **Should Have (Impressive)**

- [ ] Load test results
- [ ] Kafka for async events
- [ ] Distributed tracing
- [ ] API documentation
- [ ] Performance benchmarks
- [ ] Architecture diagrams

### **Nice to Have (Stand out)**

- [ ] Kubernetes deployment
- [ ] Blog post about architecture
- [ ] Demo video
- [ ] E2E tests

---

## 🎯 TIMELINE SUMMARY

| Week | Focus | Deliverable | Interview Readiness |
|------|-------|-------------|---------------------|
| 1 | Security & Testing | All P0 fixed, 80% coverage | 40/100 - Acceptable |
| 2 | Infrastructure | Docker, PostgreSQL, Monitoring | 55/100 - Good |
| 3-4 | Microservices | 4+ services, API Gateway | 75/100 - Strong |
| 5 | Performance | Redis, Load tests | 85/100 - Very Strong |
| 6 | CI/CD & Cloud | Pipeline, Live URL | 90/100 - Excellent |
| 7-8 | Polish | Docs, Demo, Practice | 95/100 - Outstanding |

---

## 💡 FINAL RECOMMENDATIONS

### **If You Have < 2 Weeks:**
Focus on Week 1-2 only. Skip microservices. Perfect the monolith.

### **If You Have 1 Month:**
Complete Week 1-4. This gets you to "interview-ready" state.

### **If You Have 2 Months:**
Complete all 8 weeks. This gets you to "outstanding" state.

### **Alternative Path:**
Build new microservices project from scratch (e-commerce platform) - may be faster and cleaner.

---

## 🎤 INTERVIEW PREPARATION

### **Practice These Answers:**

1. **"Walk me through your architecture"**
   - Practice drawing on whiteboard
   - Know exact flow of a request
   - Explain why microservices

2. **"How does it scale?"**
   - Horizontal scaling strategy
   - Load balancing
   - Caching layer
   - Database sharding (even if not implemented)

3. **"What was your biggest technical challenge?"**
   - Prepare 2-3 STAR format stories
   - Show problem-solving process
   - Highlight learnings

4. **"How do you ensure quality?"**
   - Point to 80%+ test coverage
   - Show CI/CD pipeline
   - Explain testing strategy

5. **"Why did you choose X over Y?"**
   - Have rationale for every tech choice
   - Know alternatives and trade-offs

---

**Good luck! 🚀**
