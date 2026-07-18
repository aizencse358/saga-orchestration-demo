# Build context must be the repo root (so the parent pom is available), e.g.:
#   docker build -f orchestrator-custom/Dockerfile -t saga-demo/orchestrator-custom .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl orchestrator-custom -am -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/orchestrator-custom/target/orchestrator-custom-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
