# Resolucion Issue #70: Fix Dockerfile de incidents-service

> **Issue:** [#70 - Fix Dockerfile de incidents-service (Dockerfile.jvm -> Dockerfile)](https://github.com/monghithub/guidewire/issues/70)
> **Prioridad:** P0 (critica)
> **Tiempo estimado:** 10 minutos

## Diagnostico

El servicio `incidents-service` es un proyecto **Quarkus 3.8** que genera un Dockerfile distinto al patron Spring Boot usado por los demas servicios. El archivo se llama `Dockerfile.jvm` en vez de `Dockerfile`.

### Estado actual

```
components/
  billing-service/Dockerfile       # Spring Boot - OK
  camel-gateway/Dockerfile         # Spring Boot - OK
  drools-engine/Dockerfile         # Spring Boot - OK
  customers-service/Dockerfile     # Node.js    - OK
  incidents-service/Dockerfile.jvm # Quarkus    - PROBLEMA
```

### Por que falla

En `lab/podman/podman-compose.yml` (linea 239), el build no especifica `dockerfile:`:

```yaml
incidents-service:
  image: guidewire-poc/incidents-service:latest
  build:
    context: ../../components/incidents-service
  # No hay "dockerfile: Dockerfile.jvm"
  # Podman busca "Dockerfile" por defecto y no lo encuentra
```

### Diferencia con los otros Dockerfiles

El `Dockerfile.jvm` de incidents-service usa un patron Quarkus (copia `quarkus-app/` con subdirectorios `lib/`, `app/`, `quarkus/`), mientras que los Dockerfiles Spring Boot copian un unico JAR fat:

| Aspecto | Spring Boot (billing, camel, drools) | Quarkus (incidents) |
|---------|--------------------------------------|---------------------|
| Build output | `target/*.jar` (fat JAR) | `target/quarkus-app/` (directorio) |
| COPY en runtime | `COPY --from=build /app/target/*.jar app.jar` | 4 COPYs: `lib/`, `*.jar`, `app/`, `quarkus/` |
| ENTRYPOINT | `java -jar app.jar` | `java -jar /app/quarkus-run.jar` |
| Puerto | 8082/8083/8085 | 8084 |

## Solucion

### Paso 1: Renombrar el archivo

```bash
cd components/incidents-service
git mv Dockerfile.jvm Dockerfile
```

> **Por que renombrar y no modificar `podman-compose.yml`?**
> Mantener consistencia con los otros 4 servicios. Todos usan `Dockerfile` como nombre.

### Paso 2: Verificar que el contenido es correcto

El `Dockerfile.jvm` actual ya tiene un multi-stage build funcional. Tras el rename, el contenido queda:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

ARG JAVA_PACKAGE=java-21-openjdk-headless
ARG RUN_JAVA_VERSION=1.3.8

ENV LANGUAGE='en_US:en'

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
```

### Paso 3: Limpiar ARGs innecesarios

Los argumentos `JAVA_PACKAGE` y `RUN_JAVA_VERSION` no se usan en ningun lugar del Dockerfile. Se pueden eliminar para mayor claridad:

```dockerfile
# Eliminar estas 2 lineas (no se referencian):
ARG JAVA_PACKAGE=java-21-openjdk-headless
ARG RUN_JAVA_VERSION=1.3.8
```

### Paso 4: Verificar que podman-compose reconoce el build

No se requiere ningun cambio en `lab/podman/podman-compose.yml`. La configuracion actual ya es correcta una vez que el archivo se llame `Dockerfile`:

```yaml
incidents-service:
  image: guidewire-poc/incidents-service:latest
  build:
    context: ../../components/incidents-service
    # Podman busca "Dockerfile" por defecto - ahora lo encuentra
```

## Verificacion

### Test local (sin VM)

```bash
# Desde la raiz del proyecto
cd components/incidents-service

# Verificar que el Dockerfile existe
ls -la Dockerfile

# Build de prueba (requiere Docker o Podman)
podman build -t incidents-service-test .

# Verificar que la imagen se creo
podman images | grep incidents-service-test
```

### Test en la VM

```bash
# Rebuild y desplegar en CRC
oc start-build incidents-service -n guidewire-apps \
  --from-dir=components/incidents-service --follow

# Verificar que levanta
oc get pods -n guidewire-apps -l app=incidents-service
oc logs -f deploy/incidents-service -n guidewire-apps

# Verificar healthcheck
curl -sk https://incidents-service-guidewire-apps.apps-crc.testing/q/health
```

### Checklist de validacion

- [ ] `Dockerfile.jvm` renombrado a `Dockerfile`
- [ ] `oc start-build incidents-service -n guidewire-apps --from-dir=components/incidents-service --follow` termina sin errores
- [ ] El pod de incidents-service pasa a estado Ready en guidewire-apps
- [ ] `curl -sk https://incidents-service-guidewire-apps.apps-crc.testing/q/health` devuelve status UP
- [ ] Los otros 4 servicios siguen buildeando correctamente

## Comandos de commit

```bash
git mv components/incidents-service/Dockerfile.jvm components/incidents-service/Dockerfile
git commit -m "fix(incidents-service): rename Dockerfile.jvm to Dockerfile

Podman-compose expects 'Dockerfile' by default. The Quarkus-generated
name 'Dockerfile.jvm' caused the container build to fail silently.

Closes #70"
```
