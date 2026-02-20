# Resolucion Issue #73: Validar que el stack completo levanta end-to-end

> **Issue:** [#73 - Validar stack completo end-to-end (vagrant up + podman-compose up)](https://github.com/monghithub/guidewire/issues/73)
> **Prioridad:** P1
> **Estado:** Guia de validacion (requiere ejecucion manual)

## Contexto

Esta issue no tiene un "fix" de codigo — es un proceso de validacion manual que debe ejecutarse
en la VM del laboratorio. Este documento sirve como guia paso a paso.

## Prerequisitos

- Host con al menos 24 GB RAM (20 GB para VM + host OS)
- Vagrant instalado con libvirt/KVM
- Acceso a internet para descargar imagenes de contenedores

## Paso 1: Provisionar VM limpia

```bash
cd lab/

# Destruir VM existente si hay una
vagrant destroy -f

# Crear VM desde cero
vagrant up

# Verificar que la VM esta corriendo
vagrant status
```

**Resultado esperado:** VM Fedora 39 con Podman, JDK 21, Maven 3.9, Node.js 20 instalados.

### Verificacion del provisionamiento

```bash
vagrant ssh -c "podman --version"        # >= 4.x
vagrant ssh -c "java --version"          # 21.x
vagrant ssh -c "mvn --version"           # 3.9.x
vagrant ssh -c "node --version"          # v20.x
vagrant ssh -c "podman-compose version"  # >= 1.x
```

## Paso 2: Build de contenedores

```bash
vagrant ssh
cd /vagrant/lab/podman

# Build de todos los servicios
podman-compose build
```

### Posibles errores y soluciones

| Error | Causa | Solucion |
|-------|-------|----------|
| `Dockerfile not found` | Nombre incorrecto | Issue #70 ya corregida (Dockerfile.jvm -> Dockerfile) |
| `mvn: command not found` en Dockerfile | Maven no incluido en imagen base | Los Dockerfiles usan `apk add maven`, deberia funcionar |
| `npm ci` falla en customers-service | Falta package-lock.json | Ejecutar `npm install` primero para generarlo |
| Timeout descargando dependencias | Red lenta | Reintentar el build |

### Verificar que las 5 imagenes se crearon

```bash
podman images | grep guidewire-poc
# Debe mostrar:
# guidewire-poc/camel-gateway
# guidewire-poc/drools-engine
# guidewire-poc/billing-service
# guidewire-poc/incidents-service
# guidewire-poc/customers-service
```

## Paso 3: Levantar infraestructura

```bash
cd /vagrant/lab/podman

# Levantar en orden (podman-compose respeta depends_on)
podman-compose up -d
```

### Orden de arranque esperado

1. **Fase 1 — Datos y mensajeria:**
   - PostgreSQL (`:15432`) — healthcheck: `pg_isready`
   - Kafka KRaft (`:9092`) — healthcheck: broker API
   - ActiveMQ Artemis (`:61616`) — healthcheck: HTTP console

2. **Fase 2 — Plataforma:**
   - Kafdrop (`:9000`) — depende de Kafka
   - Apicurio (`:8086`) — depende de PostgreSQL
   - 3Scale APIcast (`:8000`) — configuracion local

3. **Fase 3 — Motores de integracion:**
   - Drools Engine (`:8085`) — depende de PostgreSQL
   - Camel Gateway (`:8083`) — depende de Kafka, ActiveMQ

4. **Fase 4 — Microservicios:**
   - Billing Service (`:8082`) — depende de PostgreSQL, Kafka
   - Incidents Service (`:8084`) — depende de PostgreSQL, Kafka
   - Customers Service (`:8081`) — depende de PostgreSQL, Kafka

### Verificar que todos los contenedores estan UP

```bash
podman-compose ps

# Todos deben estar en estado "Up" o "healthy"
```

## Paso 4: Verificar healthchecks

```bash
# Infraestructura
curl -s http://localhost:15432 | head -1                        # PostgreSQL (conexion directa)
curl -s http://localhost:9000                                    # Kafdrop UI
curl -s http://localhost:8086/apis/registry/v2/system/info       # Apicurio

# Motores
curl -s http://localhost:8083/actuator/health | jq .             # Camel Gateway
curl -s http://localhost:8085/actuator/health | jq .             # Drools Engine

# Microservicios
curl -s http://localhost:8082/actuator/health | jq .             # Billing Service
curl -s http://localhost:8084/q/health | jq .                    # Incidents Service (Quarkus)
curl -s http://localhost:8081/health | jq .                      # Customers Service (Express)
```

**Resultado esperado:** Todos devuelven `{"status":"UP"}` o equivalente.

## Paso 5: Verificar creacion de topics Kafka

```bash
# Via Kafdrop UI
curl -s http://localhost:9000/topic | jq .

# O directamente con kcat
kcat -b localhost:9092 -L | grep topic
```

**Topics esperados:**
- `billing.invoice-created`
- `billing.invoice-status-changed`
- `incidents.incident-created`
- `incidents.incident-status-changed`
- `customers.customer-registered`
- `customers.customer-status-changed`
- `guidewire.dlq`

## Paso 6: Test E2E con Postman/Newman

```bash
# Instalar Newman si no esta instalado
npm install -g newman

# Ejecutar la coleccion E2E
newman run /vagrant/contracts/postman/guidewire-e2e.postman_collection.json \
  -e /vagrant/contracts/postman/local.postman_environment.json \
  --reporters cli \
  --delay-request 1000
```

### Flujo del test E2E

1. Health checks de todos los servicios
2. Crear cliente via API de customers
3. Crear poliza via Camel Gateway -> PolicyCenter mock
4. Crear factura via API de billing
5. Crear reclamacion via Camel Gateway -> ClaimCenter mock
6. Verificar que los eventos Kafka se publicaron (via Kafdrop)
7. Verificar que Apicurio registro los schemas

## Paso 7: Verificar integracion Kafka

```bash
# Verificar que hay mensajes en los topics (via kcat)
kcat -b localhost:9092 -t billing.invoice-created -C -c 1 -o beginning
kcat -b localhost:9092 -t customers.customer-registered -C -c 1 -o beginning
```

## Problemas conocidos y mitigaciones

### 1. Kafka no esta listo cuando los microservicios arrancan

**Sintoma:** Los servicios logean `Connection refused` a Kafka.

**Mitigacion:** Los `depends_on` con `condition: service_healthy` deberian prevenir esto.
Si persiste, agregar retry en la configuracion de Kafka del servicio o aumentar el
`start_period` del healthcheck.

### 2. SELinux bloquea volumenes

**Sintoma:** `Permission denied` al montar volumenes.

**Mitigacion:** Usar `:Z` o `:z` en los volume mounts de `podman-compose.yml`.
El commit `9e241de` ya aborda esto.

### 3. Memoria insuficiente

**Sintoma:** Contenedores se reinician con OOMKilled.

**Mitigacion:** Reducir el pool de conexiones de PostgreSQL o limitar memoria por contenedor.
El Vagrantfile asigna 20 GB RAM, que deberia ser suficiente.

### 4. Quarkus tarda en arrancar

**Sintoma:** Incidents-service responde 503 durante los primeros segundos.

**Mitigacion:** Esperar 30-60 segundos tras `podman-compose up` antes de ejecutar tests.

## Checklist de validacion

- [ ] `vagrant up` completa sin errores
- [ ] `podman-compose build` crea las 5 imagenes de aplicacion
- [ ] `podman-compose up -d` levanta los 11+ contenedores
- [ ] Todos los healthchecks responden UP
- [ ] Los 7 topics de Kafka existen
- [ ] Newman E2E pasa todos los tests
- [ ] Los schemas Avro estan registrados en Apicurio
- [ ] No hay errores en los logs (`podman-compose logs --tail=50`)
