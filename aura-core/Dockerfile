FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY aura-exception aura-exception
COPY aura-auth aura-auth
COPY aura-core aura-core

RUN mvn -q -pl aura-core/aura-core-service -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/aura-core/aura-core-service/target/aura-core-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
