# Multi-stage Docker build for Auth Service
# Stage 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Set working directory inside the container
# This is where all commands will execute
WORKDIR /app

# Copy parent POM first for dependency resolution
# Copying POM files separately leverages Docker layer caching
# If POMs don't change, Maven won't re-download dependencies
COPY pom.xml .
COPY microservices/auth-service/pom.xml microservices/auth-service/

# Download dependencies in a separate layer
# This layer will be cached if pom.xml doesn't change
# Saves time on rebuilds when only source code changes
RUN mvn dependency:go-offline -B -f microservices/auth-service/pom.xml

# Copy the source code
# Placed after dependency download to maximize cache utilization
COPY microservices/auth-service/src microservices/auth-service/src

# Build the application
# -DskipTests: Skip tests during Docker build (tests run in CI/CD)
# -B: Batch mode - non-interactive, better for containers
# clean: Remove previous builds
# package: Compile, test, and package into JAR
RUN mvn clean package -DskipTests -B -f microservices/auth-service/pom.xml

# Stage 2: Runtime stage using lightweight JRE
# Using JRE instead of JDK reduces image size by ~200MB
FROM eclipse-temurin:17-jre-alpine

# Add metadata labels for documentation and maintenance
LABEL maintainer="urlshortener-team"
LABEL service="auth-service"
LABEL version="1.0.0"

# Create a non-root user for security
# Running as root is a security risk
# alpine uses 'addgroup' and 'adduser' commands
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory for the application
WORKDIR /app

# Copy the JAR from build stage
# --from=build: Copy from the 'build' stage
# --chown: Set ownership to spring user
COPY --from=build --chown=spring:spring /app/microservices/auth-service/target/*.jar app.jar

# Switch to non-root user
# All subsequent commands and the container process run as this user
USER spring:spring

# Expose Auth service port
# This is documentation - doesn't actually publish the port
# Actual port mapping happens with 'docker run -p'
EXPOSE 8081

# Health check to ensure Auth Service is running
# Docker will periodically execute this command
# If it fails, container is marked unhealthy
# --interval: How often to check
# --timeout: How long to wait for check
# --start-period: Grace period before first check (longer for DB connectivity)
# --retries: Number of consecutive failures before unhealthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM options for production
# -Djava.security.egd: Faster startup with non-blocking random (critical for JWT)
# -XX:+UseContainerSupport: JVM aware of container limits
# -XX:MaxRAMPercentage: Use max 75% of container memory
# -XX:+UseG1GC: G1 Garbage Collector - better for microservices
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC"

# Run the application
# ENTRYPOINT: Always executed
# CMD: Default arguments, can be overridden
# Using shell form to allow variable expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
