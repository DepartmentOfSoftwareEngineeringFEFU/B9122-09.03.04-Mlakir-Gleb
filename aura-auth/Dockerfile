FROM mirror.gcr.io/library/maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY docker/maven-settings.xml /tmp/maven-settings.xml
COPY aura-exception aura-exception
COPY aura-auth aura-auth
COPY aura-core aura-core

RUN mvn -q -s /tmp/maven-settings.xml -pl aura-auth/aura-auth-service -am -DskipTests package

FROM mirror.gcr.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/aura-auth/aura-auth-service/target/aura-auth-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
