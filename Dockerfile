# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Production runtime stage
# Uses pre-packaged Spring Boot JAR from Maven build for fast multi-platform builds
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="sage-teaching-assistant"
LABEL description="Sage Teaching Assistant Spring Boot Application"

WORKDIR /app

# Create a dedicated non-root user and group
RUN groupadd -r sage && useradd -r -g sage sage

# Copy compiled JAR produced by Maven build
ARG JAR_FILE=target/sage-teaching-assistant-*.jar
COPY ${JAR_FILE} /app/app.jar

# Ensure runtime file storage directory exists with proper ownership
RUN mkdir -p /app/data/files && chown -R sage:sage /app

USER sage:sage

ENV SERVER_PORT=8081
EXPOSE 8081
VOLUME /app/data/files

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
