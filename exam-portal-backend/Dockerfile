# ============================= build stage =============================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first (pom rarely changes relative to source) — this
# layer only re-runs when pom.xml itself changes, not on every code edit.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the executable jar.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============================= run stage ===============================
# JRE (not full JDK) alpine image: smaller attack surface and image size than
# the build image; nothing here needs a compiler.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Single layer for user setup + writable data dir, owned by that user.
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data/mentalist-reports \
    && chown -R app:app /app

# The single fat jar produced above (name is version-independent via the wildcard).
COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    MENTALIST_REPORTS_DIR=/app/data/mentalist-reports \
    # Size the JVM heap as a fraction of the container's memory limit
    # (set in docker-compose.yml/Dockploy) instead of a fixed -Xmx, so it
    # adapts if the limit changes without touching this file.
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081

# Container-level health probe (compose/Dockploy also define their own).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/health || exit 1

# Shell form so JAVA_OPTS is expanded; exec via `exec` keeps java as PID 1
# for correct SIGTERM handling (needed for server.shutdown=graceful).
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
