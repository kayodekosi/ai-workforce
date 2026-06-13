# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Pre-fetch dependencies (cached layer)
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ai-workforce.jar app.jar

# Directory for the persistent H2 database (mounted as a volume in compose)
RUN mkdir -p /app/data

# The port the app listens on. Override at runtime with -e SERVER_PORT=xxxx
ENV SERVER_PORT=8080
EXPOSE 8080

# SERVER_PORT maps to Spring's server.port property
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]
