## syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY docker/maven-settings.xml /tmp/maven-settings.xml

COPY aura-exception/pom.xml aura-exception/pom.xml
COPY aura-exception/aura-exception-module/pom.xml aura-exception/aura-exception-module/pom.xml
COPY aura-exception/aura-exception-starter/pom.xml aura-exception/aura-exception-starter/pom.xml
COPY aura-auth/pom.xml aura-auth/pom.xml
COPY aura-auth/aura-auth-api/pom.xml aura-auth/aura-auth-api/pom.xml
COPY aura-auth/aura-auth-common/pom.xml aura-auth/aura-auth-common/pom.xml
COPY aura-auth/aura-auth-service/pom.xml aura-auth/aura-auth-service/pom.xml
COPY aura-auth/aura-auth-starter/pom.xml aura-auth/aura-auth-starter/pom.xml
COPY aura-core/pom.xml aura-core/pom.xml
COPY aura-core/aura-core-api/pom.xml aura-core/aura-core-api/pom.xml
COPY aura-core/aura-core-service/pom.xml aura-core/aura-core-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -s /tmp/maven-settings.xml -pl aura-core/aura-core-service -am -DskipTests dependency:go-offline

COPY aura-exception aura-exception
COPY aura-auth aura-auth
COPY aura-core aura-core

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -s /tmp/maven-settings.xml -pl aura-core/aura-core-service -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/aura-core/aura-core-service/target/aura-core-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
