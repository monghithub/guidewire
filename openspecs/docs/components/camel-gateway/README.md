# Camel Integration Gateway — Documentación

## Descripción

Gateway de integración central basado en Apache Camel 4.x + Spring Boot 3.3. Actúa como mediador entre Guidewire (SOAP/REST) y el ecosistema de microservicios. Implementa patrones EIP.

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.x |
| Integración | Apache Camel 4.x |
| Build | Maven |
| Puerto | **8083** |

## Patrones EIP Implementados

| Patrón | Uso |
|--------|-----|
| Message Translator | Conversión XML ↔ JSON |
| Content-Based Router | Enrutamiento por tipo de operación |
| Protocol Bridge | SOAP ↔ REST |
| Dead Letter Channel | Manejo de errores con DLQ |
| Wire Tap | Logging/auditoría asíncrona |

## Rutas

### REST (expone APIs de Guidewire)

```
GET/POST/PATCH /api/v1/policies   → mock-guidewire (PolicyCenter)
GET/POST/PATCH /api/v1/claims     → mock-guidewire (ClaimCenter)
GET/POST/PATCH /api/v1/gw-invoices → mock-guidewire (BillingCenter)
```

### Kafka Producer

```
direct:invoice-created     → kafka:billing.invoice-created
direct:incident-created    → kafka:incidents.incident-created
direct:customer-registered → kafka:customers.customer-registered
```

### Kafka Consumer

```
kafka:billing.invoice-created     → http:billing-service:8082
kafka:incidents.incident-created  → http:incidents-service:8084
kafka:customers.customer-registered → http:customers-service:8085
```

## Flujo de Rutas Camel

```mermaid
graph LR
    GW[Guidewire<br/>SOAP/REST] -->|XML/JSON| CAMEL[Camel Gateway<br/>:8083]
    CAMEL -->|Transform<br/>XML↔JSON| TRANSFORM[Message<br/>Translator]
    TRANSFORM --> ROUTER{Content-Based<br/>Router}
    ROUTER -->|Invoices| KI[Kafka<br/>billing.invoice-created]
    ROUTER -->|Incidents| KN[Kafka<br/>incidents.incident-created]
    ROUTER -->|Customers| KC[Kafka<br/>customers.customer-registered]
    KI --> BS[Billing Service<br/>:8082]
    KN --> IS[Incidents Service<br/>:8084]
    KC --> CS[Customers Service<br/>:8085]

    CAMEL -->|REST call| DROOLS[Drools Engine<br/>:8086]
    DROOLS -->|Enriched| CAMEL

    style GW fill:#f9d,stroke:#333
    style CAMEL fill:#ff9,stroke:#333
    style DROOLS fill:#9cf,stroke:#333
    style BS fill:#9f9,stroke:#333
    style IS fill:#9f9,stroke:#333
    style CS fill:#9f9,stroke:#333
```

## Integración con Drools

Antes de publicar eventos a Kafka, Camel invoca Drools para enriquecer los mensajes:

```
Guidewire → Camel → Drools (evalúa reglas) → Kafka (evento enriquecido) → Microservicios
```

## Observabilidad

| Feature | Endpoint |
|---------|----------|
| Health (liveness) | `/actuator/health/liveness` |
| Health (readiness) | `/actuator/health/readiness` |
| Métricas Prometheus | `/actuator/prometheus` |
| Info | `/actuator/info` |

## Dependencias

Requiere: Kafka, Apicurio, ActiveMQ (deben estar healthy antes de arrancar).

## Spec de referencia

- [spec.yml](../../../components/camel-gateway/spec.yml)
- Issues: [#45](../../../../issues/45) - [#51](../../../../issues/51)
