FROM gradle:9.1.0-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts ./
COPY --chown=gradle:gradle common/ ./common/
COPY --chown=gradle:gradle ai_service/ ./ai_service/
COPY --chown=gradle:gradle bot_service/ ./bot_service/
RUN gradle :bot_service:bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/bot_service/build/libs/*.jar bot_service.jar
ENTRYPOINT ["java", "-jar", "bot_service.jar"]