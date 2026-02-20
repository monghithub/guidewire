# Guidewire Integration POC

POC de arquitectura de integración para **Guidewire InsuranceSuite**. Demuestra patrones SOA/MSA/EDA con API-First, contract-driven development y stack enterprise Red Hat sobre un laboratorio aislado con Vagrant + Podman.

---

## Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│  VM Ubuntu 24.04 (Vagrant + libvirt/KVM)                         │
│  Podman + Podman Compose                                         │
│                                                                   │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────────┐     │
│  │  3Scale   │───▶│ Camel Gateway │───▶│ Guidewire Mock      │     │
│  │ (Gateway) │    │ (Integración)│    │ Policy/Claim/Billing│     │
│  └─────┬────┘    └──────┬───────┘    └─────────────────────┘     │
│        │                │                                         │
│        │         ┌──────┴───────┐                                │
│        │         │   Drools      │                                │
│        │         │ (Reglas)      │                                │
│        │         └──────┬───────┘                                │
│        │                │                                         │
│  ┌─────┴────────────────┴────────────────────────────────┐       │
│  │                    Apache Kafka                         │       │
│  │                 (Event Backbone)                        │       │
│  └─────┬──────────────┬──────────────┬───────────────────┘       │
│        │              │              │                            │
│  ┌─────┴─────┐  ┌─────┴─────┐  ┌────┴──────┐                   │
│  │  Billing   │  │ Incidents  │  │ Customers  │                   │
│  │ Spring Boot│  │  Quarkus   │  │  Node.js   │                   │
│  └─────┬─────┘  └─────┬─────┘  └────┬──────┘                   │
│        └──────────────┴──────────────┘                            │
│                    PostgreSQL                                     │
│                                                                   │
│  Apicurio (Registry) · ActiveMQ Artemis · Kafdrop                │
└──────────────────────────────────────────────────────────────────┘
```

---

## Estructura del Proyecto

```
guidewire/
├── README.md                          ← Este archivo
├── guia-entrevista-integracion.md     ← Guía de preparación técnica
│
├── openspecs/                         ← Especificaciones (fuente de verdad)
│   ├── README.md
│   ├── spec-index.yml
│   ├── infra/                         ← Specs de infraestructura
│   ├── design/                        ← Specs de contratos API-First
│   ├── components/                    ← Specs de componentes
│   ├── integration/                   ← Specs de integración y testing
│   ├── devops/                        ← Specs de CI/CD
│   └── docs/                          ← Documentación por componente
│
├── contracts/                         ← Contratos API-First
│   ├── openapi/                       ← 6 specs OpenAPI 3.1
│   ├── asyncapi/                      ← 1 spec AsyncAPI 3.0
│   └── avro/                          ← 6 schemas AVRO
│
├── lab/                               ← Laboratorio (Vagrant + Podman)
│   ├── Vagrantfile
│   ├── provision/                     ← Scripts de provisioning
│   └── podman/                        ← Compose + configs
│
└── components/                        ← Código fuente
    ├── camel-gateway/                 ← Java 21 + Spring Boot + Camel 4
    ├── drools-engine/                 ← Java 21 + Spring Boot + Drools 8
    ├── billing-service/               ← Java 21 + Spring Boot 3.3
    ├── incidents-service/             ← Java 21 + Quarkus 3.8
    └── customers-service/             ← Node.js 20 + TypeScript
