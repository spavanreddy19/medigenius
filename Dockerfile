# ==============================================================================
# MediGenius Backend - Multi-stage Dockerfile
# Stage 1: build the fat jar with Maven
# Stage 2: run it on a slim JRE 21 image
# ==============================================================================

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Leverage Docker layer caching: resolve dependencies before copying source
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for runtime security
RUN useradd -ms /bin/bash medigenius
USER medigenius

COPY --from=build /build/target/medigenius-backend.jar app.jar

# Directories referenced by application.properties (mounted as volumes in compose)
VOLUME ["/app/logs", "/app/storage", "/app/data"]

EXPOSE 8000

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
