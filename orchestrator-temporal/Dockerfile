# Build context must be the repo root (so the parent pom is available), e.g.:
#   docker build -f orchestrator-temporal/Dockerfile -t saga-demo/orchestrator-temporal .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl orchestrator-temporal -am -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/orchestrator-temporal/target/orchestrator-temporal-*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
