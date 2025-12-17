FROM eclipse-temurin:17-jre
WORKDIR /app
COPY microservices/analytics-service/analytics-service-app/target/analytics-service-app-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
