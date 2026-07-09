# syntax=docker/dockerfile:1.6
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG MODULE
COPY pom.xml .
COPY .mvn ./.mvn
COPY nursing-common ./nursing-common
COPY nursing-gateway ./nursing-gateway
COPY nursing-user-service ./nursing-user-service
COPY nursing-catalog-service ./nursing-catalog-service
COPY nursing-order-service ./nursing-order-service
COPY nursing-feedback-service ./nursing-feedback-service
COPY docker-compose/Healthcheck.java /workspace/Healthcheck.java
RUN --mount=type=cache,id=nursing-maven-repo,target=/root/.m2 \
    javac -d /workspace/healthcheck /workspace/Healthcheck.java && \
    mvn -pl ${MODULE} -am -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/healthcheck/Healthcheck.class /app/Healthcheck.class
COPY --from=build /workspace/${MODULE}/target/${MODULE}-*.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
