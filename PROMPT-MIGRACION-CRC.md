# PROMPT: Migración de Vagrant a Red Hat OpenShift Local (CRC)

## Contexto del proyecto

Este es un POC de integración empresarial Guidewire InsuranceSuite que simula un entorno de producción con arquitecturas SOA, MSA y EDA. El stack tecnológico completo es:

| Área | Tecnología |
|---|---|
| Plataforma CORE | Guidewire InsuranceSuite + Integration Gateway |
| Arquitecturas | SOA, MSA, EDA |
| Especificación API | OpenAPI 3.1, AsyncAPI 3.0, AVRO — API First |
| API Management | Red Hat 3Scale (APIcast v3.11) |
| Integración EIP | Red Hat Fuse (Apache Camel 4.4 + Spring Boot 3.3 / Quarkus 3.8) |
| Event Driven | Red Hat AMQ Streams (Apache Kafka 3.7 KRaft), Red Hat AMQ Broker (ActiveMQ Artemis 2.33) |
| Reglas de Negocio | IBM BAM Open Editions (Drools 8.44) |
| Registro de Servicios | Apicurio Service Registry 2.5 |
| Base de datos | PostgreSQL 16 |
| Herramientas API | Postman, Newman |
| Plataforma PaaS/CaaS | Red Hat OpenShift 4.x |

### Arquitectura actual (a migrar)

```
HOST (Linux)
└── Vagrant + libvirt/KVM
    └── VM Fedora 41 (20GB RAM, 8 CPUs, 80GB disco)
        └── Podman + podman-compose
            ├── PostgreSQL 16 (4 bases de datos)
            ├── Apache Kafka 3.7 (KRaft, 7 topics)
            ├── ActiveMQ Artemis 2.33
            ├── Kafdrop 4.0.1
            ├── Apicurio Registry 2.5.11
            ├── 3Scale APIcast
            ├── Drools KIE Engine (custom build)
            ├── Apache Camel Gateway (custom build)
            ├── Billing Service (Spring Boot)
            ├── Incidents Service (Quarkus)
            └── Customers Service (Node.js/Express)
```

### Arquitectura objetivo (nueva)

```
HOST (Linux)
├── crc CLI (gestiona cluster OpenShift)
├── oc CLI (interactúa con OpenShift)
└── Red Hat OpenShift 4.x (CRC)
    ├── Namespace: guidewire-infra
    │   ├── PostgreSQL 16 (Deployment + PVC)
    │   ├── AMQ Streams / Kafka (Strimzi Operator)
    │   ├── AMQ Broker / ActiveMQ (Operator)
    │   ├── Apicurio Registry (Operator)
    │   ├── Kafdrop (Deployment)
    │   └── 3Scale APIcast (Deployment o Operator)
    └── Namespace: guidewire-apps
        ├── Drools KIE Engine (Deployment + BuildConfig)
        ├── Apache Camel Gateway (Deployment + BuildConfig)
        ├── Billing Service (Deployment + BuildConfig)
        ├── Incidents Service (Deployment + BuildConfig)
        └── Customers Service (Deployment + BuildConfig)
```

### Servicios y puertos actuales

| Puerto | Servicio | Tecnología |
|---|---|---|
| 8000 | 3Scale APIcast (proxy) | quay.io/3scale/apicast:v3.11 |
| 8001 | 3Scale APIcast (management) | quay.io/3scale/apicast:v3.11 |
| 8081 | Apicurio Service Registry | apicurio/apicurio-registry-sql:2.5.11.Final |
| 8082 | Billing Service | Java 21 + Spring Boot 3.3 + Spring Data JPA + Kafka + Flyway |
| 8083 | Camel Gateway | Java 21 + Spring Boot 3.3 + Apache Camel 4.4 |
| 8084 | Incidents Service | Java 21 + Quarkus 3.8 + Panache + Kafka + Flyway |
| 8085 | Customers Service | Node.js 20 + TypeScript + Express.js 4 + Prisma + KafkaJS |
| 8086 | Drools KIE Engine | Java 21 + Spring Boot 3.3 + Drools 8.44 |
| 8161 | ActiveMQ Artemis (Hawtio console) | apache/activemq-artemis:2.33.0 |
| 9000 | Kafdrop (Kafka UI) | obsidiandynamics/kafdrop:4.0.1 |
| 9092 | Apache Kafka (broker) | apache/kafka:3.7.0 (KRaft) |
| 15432 | PostgreSQL | postgres:16-alpine |
| 61616 | ActiveMQ Artemis (AMQP/JMS) | apache/activemq-artemis:2.33.0 |

