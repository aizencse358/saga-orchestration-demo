# Build context must be the repo root (so the parent pom is available), e.g.:
#   docker build -f shipping-service/Dockerfile -t saga-demo/shipping-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl shipping-service -am -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/shipping-service/target/shipping-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