```

---

## Documentación

### Especificaciones (OpenSpecs)

| Documento | Descripción |
|-----------|-------------|
| [Índice de Specs](openspecs/spec-index.yml) | Índice maestro con todos los módulos y orden de implementación |
| [OpenSpecs README](openspecs/README.md) | Guía completa del sistema de specs, tablas de estado y quick start |

### Infraestructura

| Componente | Spec | Documentación |
|-----------|------|---------------|
| PostgreSQL | [spec.yml](openspecs/infra/postgres/spec.yml) | [docs](openspecs/docs/infra/postgres/README.md) |
| Kafka (KRaft) | [spec.yml](openspecs/infra/kafka/spec.yml) | [docs](openspecs/docs/infra/kafka/README.md) |
| ActiveMQ Artemis | [spec.yml](openspecs/infra/activemq/spec.yml) | [docs](openspecs/docs/infra/activemq/README.md) |
| 3Scale API Gateway | [spec.yml](openspecs/infra/threescale/spec.yml) | [docs](openspecs/docs/infra/threescale/README.md) |
| Apicurio Registry | [spec.yml](openspecs/infra/apicurio/spec.yml) | [docs](openspecs/docs/infra/apicurio/README.md) |
| Lab Environment | [spec.yml](openspecs/infra/lab-environment/spec.yml) | [docs](openspecs/docs/infra/lab-environment/README.md) |

### Contratos API-First

| Tipo | Recursos | Documentación |
|------|----------|---------------|
| OpenAPI 3.1 | [policycenter](contracts/openapi/policycenter-api.yml) · [claimcenter](contracts/openapi/claimcenter-api.yml) · [billingcenter](contracts/openapi/billingcenter-api.yml) · [billing-svc](contracts/openapi/billing-service-api.yml) · [incidents-svc](contracts/openapi/incidents-service-api.yml) · [customers-svc](contracts/openapi/customers-service-api.yml) | [docs](openspecs/docs/design/openapi/README.md) |
| AsyncAPI 3.0 | [guidewire-events](contracts/asyncapi/guidewire-events.yml) | [docs](openspecs/docs/design/asyncapi/README.md) |
| AVRO Schemas | [InvoiceCreated](contracts/avro/InvoiceCreated.avsc) · [InvoiceStatusChanged](contracts/avro/InvoiceStatusChanged.avsc) · [IncidentCreated](contracts/avro/IncidentCreated.avsc) · [IncidentStatusChanged](contracts/avro/IncidentStatusChanged.avsc) · [CustomerRegistered](contracts/avro/CustomerRegistered.avsc) · [CustomerStatusChanged](contracts/avro/CustomerStatusChanged.avsc) | [docs](openspecs/docs/design/avro/README.md) |

#### Specs de diseño (detalle de modelos y campos)

| Contrato | Spec |
|----------|------|
| PolicyCenter | [spec.yml](openspecs/design/openapi/guidewire-policycenter/spec.yml) |
| ClaimCenter | [spec.yml](openspecs/design/openapi/guidewire-claimcenter/spec.yml) |
| BillingCenter | [spec.yml](openspecs/design/openapi/guidewire-billingcenter/spec.yml) |
| Billing Service | [spec.yml](openspecs/design/openapi/billing-service/spec.yml) |
| Incidents Service | [spec.yml](openspecs/design/openapi/incidents-service/spec.yml) |
| Customers Service | [spec.yml](openspecs/design/openapi/customers-service/spec.yml) |
| Eventos Kafka (AsyncAPI) | [spec.yml](openspecs/design/asyncapi/guidewire-events/spec.yml) |
| AVRO — Billing Events | [spec.yml](openspecs/design/avro/billing-events/spec.yml) |
| AVRO — Incidents Events | [spec.yml](openspecs/design/avro/incidents-events/spec.yml) |
| AVRO — Customers Events | [spec.yml](openspecs/design/avro/customers-events/spec.yml) |

### Componentes

| Componente | Tech Stack | Spec | Documentación | Código |
|-----------|-----------|------|---------------|--------|
| Camel Gateway | Java 21 · Spring Boot 3.3 · Camel 4 | [spec.yml](openspecs/components/camel-gateway/spec.yml) | [docs](openspecs/docs/components/camel-gateway/README.md) | [src](components/camel-gateway/) |
| Drools Engine | Java 21 · Spring Boot 3.3 · Drools 8 | [spec.yml](openspecs/components/drools-engine/spec.yml) | [docs](openspecs/docs/components/drools-engine/README.md) | [src](components/drools-engine/) |
| Billing Service | Java 21 · Spring Boot 3.3 · JPA · Kafka | [spec.yml](openspecs/components/billing-service/spec.yml) | [docs](openspecs/docs/components/billing-service/README.md) | [src](components/billing-service/) |
| Incidents Service | Java 21 · Quarkus 3.8 · Panache · Kafka | [spec.yml](openspecs/components/incidents-service/spec.yml) | [docs](openspecs/docs/components/incidents-service/README.md) | [src](components/incidents-service/) |
| Customers Service | Node.js 20 · TypeScript · Prisma · KafkaJS | [spec.yml](openspecs/components/customers-service/spec.yml) | [docs](openspecs/docs/components/customers-service/README.md) | [src](components/customers-service/) |

### Integración y DevOps

| Módulo | Spec | Documentación |
|--------|------|---------------|
| 3Scale API Registration | [spec.yml](openspecs/integration/threescale-registration/spec.yml) | [docs](openspecs/docs/integration/README.md) |
| Postman E2E Tests | [spec.yml](openspecs/integration/postman-e2e/spec.yml) | [docs](openspecs/docs/integration/README.md) |
| CI/CD Pipeline | [spec.yml](openspecs/devops/ci-cd/spec.yml) | [docs](openspecs/docs/devops/README.md) |
| Arquitectura (ADRs) | [spec.yml](openspecs/docs/architecture/spec.yml) | — |

### Laboratorio

| Archivo | Descripción |
|---------|-------------|
| [Vagrantfile](lab/Vagrantfile) | VM Ubuntu 24.04 — 20GB RAM, 8 CPUs, 80GB disco |
| [setup.sh](lab/provision/setup.sh) | Provisioning: instala Podman + Podman Compose |
| [install-dev-tools.sh](lab/provision/install-dev-tools.sh) | Opcional: JDK 21, Maven, Node.js 20, kcat |
| [podman-compose.yml](lab/podman/podman-compose.yml) | Stack completo: 12 servicios con healthchecks |
| [.env](lab/podman/.env) | Variables de entorno (versiones, credenciales, puertos) |
| [init-db.sql](lab/podman/config/init-db.sql) | Inicialización PostgreSQL (4 bases de datos) |
| [create-topics.sh](lab/podman/config/create-topics.sh) | Creación de 7 topics Kafka |
| [apicast-config.json](lab/podman/config/apicast-config.json) | Configuración declarativa de 3Scale |

---

## Quick Start

```bash
# 1. Instalar en tu máquina (lo único necesario)
sudo apt install vagrant qemu-kvm libvirt-daemon-system virt-manager
vagrant plugin install vagrant-libvirt

