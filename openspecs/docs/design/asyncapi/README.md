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

## Serialización

Todos los mensajes usan serialización **AVRO binaria** con schema ID resuelto desde Apicurio Service Registry.

## Herramientas

- **AsyncAPI Studio**: diseño visual de la spec
- **Spectral**: linting con ruleset asyncapi
- **Microcks**: generación de mocks desde la spec

## Spec de referencia

- [spec.yml](../../../design/asyncapi/guidewire-events/spec.yml)
- Issue: [#38](../../../../issues/38)
