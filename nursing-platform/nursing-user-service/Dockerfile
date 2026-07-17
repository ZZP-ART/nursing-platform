# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# === Layer 1: Maven reactor descriptors ===
COPY .mvn ./.mvn
COPY pom.xml ./
COPY nursing-common/pom.xml ./nursing-common/
COPY nursing-gateway/pom.xml ./nursing-gateway/
COPY nursing-user-service/pom.xml ./nursing-user-service/
COPY nursing-catalog-service/pom.xml ./nursing-catalog-service/
COPY nursing-order-service/pom.xml ./nursing-order-service/
COPY nursing-feedback-service/pom.xml ./nursing-feedback-service/
COPY nursing-operations-service/pom.xml ./nursing-operations-service/
RUN --mount=type=cache,id=nursing-user-service-maven-repo,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress dependency:go-offline \
        -pl nursing-user-service -am

# === Layer 2: required source modules ===
COPY nursing-common/src ./nursing-common/src/
COPY nursing-user-service/src ./nursing-user-service/src/
COPY docker-compose/Healthcheck.java ./Healthcheck.java
RUN --mount=type=cache,id=nursing-user-service-maven-repo,target=/root/.m2 \
    javac -d /workspace/healthcheck /workspace/Healthcheck.java && \
    mvn --batch-mode --no-transfer-progress -pl nursing-user-service -am \
        -Dmaven.test.skip=true -DskipTests package


FROM eclipse-temurin:21-jre AS runtime

ARG APP_VERSION=dev
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.title="nursing-user-service" \
      org.opencontainers.image.description="Nursing platform user service" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=Asia/Shanghai \
    NURSING_UPLOAD_DIR=/app/data/uploads \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"

WORKDIR /app

RUN groupadd --gid 10001 appgroup && useradd --uid 10001 --gid appgroup --shell /usr/sbin/nologin --create-home appuser && \
    mkdir -p /app/data/uploads && chown -R appuser:appgroup /app

COPY --from=build --chown=10001:10001 /workspace/healthcheck/Healthcheck.class /app/Healthcheck.class
COPY --from=build --chown=10001:10001 /workspace/nursing-user-service/target/nursing-user-service-*.jar /app/app.jar

USER 10001:10001

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["java", "-cp", "/app", "Healthcheck", "http://127.0.0.1:8081/actuator/health"]

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