### Bases de datos PostgreSQL

| Base de datos | Usuario | Password | Uso |
|---|---|---|---|
| apicurio | apicurio | apicurio123 | Apicurio Registry storage |
| billing | billing_user | billing123 | Billing Service |
| incidents | incidents_user | incidents123 | Incidents Service |
| customers | customers_user | customers123 | Customers Service |

### Kafka Topics (7 topics)

- `billing.invoice-created` (3 particiones)
- `billing.invoice-status-changed` (3 particiones)
- `incidents.incident-created` (3 particiones)
- `incidents.incident-status-changed` (3 particiones)
- `customers.customer-registered` (3 particiones)
- `customers.customer-status-changed` (3 particiones)
- `dlq.errors` (1 partición, retención 30 días)

### ActiveMQ Queues

- `claims.invoice-requests` (anycast)
- `claims.fraud-alerts` (anycast)
- `notifications.outbound` (anycast)

### 3Scale API Routes

6 servicios registrados con API Key auth (header X-API-Key) y rate limiting:
- Guidewire PolicyCenter API → Camel Gateway (100 req/min)
- Guidewire ClaimCenter API → Camel Gateway (100 req/min)
- Guidewire BillingCenter API → Camel Gateway (100 req/min)
- Billing Service API (200 req/min)
- Incidents Service API (200 req/min)
- Customers Service API (200 req/min)

---

## Tarea a realizar

### 1. ELIMINAR todas las referencias a Vagrant

Buscar y actualizar TODOS los archivos que contengan referencias a Vagrant, libvirt, KVM, `vagrant up`, `vagrant ssh`, `vagrant destroy`, Vagrantfile, o la VM Fedora. Los archivos conocidos que contienen estas referencias son:

- `README.md` (raíz del proyecto)
- `openspecs/README.md`
- `openspecs/docs/infra/lab-environment/README.md`
- `openspecs/docs/infra/lab-environment/INSTALL.md`
- `openspecs/infra/lab-environment/spec.yml`
- `openspecs/docs/architecture/README.md`
- `openspecs/docs/architecture/spec.yml`
- `guia-entrevista-integracion.md` (sección 13 completa del lab)
- `lab/Vagrantfile` → **ELIMINAR este archivo**
- `lab/provision/setup.sh` → **ELIMINAR este archivo**
- `lab/provision/install-dev-tools.sh` → **ELIMINAR este archivo**

Buscar también con grep en todo el proyecto por si hay más referencias: `vagrant`, `Vagrant`, `Vagrantfile`, `libvirt`, `192.168.56.10`, `generic/fedora`, `vagrant ssh`, `virsh`.

### 2. CREAR documento de nueva infraestructura con CRC

Crear `openspecs/docs/infra/lab-environment/README.md` (reemplazando el actual) con:

- Diagrama Mermaid de la nueva arquitectura (HOST → CRC → OpenShift → Namespaces → Pods)
- Requisitos del host (RAM 16GB+, CPUs 6+, disco 50GB+, cuenta Red Hat Developer gratuita)
- Instalación de CRC paso a paso (descarga, `crc setup`, `crc start`, pull secret)
- Instalación de `oc` CLI
- Configuración de Operators desde OperatorHub:
  - AMQ Streams (Strimzi) para Kafka
  - AMQ Broker para ActiveMQ Artemis
  - Apicurio Registry Operator
  - 3Scale API Management (o APIcast standalone como Deployment)
