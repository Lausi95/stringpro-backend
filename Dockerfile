# syntax=docker/dockerfile:1

# ---- Build stage ------------------------------------------------------------
# Self-contained build: compiles from source with the project's Gradle wrapper,
# so the image builds anywhere with no host prerequisites. JDK 21 matches the
# project's Java toolchain (build.gradle.kts).
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Copy the Gradle wrapper and build configuration first. Dependency resolution
# then caches in its own layer, reused on any build that doesn't touch the
# build files (works with GitHub Actions' type=gha layer cache).
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now bring in the source and assemble the executable jar. No tests / ktlint —
# packaging is not a CI gate (run those in CI later).
COPY src ./src
RUN ./gradlew --no-daemon bootJar \
    && cp build/libs/*.jar app.jar

# Split the fat jar into Spring Boot layers (dependencies, loader,
# snapshot-dependencies, application) so the rarely-changing dependency layer
# caches separately from application code on rebuilds. Boot 4.x 'tools' jarmode.
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- Runtime stage ----------------------------------------------------------
# Distroless: no shell or package manager (minimal attack surface); :nonroot
# runs as an unprivileged user (UID 65532) out of the box.
FROM gcr.io/distroless/java21-debian12:nonroot AS runtime
WORKDIR /app

# Copy the extracted layers dependency-first so a code-only change only rewrites
# the small final 'application' layer.
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

# Documentation only (matches server.port in application.yml).
EXPOSE 8080

# Exec form so the JVM is PID 1 and receives SIGTERM for graceful shutdown.
# The 'tools' jarmode application layer is a restructured jar launched with
# -jar; its loader resolves the sibling dependencies/ layer relatively.
# No active profile => production mode (JSON/Datadog logging). Runtime config
# (MONGODB_URI, MONGODB_USERNAME, MONGODB_PASSWORD) is supplied via env vars.
ENTRYPOINT ["java", "-jar", "app.jar"]
