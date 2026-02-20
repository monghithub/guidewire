# Resolucion Issue #78: Agregar OpenAPI codegen a incidents-service

> **Issue:** [#78 - Agregar OpenAPI codegen a incidents-service](https://github.com/monghithub/guidewire/issues/78)
> **Prioridad:** P0
> **Estado:** Resuelta

## Diagnostico

El servicio incidents-service (Quarkus 3.8) no tenia ninguna configuracion de generacion
de codigo desde OpenAPI. El `IncidentResource` era un JAX-RS resource standalone con
anotaciones manuales.

La spec `contracts/openapi/incidents-service-api.yml` existia pero no se usaba.

## Solucion aplicada

Agregado `openapi-generator-maven-plugin` al `components/incidents-service/pom.xml`:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/../../contracts/openapi/incidents-service-api.yml</inputSpec>
                <generatorName>jaxrs-spec</generatorName>
                <apiPackage>com.guidewire.incidents.api</apiPackage>
                <modelPackage>com.guidewire.incidents.model.generated</modelPackage>
                <configOptions>
                    <interfaceOnly>true</interfaceOnly>
                    <useJakartaEe>true</useJakartaEe>
                    <useTags>true</useTags>
                    <returnResponse>true</returnResponse>
                    <skipDefaultInterface>true</skipDefaultInterface>
                </configOptions>
                <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Diferencias con los servicios Spring Boot

| Config | billing/camel/drools (Spring) | incidents (Quarkus) |
|--------|-------------------------------|---------------------|
| Generator | `spring` | `jaxrs-spec` |
| Jakarta EE | via Spring Boot 3 | `useJakartaEe: true` |
| Return type | ResponseEntity | `returnResponse: true` (JAX-RS Response) |

## Siguiente paso

Ejecutar `mvn generate-sources` y hacer que `IncidentResource implements IncidentsApi`.

## Verificacion

```bash
cd components/incidents-service
./mvnw generate-sources -B
ls target/generated-sources/openapi/
```
