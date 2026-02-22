# AsyncAPI Spec — Documentación

> [Volver a OpenSpecs](../../../README.md) · [Volver al README principal](../../../../README.md)

## Descripción

Especificacion AsyncAPI **2.6.0** que documenta todos los canales de eventos Kafka del ecosistema Guidewire. Referencia los schemas AVRO registrados en Apicurio.

> **Nota**: Apicurio 2.5.x no soporta la visualizacion de AsyncAPI 3.0. El archivo en git (`contracts/asyncapi/guidewire-events.yml`) puede estar en 3.0, pero la version registrada en Apicurio es 2.6.0.

## Canales

| Canal | Topic Kafka | Producer | Consumers |
|-------|-------------|----------|-----------|
| billing-invoice-created | `billing.invoice-created` | Camel Gateway | Billing Service |
| billing-invoice-status-changed | `billing.invoice-status-changed` | Camel Gateway | Billing Service |
| incidents-incident-created | `incidents.incident-created` | Camel Gateway | Incidents Service, Drools |
| incidents-incident-status-changed | `incidents.incident-status-changed` | Camel Gateway | Incidents Service |
| customers-customer-registered | `customers.customer-registered` | Camel Gateway | Customers, Billing, Incidents |
| customers-customer-status-changed | `customers.customer-status-changed` | Camel Gateway | Customers Service |
| policies-policy-events | `policies.policy-events` | Camel Gateway | — |
| events-unclassified | `events.unclassified` | Camel Gateway | — (fallback) |
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

    T7[policies.policy-events]
    T8[events.unclassified]
    T9[dlq.errors]

    CG -->|publish| T1
    CG -->|publish| T2
    CG -->|publish| T3
    CG -->|publish| T4
    CG -->|publish| T5
    CG -->|publish| T6
    CG -->|publish| T7
    CG -->|publish| T8

    T1 -->|subscribe| BS[Billing Service]
    T2 -->|subscribe| BS
    T3 -->|subscribe| IS[Incidents Service]
    T3 -->|subscribe| DR[Drools]
    T4 -->|subscribe| IS
    T5 -->|subscribe| CSvc[Customers Service]
    T5 -->|subscribe| BS
    T5 -->|subscribe| IS
    T6 -->|subscribe| CSvc

    BS -.->|errors| T9
    IS -.->|errors| T9
    CSvc -.->|errors| T9
    T9 -->|subscribe| MON[Monitoring]
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

## Documentacion relacionada

- [Kafka](../../infra/kafka/README.md)
- [AVRO Schemas](../avro/README.md)
- [OpenAPI Specs](../openapi/README.md)
