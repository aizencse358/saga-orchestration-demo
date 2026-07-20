# Build context must be the repo root (so the parent pom is available), e.g.:
#   docker build -f load-generator/Dockerfile -t saga-demo/load-generator .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl load-generator -am -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/load-generator/target/load-generator-*.jar app.jar
COPY load-generator/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]
