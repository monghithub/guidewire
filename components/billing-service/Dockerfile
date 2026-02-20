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

COPY --from=build /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
