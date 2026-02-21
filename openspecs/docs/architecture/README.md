# Architecture Documentation

Guidewire Integration POC -- Architecture reference for the insurance integration platform.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Interaction Diagram](#component-interaction-diagram)
3. [Componentes del Sistema](#componentes-del-sistema)
   - [Servicios de Aplicacion](#servicios-de-aplicacion-namespace-guidewire-apps)
   - [Infraestructura](#infraestructura-namespace-guidewire-infra)
4. [Data Flow Diagrams](#data-flow-diagrams)
5. [Architecture Decision Records (ADRs)](#architecture-decision-records-adrs)
6. [Technology Stack Summary](#technology-stack-summary)

---

## Architecture Overview

This POC demonstrates integration patterns for **Guidewire InsuranceSuite** (PolicyCenter, ClaimCenter, BillingCenter) using a polyglot microservices architecture with event-driven communication.

The system follows these architectural principles:

- **API-First / Contract-Driven**: All interfaces defined via OpenAPI 3.1, AsyncAPI 3.0, and AVRO schemas before implementation
- **Event-Driven Architecture (EDA)**: Apache Kafka as the event backbone for asynchronous, decoupled communication
- **Polyglot Microservices**: Each service chooses the best technology for its domain
- **Infrastructure as Code**: Reproducible lab environment via Red Hat OpenShift Local (CRC) + Kubernetes manifests

---

## Component Interaction Diagram

```mermaid
graph TD
    consumers["External Consumers"]
    consumers --> threescale

    threescale["3Scale API Gateway — APIcast<br/>:8000 / :8001"]
    threescale --> camel

    camel["Camel Gateway<br/>Apache Camel 4 + Spring Boot :8083<br/>Routes: PolicyCenter SOAP · ClaimCenter REST · BillingCenter REST"]
    camel --> gwmock
    camel --> drools
    camel --> kafka

    gwmock["Guidewire Mock APIs<br/>Policy / Claim / Billing"]
    drools["Drools Rules Engine :8086<br/>Validation · Fraud · Routing"]

    kafka["Apache Kafka — KRaft :9092<br/>Topics: billing.invoice-created · billing.invoice-status-changed<br/>incidents.incident-created · incidents.incident-status-changed<br/>customers.customer-registered · customers.customer-status-changed<br/>policies.policy-events · events.unclassified · dlq.errors"]

    kafka --> billing
    kafka --> incidents
    kafka --> customers

    billing["Billing Service<br/>Spring Boot :8082"]
    incidents["Incidents Service<br/>Quarkus :8084"]
    customers["Customers Service<br/>Node.js/TS :8085"]

    billing --> pg
    incidents --> pg
    customers --> pg

    pg["PostgreSQL :15432<br/>billing_db · incidents_db · customers_db · drools_audit · apicurio"]

    apicurio["Apicurio Schema Registry :8081"]
    activemq["ActiveMQ Artemis :61616 / :8161"]
    kafdrop["Kafdrop — Kafka UI :9000"]
```

---

## Componentes del Sistema

El POC se compone de **5 servicios de aplicacion** y **6 componentes de infraestructura**, desplegados en dos namespaces separados de OpenShift.

### Servicios de Aplicacion (namespace: guidewire-apps)

#### [Camel Gateway](../components/camel-gateway/README.md) — Hub de Integracion

El punto de entrada al ecosistema. Apache Camel 4 actua como mediador de protocolos entre los sistemas Guidewire (SOAP/REST) y la arquitectura interna basada en eventos. Implementa los Enterprise Integration Patterns (EIP) clasicos: Content-Based Router, Message Translator, Dead Letter Channel y Wire Tap. Expone endpoints CXF para PolicyCenter, ClaimCenter y BillingCenter, transforma los mensajes a formato AVRO y los publica en los topics Kafka correspondientes. Antes de publicar, invoca al Drools Engine para enriquecer los eventos con reglas de negocio.

- **Stack**: Java 21, Spring Boot 3.3, Apache Camel 4.4
- **Puerto**: 8083
- **Codigo**: [`components/camel-gateway/`](../../../components/camel-gateway/)

#### [Drools Engine](../components/drools-engine/README.md) — Motor de Reglas de Negocio

Centraliza las reglas de negocio del dominio asegurador en archivos DRL declarativos. Expone 4 conjuntos de reglas via REST: deteccion de fraude (evalua riesgo por monto, frecuencia y antiguedad del cliente), validacion de polizas (limites y elegibilidad), calculo de comisiones (por producto y canal) y enrutamiento de siniestros (asigna equipos segun prioridad y monto). Cada evaluacion se registra en una base de datos de auditoria.

- **Stack**: Java 21, Spring Boot 3.3, Drools 8
- **Puerto**: 8086
- **Codigo**: [`components/drools-engine/`](../../../components/drools-engine/)

#### [Billing Service](../components/billing-service/README.md) — Gestion de Facturacion

Microservicio de facturacion que gestiona el ciclo de vida completo de las facturas: creacion, transiciones de estado y consulta. Las facturas se crean principalmente desde eventos Kafka (originados en Camel Gateway) y progresan a traves de una maquina de estados (PENDING -> PROCESSING -> COMPLETED/FAILED/CANCELLED). Cada transicion de estado publica un evento AVRO serializado con schema de Apicurio.

- **Stack**: Java 21, Spring Boot 3.3, JPA/Hibernate, Spring Kafka
- **Puerto**: 8082
- **Codigo**: [`components/billing-service/`](../../../components/billing-service/)

#### [Incidents Service](../components/incidents-service/README.md) — Gestion de Siniestros

Microservicio cloud-native para la gestion de incidencias/siniestros. Demuestra el uso de Quarkus como alternativa a Spring Boot, con Panache ORM (simplificacion de JPA) y SmallRye Reactive Messaging para Kafka. Los siniestros siguen un ciclo de vida OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, con posibilidad de escalamiento (ESCALATED). Cada cambio de estado emite un evento a Kafka.

- **Stack**: Java 21, Quarkus 3.8, Hibernate Panache, SmallRye Reactive Messaging
- **Puerto**: 8084
- **Codigo**: [`components/incidents-service/`](../../../components/incidents-service/)

#### [Customers Service](../components/customers-service/README.md) — Gestion de Clientes

Microservicio poliglota que demuestra que la arquitectura contract-driven permite usar tecnologias fuera del ecosistema JVM. Gestiona el registro y ciclo de vida de clientes (ACTIVE/INACTIVE/SUSPENDED/BLOCKED). Usa Prisma como ORM (migraciones declarativas, type-safety) y KafkaJS para la integracion con el bus de eventos. Valida todas las entradas con Zod schemas.

- **Stack**: Node.js 20, TypeScript, Express 4, Prisma ORM, KafkaJS
- **Puerto**: 8085
- **Codigo**: [`components/customers-service/`](../../../components/customers-service/)

### Infraestructura (namespace: guidewire-infra)

#### [Apache Kafka](../infra/kafka/README.md) — Event Backbone

El bus de eventos central de la arquitectura. Opera en modo KRaft (sin ZooKeeper) gestionado por el operador Strimzi. Mantiene 9 topics organizados por dominio: billing (2), incidents (2), customers (2), policies (1), eventos sin clasificar (1) y dead-letter queue (1). Todos los mensajes se serializan en formato AVRO con schemas gobernados por Apicurio. Retencion de 7 dias por defecto.

- **Stack**: Apache Kafka 4.0 (Strimzi v0.50.0, KafkaNodePool CRD)
- **Puerto**: 9092

#### [PostgreSQL](../infra/postgres/README.md) — Base de Datos Relacional

Instancia compartida de PostgreSQL que implementa el patron database-per-service a nivel logico. Contiene 5 bases de datos aisladas (billing, incidents, customers, drools_audit, apicurio), cada una con su usuario y permisos dedicados. Las migraciones se gestionan por cada servicio (JPA auto-DDL, Prisma, Hibernate).

- **Stack**: PostgreSQL 16 Alpine
- **Puerto**: 5432 (interno), 15432 (expuesto)

#### [Apicurio Schema Registry](../infra/apicurio/README.md) — Gobernanza de Schemas

Registro centralizado de schemas que garantiza la compatibilidad de los contratos de eventos. Almacena los 6 schemas AVRO, los 6 specs OpenAPI y el spec AsyncAPI en grupos de artefactos separados. Aplica politica de compatibilidad FULL (forward + backward) para evitar roturas entre productores y consumidores. Los serializadores Kafka de cada servicio consultan el registry en tiempo de ejecucion.

- **Stack**: Apicurio Service Registry 2.5.11 (backend PostgreSQL)
- **Puerto**: 8081

#### [Apache ActiveMQ Artemis](../infra/activemq/README.md) — Mensajeria JMS

Broker de mensajeria para patrones punto-a-punto y request/reply con sistemas legacy que usan JMS/AMQP. Complementa a Kafka (que es para event streaming). Define 3 colas: solicitudes de facturacion desde claims, alertas de fraude y notificaciones salientes. El Camel Gateway lo utiliza para comunicacion sincrona con BillingCenter.

- **Stack**: Apache ActiveMQ Artemis 2.33 (operador AMQ Broker)
- **Puerto**: 61616 (AMQP), 8161 (consola Hawtio)

#### [3Scale API Gateway](../infra/threescale/README.md) — Gestion de APIs

Gateway de API empresarial que protege todos los endpoints publicos. Implementa autenticacion por API Key (header `X-API-Key`), rate limiting diferenciado por servicio (100-200 req/min) y enrutamiento a los backends. Configurado en modo standalone (sin base de datos) con configuracion declarativa JSON.

- **Stack**: Red Hat 3Scale APIcast
- **Puerto**: 8000 (proxy), 8001 (management)

#### [Kafdrop](../infra/kafka/README.md#kafdrop) — UI de Kafka

Interfaz web para inspeccion de topics, consumer groups y mensajes de Kafka. Permite verificar visualmente que los eventos se estan produciendo y consumiendo correctamente durante el desarrollo y las pruebas E2E.

- **Stack**: Kafdrop 4.0.1
- **Puerto**: 9000

---

## Data Flow Diagrams

### Flow 1: Policy Creation and Billing

This flow demonstrates how a policy created in PolicyCenter triggers invoice creation in the billing microservice.

```mermaid
sequenceDiagram
    participant PC as PolicyCenter
    participant CG as Camel Gateway
    participant DR as Drools Engine
    participant KF as Kafka
    participant BS as Billing Service

    PC->>CG: SOAP/REST request
    CG->>DR: Validate policy
    DR-->>CG: Validation result
    Note over CG: [if valid] Transform SOAP to AVRO
    CG->>KF: Produce billing.invoice-created
    CG-->>PC: HTTP 201
    KF->>BS: Consume event
    Note over BS: Create invoice (status: PENDING)<br/>Store in PostgreSQL
```

#### Invoice State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> PROCESSING
    PENDING --> CANCELLED
    PROCESSING --> COMPLETED
    PROCESSING --> FAILED
    FAILED --> PENDING : retry
    COMPLETED --> [*]
    CANCELLED --> [*]
```

### Flow 2: Claim Processing and Incident Management

This flow shows how a claim from ClaimCenter creates an incident, goes through fraud detection rules, and progresses through the incident lifecycle.

```mermaid
sequenceDiagram
    participant CC as ClaimCenter
    participant CG as Camel Gateway
    participant DR as Drools Engine
    participant KF as Kafka
    participant IS as Incidents Service

    CC->>CG: REST POST claim
    CG->>DR: Evaluate fraud + routing rules
    DR-->>CG: Priority + flags
    Note over CG: Transform to AVRO<br/>Enrich with priority
    CG->>KF: Produce incidents.incident-created
    CG-->>CC: HTTP 201
    KF->>IS: Consume event
    Note over IS: Create incident (status: OPEN)<br/>Store in PostgreSQL
```

#### Incident State Machine

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> IN_PROGRESS
    OPEN --> ESCALATED
    IN_PROGRESS --> RESOLVED
    IN_PROGRESS --> ESCALATED
    ESCALATED --> IN_PROGRESS
    ESCALATED --> RESOLVED
    RESOLVED --> CLOSED
    CLOSED --> [*]
```

### Flow 3: Customer Registration (Cross-Domain)

This flow illustrates customer registration and how the event is consumed by multiple services for cross-domain data synchronization.

```mermaid
sequenceDiagram
    participant Client as API Client
    participant CG as Camel Gateway
    participant KF as Kafka
    participant CS as Customers Service
    participant BS as Billing Service
    participant IS as Incidents Service

    Client->>CG: POST customer
    CG->>KF: Produce customers.customer-registered
    CG-->>Client: HTTP 201

    par customers-svc-group
        KF->>CS: Consume event
        Note over CS: Register customer (ACTIVE)<br/>Store in PostgreSQL
    and billing-svc-group
        KF->>BS: Consume event
        Note over BS: Cache customer ref<br/>for invoice linking
    and incidents-svc-group
        KF->>IS: Consume event
        Note over IS: Cache customer ref<br/>for incident linking
    end
```

#### Customer State Machine

```mermaid
stateDiagram-v2
    [*] --> ACTIVE
    ACTIVE --> INACTIVE
    INACTIVE --> ACTIVE
    ACTIVE --> SUSPENDED
    SUSPENDED --> ACTIVE
    SUSPENDED --> BLOCKED
    ACTIVE --> BLOCKED
    BLOCKED --> [*]
```

---

## Architecture Decision Records (ADRs)

### ADR-001: API-First / Contract-Driven Development

**Status**: Accepted

**Context**: The integration between Guidewire InsuranceSuite and internal microservices requires well-defined interfaces. Teams working on different services need stable contracts to develop in parallel without tight coupling.

**Decision**: Adopt API-First development where all interfaces (REST APIs, async events, data schemas) are defined as machine-readable contracts (OpenAPI 3.1, AsyncAPI 3.0, Apache AVRO) before any implementation begins. Contracts live in the `contracts/` directory and serve as the single source of truth.

**Consequences**:
- Positive: Parallel team development is possible from day one
- Positive: Contract validation can be automated in CI/CD (Spectral, AsyncAPI CLI)
- Positive: Code generation from contracts reduces boilerplate and drift
- Positive: Apicurio Schema Registry enforces AVRO schema compatibility at runtime
- Negative: Upfront design effort is higher
- Negative: Contract changes require coordination across teams

---

### ADR-002: Red Hat OpenShift Local (CRC) over Vagrant + Podman Compose

**Status**: Accepted (supersedes original ADR-002: Podman over Docker)

**Context**: The POC needs a local development environment that closely mirrors production OpenShift. The original approach used Vagrant + libvirt/KVM to create a VM running Podman Compose. While functional, this added an extra virtualization layer and didn't exercise Kubernetes/OpenShift primitives (Operators, Routes, Services, BuildConfigs).

**Decision**: Use Red Hat OpenShift Local (CRC) as the local development platform. CRC runs a single-node OpenShift 4.x cluster with full Kubernetes API, Operator Lifecycle Manager, and enterprise features. Podman Compose is retained as a legacy alternative.

**Consequences**:
- Positive: Real OpenShift environment matches production (Operators, Routes, RBAC, SCC)
- Positive: Strimzi, AMQ Broker, and Apicurio operators manage infrastructure lifecycle
- Positive: Cross-namespace service discovery via Kubernetes DNS
- Positive: BuildConfig + ImageStream provide native container build pipeline
- Positive: No extra VM layer -- CRC manages its own lightweight VM
- Negative: CRC requires a free Red Hat account for pull secret
- Negative: Higher resource baseline (~9GB RAM minimum for CRC itself)
- Negative: Operator startup can be slow on first deployment

---

### ADR-003: Apache Kafka over RabbitMQ

**Status**: Accepted

**Context**: The system needs an event backbone for asynchronous communication between the Camel Gateway and downstream microservices. The events represent domain state changes (invoices, incidents, customers) that may need replay, auditing, and multi-consumer patterns.

**Decision**: Use Apache Kafka (in KRaft mode, without ZooKeeper) as the primary event streaming platform. Use AVRO serialization with Apicurio Schema Registry for schema governance.

**Consequences**:
- Positive: Kafka provides durable, ordered, replayable event log -- essential for event sourcing patterns
- Positive: Multiple consumer groups can independently consume the same events (fan-out)
- Positive: KRaft mode eliminates ZooKeeper dependency, simplifying operations
- Positive: AVRO + Schema Registry enables schema evolution with backward/forward compatibility
- Positive: Kafka is the industry standard for event-driven architectures at scale
- Positive: Kafdrop provides a web UI for topic inspection and debugging
- Negative: Higher operational complexity compared to RabbitMQ for simple pub/sub
- Negative: KRaft is relatively newer (GA since Kafka 3.3) compared to ZooKeeper mode
- Negative: AVRO serialization adds complexity vs plain JSON

**Note**: ActiveMQ Artemis is also included in the stack for JMS-based messaging with legacy systems that require point-to-point or traditional request/reply patterns.

---

### ADR-004: Polyglot Microservices

**Status**: Accepted

**Context**: The POC needs to demonstrate that microservices in an insurance integration platform can use different technology stacks while maintaining interoperability through well-defined contracts and Kafka events.

**Decision**: Implement microservices using three different technology stacks:
- **Billing Service**: Java 21 + Spring Boot 3.3 (mainstream enterprise framework)
- **Incidents Service**: Java 21 + Quarkus 3.8 (cloud-native, fast startup, low memory)
- **Customers Service**: Node.js 20 + TypeScript + Express (non-JVM alternative)
- **Camel Gateway**: Java 21 + Spring Boot 3.3 + Apache Camel 4 (integration patterns)
- **Drools Engine**: Java 21 + Spring Boot 3.3 + Drools 8 (business rules)

**Consequences**:
- Positive: Demonstrates true microservice independence -- each team can pick their best tool
- Positive: Proves that API-First contracts enable technology heterogeneity
- Positive: Quarkus demonstrates cloud-native Java benefits (fast boot, low RSS)
- Positive: Node.js shows that non-JVM services integrate seamlessly via Kafka + REST
- Positive: Validates the contract-driven approach across language boundaries
- Negative: Broader skill set required from the team
- Negative: Shared tooling (monitoring, logging, tracing) must be language-agnostic
- Negative: Each service has its own build pipeline and dependency management

---

### ADR-005: AVRO over Protobuf / JSON Schema for Event Serialization

**Status**: Accepted

**Context**: Kafka events need a serialization format that supports schema evolution, is compact on the wire, and integrates well with the chosen Schema Registry (Apicurio).

**Decision**: Use Apache AVRO as the serialization format for all Kafka events. Schemas are stored in `contracts/avro/` and registered in Apicurio Schema Registry.

**Consequences**:
- Positive: AVRO is the de facto standard in the Kafka ecosystem
- Positive: Native support in Apicurio Schema Registry (compatibility checks, versioning)
- Positive: Binary encoding is compact and efficient
- Positive: Schema evolution rules (backward, forward, full) are well-defined
- Positive: Both Java (Camel, Spring Kafka) and Node.js (kafkajs + schema-registry) have mature AVRO support
- Negative: AVRO schemas are more verbose than Protobuf definitions
- Negative: Debugging binary messages requires schema-aware tooling (Kafdrop with registry integration)
- Negative: Schema registry becomes a critical infrastructure dependency

---

### ADR-006: 3Scale API Gateway over Kong / Traefik

**Status**: Accepted

**Context**: The POC needs an API Gateway for rate limiting, authentication, and API management. The target enterprise environment uses Red Hat middleware.

**Decision**: Use Red Hat 3Scale (APIcast) as the API Gateway, configured declaratively via JSON.

**Consequences**:
- Positive: Full alignment with Red Hat enterprise stack (OpenShift, Fuse, AMQ)
- Positive: APIcast supports declarative configuration (no database required for POC)
- Positive: Production-grade API management features (rate limiting, analytics, developer portal)
- Positive: Demonstrates enterprise API governance patterns
- Negative: Heavier footprint than lightweight alternatives (Traefik, Envoy)
- Negative: Less community documentation compared to Kong or Traefik
- Negative: APIcast configuration model is specific to 3Scale

---

### ADR-007: Apache Camel as Integration Gateway

**Status**: Accepted

**Context**: Integration with Guidewire requires protocol mediation (SOAP to REST), message transformation, content-based routing, and error handling. These are classic Enterprise Integration Patterns (EIP).

**Decision**: Use Apache Camel 4 (embedded in Spring Boot) as the integration gateway layer between external Guidewire systems and internal Kafka topics.

**Consequences**:
- Positive: Camel implements 60+ Enterprise Integration Patterns natively
- Positive: Built-in support for SOAP, REST, Kafka, JMS, and hundreds of other protocols
- Positive: Spring Boot integration provides familiar configuration and management
- Positive: Strong alignment with Red Hat Fuse / Camel K for production deployment
- Positive: Route definitions are declarative and testable
- Negative: Camel has a steep learning curve for developers unfamiliar with EIP
- Negative: Debugging complex routes with multiple transformations can be challenging
- Negative: Adds an additional service to the deployment topology

---

## Technology Stack Summary

| Layer               | Technology                  | Version | Purpose                                      |
|---------------------|-----------------------------|---------|----------------------------------------------|
| Platform            | Red Hat OpenShift Local (CRC) | 4.x   | Single-node OpenShift cluster                 |
| Orchestration       | Kubernetes / OpenShift      | 4.x     | Pod scheduling, service discovery, routes     |
| API Gateway         | Red Hat 3Scale (APIcast)    | latest  | Rate limiting, auth, API management           |
| Integration         | Apache Camel 4              | 4.4     | EIP, protocol mediation, routing              |
| Event Streaming     | Apache Kafka (KRaft)        | 4.0     | Event backbone, durable log (Strimzi v0.50.0) |
| JMS Messaging       | Apache ActiveMQ Artemis     | 2.33    | Point-to-point messaging, legacy integration  |
| Rules Engine        | Drools 8                    | 8.x     | Business rules, fraud detection, validation   |
| Schema Registry     | Apicurio Service Registry   | 2.5     | AVRO schema governance, compatibility checks  |
| Database            | PostgreSQL                  | 16      | Relational storage for all microservices      |
| Java Runtime        | Eclipse Temurin             | 21      | LTS Java distribution                         |
| Node.js Runtime     | Node.js LTS                 | 20      | JavaScript/TypeScript runtime                 |
| Java Framework 1    | Spring Boot                 | 3.3     | Billing, Camel Gateway, Drools                |
| Java Framework 2    | Quarkus                     | 3.8     | Incidents Service (cloud-native)              |
| Node.js Framework   | Express 4 + TypeScript      | 4.x     | Customers Service                             |
| API Specs           | OpenAPI 3.1                 | 3.1     | REST API contracts                            |
| Event Specs         | AsyncAPI 3.0                | 3.0     | Async event contracts                         |
| Serialization       | Apache AVRO                 | 1.9+    | Kafka message serialization                   |
| Kafka UI            | Kafdrop                     | 4.0     | Topic inspection, message browsing            |
