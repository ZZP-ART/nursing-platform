# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn ./.mvn
COPY pom.xml ./
COPY nursing-common ./nursing-common
COPY nursing-user-service ./nursing-user-service
COPY nursing-catalog-service ./nursing-catalog-service
COPY nursing-order-service ./nursing-order-service
COPY nursing-feedback-service ./nursing-feedback-service
COPY nursing-gateway ./nursing-gateway
COPY docker-compose/Healthcheck.java ./Healthcheck.java

RUN --mount=type=cache,id=nursing-gateway-maven-repo,target=/root/.m2 \
    javac -d /workspace/healthcheck /workspace/Healthcheck.java && \
    mvn --batch-mode --no-transfer-progress -pl nursing-gateway -am -DskipTests package


FROM eclipse-temurin:21-jre AS runtime

ARG APP_VERSION=dev
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.title="nursing-gateway" \
      org.opencontainers.image.description="Nursing platform API gateway" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"

WORKDIR /app

RUN groupadd --gid 10001 appgroup && \
    useradd --uid 10001 --gid appgroup --shell /usr/sbin/nologin --create-home appuser

COPY --from=build --chown=10001:10001 /workspace/healthcheck/Healthcheck.class /app/Healthcheck.class
COPY --from=build --chown=10001:10001 /workspace/nursing-gateway/target/nursing-gateway-*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["java", "-cp", "/app", "Healthcheck", "http://127.0.0.1:8080/actuator/health"]

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
