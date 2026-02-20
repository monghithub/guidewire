# Resolucion Issue #73: Validar que el stack completo levanta end-to-end

> **Issue:** [#73 - Validar stack completo end-to-end (CRC + deploy-all.sh)](https://github.com/monghithub/guidewire/issues/73)
> **Prioridad:** P1
> **Estado:** Guia de validacion (requiere ejecucion manual)

## Contexto

Esta issue no tiene un "fix" de codigo — es un proceso de validacion manual que debe ejecutarse
en el cluster CRC (OpenShift Local). Este documento sirve como guia paso a paso.

## Prerequisitos

- Host con al menos 16 GB RAM (recomendado 32 GB+)
- CRC instalado y configurado (`crc setup` completado)
- Cuenta Red Hat con pull secret (gratuita)
- `oc` CLI disponible en PATH

## Paso 1: Arrancar CRC limpio

```bash
# Si ya existe un cluster, eliminar y recrear
crc delete -f

# Arrancar con recursos adecuados
crc start --cpus 8 --memory 20480 --disk-size 80

# Configurar CLI
eval $(crc oc-env)
oc login -u developer -p developer https://api.crc.testing:6443

# Verificar que el cluster esta OK
oc cluster-info
crc status
```

**Resultado esperado:** Cluster OpenShift 4.x corriendo con oc CLI conectado.

### Verificacion del cluster

```bash
oc version                               # Client + server version
oc get nodes                             # 1 nodo Ready
oc get clusteroperators | grep -c True   # Todos los operators disponibles
```

## Paso 2: Desplegar el stack

```bash
cd lab/openshift

# Deploy completo (infra + apps)
./deploy-all.sh
```

### Posibles errores y soluciones

| Error | Causa | Solucion |
|-------|-------|----------|
| `oc: command not found` | CLI no configurado | `eval $(crc oc-env)` |
| Operator no instala | OLM no ready | `oc get csv -n openshift-operators` y esperar |
| Build falla con OOM | Poca memoria para builds | `crc start --memory 24576` |
| ImagePull error | Pull secret expirado | Renovar en console.redhat.com |
| Timeout descargando dependencias | Red lenta | Reintentar el build |

### Verificar que las 5 imagenes se crearon

```bash
oc get is -n guidewire-apps
# Debe mostrar:
# billing-service
# camel-gateway
# drools-engine
# incidents-service
# customers-service
```

## Paso 3: Verificar infraestructura

```bash
# Verificar pods en guidewire-infra
oc get pods -n guidewire-infra

# Todos deben estar Running/Ready
```

### Componentes esperados

1. **PostgreSQL** — `deploy/postgres` (1/1 Ready)
2. **Kafka (Strimzi)** — `kafka-cluster-kafka-0` (1/1 Ready)
3. **ActiveMQ (AMQ Broker)** — `activemq-broker-ss-0` (1/1 Ready)
4. **Apicurio Registry** — `deploy/apicurio-registry` (1/1 Ready)
5. **Kafdrop** — `deploy/kafdrop` (1/1 Ready)
6. **3Scale APIcast** — `deploy/apicast` (1/1 Ready)

### Verificar Routes

```bash
oc get routes -n guidewire-infra
# kafdrop   → https://kafdrop-guidewire-infra.apps-crc.testing
# apicast   → https://apicast-guidewire-infra.apps-crc.testing
# apicurio  → https://apicurio-guidewire-infra.apps-crc.testing
```

## Paso 4: Verificar aplicaciones

```bash
# Verificar pods en guidewire-apps
oc get pods -n guidewire-apps

# Todos deben estar Running/Ready
```

### Verificar healthchecks

```bash
# Via Routes
curl -sk https://camel-gateway-guidewire-apps.apps-crc.testing/actuator/health | jq .
curl -sk https://billing-service-guidewire-apps.apps-crc.testing/actuator/health | jq .
curl -sk https://incidents-service-guidewire-apps.apps-crc.testing/q/health | jq .
curl -sk https://customers-service-guidewire-apps.apps-crc.testing/health | jq .
curl -sk https://drools-engine-guidewire-apps.apps-crc.testing/actuator/health | jq .
```

**Resultado esperado:** Todos devuelven `{"status":"UP"}` o equivalente.

## Paso 5: Verificar topics Kafka

```bash
# Via oc exec
oc exec -n guidewire-infra kafka-cluster-kafka-0 -- bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# O via Kafdrop UI
# https://kafdrop-guidewire-infra.apps-crc.testing
```

**Topics esperados:**
- `billing.invoice-created`
- `billing.invoice-status-changed`
- `incidents.incident-created`
- `incidents.incident-status-changed`
- `customers.customer-registered`
- `customers.customer-status-changed`
- `dlq.errors`

## Paso 6: Test E2E con Postman/Newman

```bash
# Desde tu host (necesitas Newman instalado)
npm install -g newman

# Ejecutar la coleccion E2E apuntando a las Routes
newman run contracts/postman/guidewire-e2e.postman_collection.json \
  -e contracts/postman/crc.postman_environment.json \
  --reporters cli \
  --delay-request 1000
```

### Flujo del test E2E

1. Health checks de todos los servicios (via Routes)
2. Crear cliente via API de customers
3. Crear poliza via Camel Gateway -> PolicyCenter mock
4. Crear factura via API de billing
5. Crear reclamacion via Camel Gateway -> ClaimCenter mock
6. Verificar que los eventos Kafka se publicaron (via Kafdrop)
7. Verificar que Apicurio registro los schemas

## Paso 7: Verificar integracion Kafka

```bash
# Verificar que hay mensajes en los topics
oc exec -n guidewire-infra kafka-cluster-kafka-0 -- bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic billing.invoice-created \
  --from-beginning --max-messages 1

oc exec -n guidewire-infra kafka-cluster-kafka-0 -- bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic customers.customer-registered \
  --from-beginning --max-messages 1
```

## Problemas conocidos y mitigaciones

### 1. Kafka no esta listo cuando los microservicios arrancan

**Sintoma:** Los servicios logean `Connection refused` a Kafka.

**Mitigacion:** El script `deploy-all.sh` espera a que el Kafka CRD reporte Ready antes
de desplegar aplicaciones. Si persiste, escalar el deployment a 0 y volver a 1:
`oc scale deploy/<service> -n guidewire-apps --replicas=0 && oc scale deploy/<service> -n guidewire-apps --replicas=1`

### 2. Pods en estado Pending

**Sintoma:** `oc get pods` muestra pods en Pending.

**Mitigacion:** Verificar recursos disponibles con `crc status`. Si no hay suficiente
memoria, aumentar con `crc stop && crc start --memory 24576`.

### 3. Memoria insuficiente

**Sintoma:** Pods se reinician con OOMKilled.

**Mitigacion:** Reducir el pool de conexiones de PostgreSQL o ajustar resource limits.
CRC con 20 GB RAM asignados deberia ser suficiente para todo el stack.

### 4. Quarkus tarda en arrancar

**Sintoma:** Incidents-service responde 503 durante los primeros segundos.

**Mitigacion:** Esperar 30-60 segundos tras el deploy. Los readiness probes evitan
que el Service envie trafico antes de que el pod este listo.

## Checklist de validacion

- [ ] `crc start` completa sin errores
- [ ] `./deploy-all.sh` despliega sin errores fatales
- [ ] `oc get pods -n guidewire-infra` muestra todos los pods Running
- [ ] `oc get pods -n guidewire-apps` muestra todos los pods Running
- [ ] Todos los healthchecks responden UP via Routes
- [ ] Los 7 topics de Kafka existen
- [ ] Newman E2E pasa todos los tests
- [ ] Los schemas Avro estan registrados en Apicurio
- [ ] No hay errores en los logs (`oc logs -n guidewire-apps deploy/<service>`)
