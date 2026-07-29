# ============================= build stage =============================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first (pom rarely changes relative to source).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the executable jar.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============================= run stage ===============================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user.
RUN addgroup -S app && adduser -S app -G app

# The single fat jar produced above (name is version-independent via the wildcard).
COPY --from=build /app/target/*.jar app.jar

# Reports are written here; mount a volume in production for persistence.
RUN mkdir -p /app/data/mentalist-reports && chown -R app:app /app
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    MENTALIST_REPORTS_DIR=/app/data/mentalist-reports

EXPOSE 8081

# Container-level health probe (compose/Dockploy also define their own).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
