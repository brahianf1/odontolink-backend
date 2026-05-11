# syntax=docker/dockerfile:1.7

# =============================================================================
# Stage 1 — Build
#   Uses Eclipse Temurin JDK 17 (official multi-arch image including linux/arm64).
#   Builds the Spring Boot fat jar and extracts it into layered directories so
#   the runtime image can copy each layer separately for better cache reuse.
# =============================================================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Copy Maven wrapper and project descriptor first to maximize cache hits on
# dependency resolution when only source files change.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B -ntp dependency:go-offline

# Copy sources and build. Tests are skipped here because CI runs them before
# deployment; the container build focuses on producing the deployable artifact.
COPY src src

RUN ./mvnw -B -ntp clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted


# =============================================================================
# Stage 2 — Runtime
#   Uses Eclipse Temurin JRE 17 (multi-arch, linux/arm64 included).
#   Runs as an unprivileged user, exposes the application on port 8080 and
#   reports liveness via Spring Boot Actuator.
# =============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl is required for the HEALTHCHECK; tini gives proper signal handling
# so the JVM receives SIGTERM and shuts down gracefully on Dokploy redeploys.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tini \
    && rm -rf /var/lib/apt/lists/*

# Unprivileged runtime user.
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --home-dir /app --shell /usr/sbin/nologin spring

WORKDIR /app

# Copy Spring Boot layers in order of expected change frequency (least to
# most volatile). Each COPY produces an independent image layer, so rebuilds
# only invalidate the layers that actually changed.
COPY --from=builder --chown=spring:spring /workspace/target/extracted/dependencies/         ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/spring-boot-loader/   ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/application/          ./

USER spring:spring

# Container-aware JVM tuning. MaxRAMPercentage lets the heap follow whatever
# memory limit Dokploy assigns from the UI, so no value is hard-coded.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Documents the in-container port. Dokploy/Traefik handles external routing.
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/usr/bin/tini", "--", "java", "org.springframework.boot.loader.launch.JarLauncher"]
