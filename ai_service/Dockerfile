FROM gradle:9.1.0-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar ai_service.jar
ENTRYPOINT ["java", "-jar", "ai_service.jar"]