- Creación de namespaces (`guidewire-infra`, `guidewire-apps`)
- Despliegue de infraestructura (PostgreSQL, Kafka vía CRDs de Strimzi, ActiveMQ vía CRDs, Apicurio, Kafdrop, 3Scale)
- Despliegue de microservicios (BuildConfigs + Deployments para los 5 servicios custom)
- Tabla de rutas OpenShift equivalentes a los puertos anteriores
- Operaciones del día a día (`crc start`, `crc stop`, `oc login`, `oc get pods`, etc.)
- Limpieza completa (`crc delete`, `crc cleanup`)

### 3. CREAR manifiestos Kubernetes/OpenShift

Crear el directorio `lab/openshift/` con los manifiestos K8s necesarios para desplegar todo el stack en CRC. Estructura sugerida:

```
lab/openshift/
├── namespaces.yml
├── infra/
│   ├── postgres/
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   ├── pvc.yml
│   │   ├── configmap-init-db.yml    (contenido de init-db.sql actual)
│   │   └── secret.yml
│   ├── kafka/
│   │   ├── kafka-cluster.yml         (CRD de Strimzi Operator)
│   │   └── kafka-topics.yml          (KafkaTopic CRDs para los 7 topics)
│   ├── activemq/
│   │   ├── activemq-broker.yml       (CRD de AMQ Broker Operator)
│   │   └── activemq-addresses.yml    (queues)
│   ├── apicurio/
│   │   └── apicurio-registry.yml     (CRD del Operator o Deployment)
│   ├── kafdrop/
│   │   ├── deployment.yml
│   │   └── service.yml
│   └── threescale/
│       ├── deployment.yml             (APIcast)
│       ├── service.yml
│       └── configmap-apicast.yml      (apicast-config.json actual)
├── apps/
│   ├── drools-engine/
│   │   ├── buildconfig.yml
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   └── route.yml
│   ├── camel-gateway/
│   │   ├── buildconfig.yml
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   └── route.yml
│   ├── billing-service/
│   │   ├── buildconfig.yml
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   └── route.yml
│   ├── incidents-service/
│   │   ├── buildconfig.yml
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   └── route.yml
│   └── customers-service/
│       ├── buildconfig.yml
│       ├── deployment.yml
│       ├── service.yml
│       └── route.yml
├── operators/
│   ├── amq-streams-subscription.yml
│   ├── amq-broker-subscription.yml
│   └── apicurio-subscription.yml
└── deploy-all.sh                      (script que aplica todo en orden)
```

Los manifiestos deben:
- Usar las MISMAS imágenes, versiones y configuraciones que el podman-compose.yml actual
- Mantener las MISMAS variables de entorno, credenciales y configuraciones
- Incluir readiness/liveness probes equivalentes a los healthchecks del compose
- Usar Secrets para credenciales (no en texto plano en deployments)
- Usar ConfigMaps para configuraciones (init-db.sql, apicast-config.json, create-topics.sh)
- Respetar las dependencias de arranque (infra primero, luego platform, luego engines, luego apps)
- El script `deploy-all.sh` debe aplicar todo en el orden correcto con esperas entre fases

### 4. ACTUALIZAR el INSTALL.md

Crear `openspecs/docs/infra/lab-environment/INSTALL.md` (reemplazando el actual) con:

- Prerrequisitos del host (hardware, SO, cuenta Red Hat)
- Instalación de CRC paso a paso (descarga, setup, start)
- Configuración del pull secret
- Instalación de operadores (AMQ Streams, AMQ Broker, Apicurio)
- Despliegue del stack completo usando los manifiestos
- Verificación de que todo funciona
- Operaciones diarias
- Troubleshooting común de CRC y OpenShift
- Desinstalación completa

### 5. ACTUALIZAR la spec YAML del lab

Actualizar `openspecs/infra/lab-environment/spec.yml` reemplazando toda la sección de Vagrant/Podman por la nueva arquitectura CRC/OpenShift, manteniendo el formato y estructura del YAML.

### 6. ACTUALIZAR el README.md raíz

