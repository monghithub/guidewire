# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mkdir -p /contracts/avro /contracts/openapi && \
    apk add --no-cache maven && \
    mvn clean package -DskipTests -Dopenapi.generator.maven.plugin.skip=true -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8084

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/app/quarkus-run.jar"

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