# 2. Levantar la VM
cd lab
vagrant up

# 3. Entrar a la VM y levantar servicios
vagrant ssh
cd ~/lab-guidewire/podman
podman-compose up -d

# 4. Verificar
podman-compose ps

# 5. Acceder desde tu navegador
#    http://localhost:8081  → Apicurio Registry
#    http://localhost:9000  → Kafdrop (Kafka UI)
#    http://localhost:8161  → ActiveMQ Console
#    http://localhost:8000  → 3Scale Gateway

# 6. Apagar todo
podman-compose down
exit
vagrant halt
```

---

## Puertos

| Puerto | Servicio | URL |
|--------|----------|-----|
| 8000 | 3Scale API Gateway | http://localhost:8000 |
| 8001 | 3Scale Management | http://localhost:8001 |
| 8081 | Apicurio Registry | http://localhost:8081 |
| 8082 | Billing Service | http://localhost:8082/api/v1/invoices |
| 8083 | Camel Gateway | http://localhost:8083/api/v1 |
| 8084 | Incidents Service | http://localhost:8084/api/v1/incidents |
| 8085 | Customers Service | http://localhost:8085/api/v1/customers |
| 8086 | Drools KIE Server | http://localhost:8086/api/v1/rules |
| 8161 | ActiveMQ Console | http://localhost:8161/console |
| 9000 | Kafdrop | http://localhost:9000 |
| 9092 | Kafka Broker | localhost:9092 |
| 15432 | PostgreSQL | localhost:15432 |
| 61616 | ActiveMQ AMQP/JMS | localhost:61616 |

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Virtualización | Vagrant + libvirt/KVM | 2.4+ |
| Contenedores | Podman + Podman Compose | 4.9+ |
| API Gateway | Red Hat 3Scale (APIcast) | 3.11 |
| Integración | Apache Camel | 4.x |
| Event Streaming | Apache Kafka (KRaft) | 3.7 |
| Mensajería JMS | Apache ActiveMQ Artemis | 2.33 |
| Reglas de Negocio | Drools / KIE Server | 8.x |
| Schema Registry | Apicurio Service Registry | 2.5 |
| Base de Datos | PostgreSQL | 16 |
| Runtime Java | Eclipse Temurin | 21 |
| Runtime Node.js | Node.js LTS | 20 |
| Frameworks | Spring Boot 3.3 · Quarkus 3.8 · Express 4 | — |
| Specs | OpenAPI 3.1 · AsyncAPI 3.0 · Apache AVRO | — |

---

## Metodología

**Spec-Driven Development con IA**: las especificaciones YAML en `openspecs/` son la fuente de verdad. El código, contratos y documentación se generan y validan contra ellas.

1. **Spec** → Define completamente cada componente
2. **Contracts** → OpenAPI, AsyncAPI, AVRO generados desde los specs
3. **Code** → Scaffolds generados desde contratos y specs
4. **Docs** → Documentación sincronizada con la implementación
5. **Test** → Postman E2E valida el flujo completo

---

## Issues

El proyecto se gestiona con [GitHub Issues](../../issues). Los 41 issues abiertos (#29-#69) definen cada tarea de implementación, organizados por fase:

| Fase | Issues | Descripción |
|------|--------|-------------|
| Infraestructura | [#29](../../issues/29)-[#34](../../issues/34) | Kafka, Apicurio, 3Scale, ActiveMQ, PostgreSQL, Docker Compose |
| Diseño | [#35](../../issues/35)-[#44](../../issues/44) | OpenAPI, AsyncAPI, AVRO |
| Camel Gateway | [#45](../../issues/45)-[#51](../../issues/51) | Rutas SOAP/REST, transformaciones, Kafka |
| Drools | [#52](../../issues/52)-[#53](../../issues/53) | Reglas de fraude, validaciones, comisiones |
| Microservicios | [#54](../../issues/54)-[#65](../../issues/65) | Billing, Incidents, Customers |
| Cross-cutting | [#66](../../issues/66)-[#69](../../issues/69) | 3Scale registration, Postman E2E, Docs, CI/CD |