En `README.md` del proyecto:
- Reemplazar el diagrama Mermaid de arquitectura para mostrar CRC/OpenShift en vez de Vagrant/Podman
- Actualizar la sección Quick Start con comandos de CRC (`crc start`, `oc apply`, etc.)
- Actualizar la tabla de puertos por rutas de OpenShift
- Actualizar la tabla de tecnologías: reemplazar "Vagrant + libvirt/KVM" por "Red Hat OpenShift Local (CRC)"
- Mantener todo lo demás igual (componentes, contratos, etc.)

### 7. ACTUALIZAR el README.md de openspecs

En `openspecs/README.md`:
- Reemplazar diagrama Mermaid de arquitectura (HOST → CRC → OpenShift)
- Actualizar Quick Start
- Actualizar tabla de tecnologías

### 8. ACTUALIZAR documentación de componentes de infraestructura

En cada uno de estos archivos, actualizar las secciones de "cómo acceder" y "cómo conectar" para reflejar OpenShift (rutas, servicios internos, `oc port-forward`) en vez de puertos locales de podman-compose:

- `openspecs/docs/infra/kafka/README.md` — acceso vía Strimzi CRDs, `oc exec` para CLI
- `openspecs/docs/infra/postgres/README.md` — acceso vía `oc port-forward`, connection strings internas
- `openspecs/docs/infra/activemq/README.md` — acceso vía AMQ Broker CRDs, rutas para Hawtio
- `openspecs/docs/infra/apicurio/README.md` — acceso vía ruta OpenShift
- `openspecs/docs/infra/threescale/README.md` — acceso vía ruta OpenShift

### 9. ACTUALIZAR la guía de entrevista

En `guia-entrevista-integracion.md`:
- Sección 10 (OpenShift): ampliar con detalles específicos de CRC y cómo se usa en este POC
- Sección 12 (POC architecture): actualizar para reflejar OpenShift en vez de Docker Compose
- Sección 13 (Lab setup): REESCRIBIR COMPLETAMENTE para CRC en vez de Vagrant. Incluir el nuevo workflow de setup, comandos diarios, y manifiestos K8s

### 10. ACTUALIZAR el spec-index.yml

En `openspecs/spec-index.yml`:
- Actualizar la sección tech_stack: reemplazar Vagrant por CRC/OpenShift
- Añadir OpenShift, Strimzi, Operators al stack

### 11. ACTUALIZAR documentación de arquitectura

En `openspecs/docs/architecture/README.md`:
- Actualizar el diagrama de componentes para incluir OpenShift como plataforma
- Actualizar o añadir ADR: "OpenShift Local (CRC) sobre Vagrant" explicando la decisión de migración
- Actualizar el ADR de "Podman over Docker" para explicar que ahora se usa el container runtime de OpenShift (CRI-O) para el cluster, y Podman opcionalmente para builds locales

### 12. MANTENER el podman-compose.yml como alternativa

NO eliminar `lab/podman/podman-compose.yml` ni sus archivos de config. Mantenerlos como alternativa para desarrollo rápido sin OpenShift. Pero añadir una nota en su directorio indicando que es la opción legacy/alternativa y que la opción principal es OpenShift.

---

## Reglas importantes

1. **Idioma**: Toda la documentación debe mantenerse en el MISMO idioma que el archivo original (la mayoría está en inglés, la guía de entrevista en español).
2. **Formato**: Respetar el formato Markdown existente, incluyendo diagramas Mermaid, tablas, y estructura de headers.
3. **Consistencia**: Usar los mismos nombres de servicios, puertos internos, credenciales y versiones que ya existen.
4. **No romper nada**: Los Dockerfiles de los microservicios en `components/` NO deben modificarse. Los contratos en `contracts/` NO deben modificarse. Los AVRO schemas NO deben modificarse.
5. **Spec-Driven**: Mantener la metodología spec-driven del proyecto. Si actualizas un spec.yml, asegúrate de que el README.md correspondiente refleje los cambios.
6. **Git-friendly**: Los cambios deben poder comitearse de forma atómica y lógica.
