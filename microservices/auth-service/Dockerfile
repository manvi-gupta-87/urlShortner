FROM eclipse-temurin:17-jre
WORKDIR /app
COPY microservices/auth-service/auth-service-app/target/auth-service-app-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
