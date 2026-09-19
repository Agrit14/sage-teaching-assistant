# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Build stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy application source and build the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Production runtime stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="sage-teaching-assistant"
LABEL description="Sage Teaching Assistant Spring Boot Application"

WORKDIR /app

# Create a dedicated non-root user and group
RUN groupadd -r sage && useradd -r -g sage sage

# Copy compiled JAR from the builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

# Ensure runtime file storage directory exists with proper ownership
RUN mkdir -p /app/data/files && chown -R sage:sage /app

USER sage:sage

ENV SERVER_PORT=8081
EXPOSE 8081
VOLUME /app/data/files

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
