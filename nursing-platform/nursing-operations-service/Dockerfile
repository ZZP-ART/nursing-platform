# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY .mvn ./.mvn
COPY pom.xml ./
COPY nursing-common/pom.xml ./nursing-common/
COPY nursing-gateway/pom.xml ./nursing-gateway/
COPY nursing-user-service/pom.xml ./nursing-user-service/
COPY nursing-catalog-service/pom.xml ./nursing-catalog-service/
COPY nursing-order-service/pom.xml ./nursing-order-service/
COPY nursing-feedback-service/pom.xml ./nursing-feedback-service/
COPY nursing-operations-service/pom.xml ./nursing-operations-service/
RUN --mount=type=cache,id=nursing-operations-service-maven-repo,target=/root/.m2 mvn --batch-mode --no-transfer-progress dependency:go-offline -pl nursing-operations-service -am
COPY nursing-common/src ./nursing-common/src/
COPY nursing-operations-service/src ./nursing-operations-service/src/
COPY docker-compose/Healthcheck.java ./Healthcheck.java
RUN --mount=type=cache,id=nursing-operations-service-maven-repo,target=/root/.m2 javac -d /workspace/healthcheck /workspace/Healthcheck.java && mvn --batch-mode --no-transfer-progress -pl nursing-operations-service -am -Dmaven.test.skip=true -DskipTests package
FROM eclipse-temurin:21-jre
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=Asia/Shanghai JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
WORKDIR /app
RUN groupadd --gid 10001 appgroup && useradd --uid 10001 --gid appgroup --shell /usr/sbin/nologin --create-home appuser && chown -R appuser:appgroup /app
COPY --from=build --chown=10001:10001 /workspace/healthcheck/Healthcheck.class /app/Healthcheck.class
COPY --from=build --chown=10001:10001 /workspace/nursing-operations-service/target/nursing-operations-service-*.jar /app/app.jar
USER 10001:10001
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD ["java", "-cp", "/app", "Healthcheck", "http://127.0.0.1:8085/actuator/health"]
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
