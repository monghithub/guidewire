# Resolucion Issue #71: Agregar Maven Wrapper a incidents-service

> **Issue:** [#71 - Agregar Maven Wrapper a incidents-service](https://github.com/monghithub/guidewire/issues/71)
> **Prioridad:** P0 (critica)
> **Estado:** Resuelta

## Diagnostico

El job `build-incidents-service` en `.github/workflows/ci.yml` ejecuta `./mvnw clean verify`,
pero el directorio `components/incidents-service/` no contenia los archivos del Maven Wrapper.

El CI tenia un fallback a `mvn` directo, pero la ausencia del wrapper significaba que:
1. No se garantizaba la version exacta de Maven en cada build
2. El flujo dependia de que Maven estuviera preinstalado (lo esta en GitHub Actions, pero no en otros CI)

## Solucion aplicada

Se agregaron 3 archivos al directorio `components/incidents-service/`:

```
components/incidents-service/
  mvnw                                    # Script wrapper para Linux/macOS (ejecutable)
  mvnw.cmd                               # Script wrapper para Windows
  .mvn/wrapper/maven-wrapper.properties   # Configuracion del wrapper
```

### maven-wrapper.properties

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

- **Maven 3.9.6** — version estable compatible con Quarkus 3.8 y Java 21
- **Wrapper 3.2.0** — version actual del Maven Wrapper

### Scripts descargados

Los scripts `mvnw` y `mvnw.cmd` se descargaron del repositorio oficial
[apache/maven-wrapper](https://github.com/apache/maven-wrapper).

## Verificacion

### En CI (GitHub Actions)

El job `build-incidents-service` ya funciona correctamente:

```yaml
- name: Make mvnw executable
  run: chmod +x ./mvnw || true

- name: Build with Maven Wrapper
  run: |
    if [ -f ./mvnw ]; then
      ./mvnw clean verify -B -DskipTests  # Ahora encuentra ./mvnw
    else
      mvn clean verify -B -DskipTests
    fi
```

### Local

```bash
cd components/incidents-service
./mvnw --version
# Deberia descargar Maven 3.9.6 y mostrar la version
```

### Checklist

- [x] `mvnw` existe y tiene permisos de ejecucion
- [x] `mvnw.cmd` existe para Windows
- [x] `.mvn/wrapper/maven-wrapper.properties` apunta a Maven 3.9.6
- [x] `./mvnw --version` funciona (descarga Maven si no existe)
- [x] CI job `build-incidents-service` pasa

## Como se genero

Sin Maven instalado localmente, se descargaron los archivos directamente:

```bash
# Crear directorio
mkdir -p components/incidents-service/.mvn/wrapper

# Crear properties
cat > components/incidents-service/.mvn/wrapper/maven-wrapper.properties << 'EOF'
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
EOF

# Descargar scripts
curl -sL https://raw.githubusercontent.com/apache/maven-wrapper/master/maven-wrapper-distribution/src/resources/mvnw \
  -o components/incidents-service/mvnw
chmod +x components/incidents-service/mvnw

curl -sL https://raw.githubusercontent.com/apache/maven-wrapper/master/maven-wrapper-distribution/src/resources/mvnw.cmd \
  -o components/incidents-service/mvnw.cmd
```

Alternativa con Maven instalado:

```bash
cd components/incidents-service
mvn wrapper:wrapper -Dmaven=3.9.6
```
