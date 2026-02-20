# AsyncAPI Spec — Documentación

## Descripción

Especificación AsyncAPI 3.0 que documenta todos los canales de eventos Kafka del ecosistema Guidewire. Referencia los schemas AVRO registrados en Apicurio.

## Canales

| Canal | Topic Kafka | Producer | Consumers |
|-------|-------------|----------|-----------|
| billing-invoice-created | `billing.invoice-created` | Camel Gateway | Billing Service |
| billing-invoice-status-changed | `billing.invoice-status-changed` | Camel Gateway | Billing Service |
| incidents-incident-created | `incidents.incident-created` | Camel Gateway | Incidents Service, Drools |
| incidents-incident-status-changed | `incidents.incident-status-changed` | Camel Gateway | Incidents Service |
| customers-customer-registered | `customers.customer-registered` | Camel Gateway | Customers, Billing, Incidents |
| customers-customer-status-changed | `customers.customer-status-changed` | Camel Gateway | Customers Service |
| dlq-errors | `dlq.errors` | Todos | Monitoring |

## Diagrama de Canales

```mermaid
graph TD
    CG[Camel Gateway]

    subgraph Billing Channels
        T1[billing.invoice-created]
        T2[billing.invoice-status-changed]
    end

    subgraph Incidents Channels
        T3[incidents.incident-created]
        T4[incidents.incident-status-changed]
    end

    subgraph Customers Channels
        T5[customers.customer-registered]
        T6[customers.customer-status-changed]
    end

    T7[dlq.errors]

    CG -->|publish| T1
    CG -->|publish| T2
    CG -->|publish| T3
    CG -->|publish| T4
    CG -->|publish| T5
    CG -->|publish| T6

    T1 -->|subscribe| BS[Billing Service]
    T2 -->|subscribe| BS
    T3 -->|subscribe| IS[Incidents Service]
    T3 -->|subscribe| DR[Drools]
    T4 -->|subscribe| IS
    T5 -->|subscribe| CSvc[Customers Service]
    T5 -->|subscribe| BS
    T5 -->|subscribe| IS
    T6 -->|subscribe| CSvc

    BS -.->|errors| T7
    IS -.->|errors| T7
    CSvc -.->|errors| T7
    T7 -->|subscribe| MON[Monitoring]
```

## Serialización

Todos los mensajes usan serialización **AVRO binaria** con schema ID resuelto desde Apicurio Service Registry.

## Herramientas

- **AsyncAPI Studio**: diseño visual de la spec
- **Spectral**: linting con ruleset asyncapi
- **Microcks**: generación de mocks desde la spec

## Spec de referencia

- [spec.yml](../../../design/asyncapi/guidewire-events/spec.yml)
- Issue: [#38](../../../../issues/38)
