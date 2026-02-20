# ActiveMQ Artemis — Documentación

## Descripción

Broker de mensajería JMS/AMQP para patrones request/reply, transacciones y colas de trabajo. Complementa a Kafka para casos que requieren entrega garantizada punto a punto.

## Cuándo usar ActiveMQ vs Kafka

| Caso | ActiveMQ | Kafka |
|------|----------|-------|
| Request/Reply síncrono | Si | No |
| Transacciones JTA/XA | Si | No |
| Prioridad de mensajes | Si | No |
| Cola de trabajo (un solo consumer) | Si | Posible |
| Event streaming (múltiples consumers) | No | Si |
| Alta retención (días/semanas) | No | Si |
| Replay de eventos | No | Si |

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Imagen | `apache/activemq-artemis:2.33.0` |
| Puerto broker | **61616** (AMQP/JMS) |
| Puerto consola | **8161** (Hawtio) |
| Usuario | `admin` / `admin123` |
| Protocolos | AMQP 1.0, JMS, STOMP, MQTT, OpenWire |

## Colas

| Cola | Tipo | Descripción |
|------|------|-------------|
| `claims.invoice-requests` | anycast | Solicitudes de facturación desde ClaimCenter |
| `claims.fraud-alerts` | anycast | Alertas de fraude generadas por Drools |
| `notifications.outbound` | anycast | Notificaciones salientes (email, SMS) |

## Consola Web (Hawtio)

- URL: http://localhost:8161/console
- Credenciales: `admin` / `admin123`
- Permite: ver colas, enviar mensajes de prueba, inspeccionar consumers

## Conexión desde Camel

```yaml
# application.yml del Camel Gateway
activemq:
  broker-url: tcp://activemq:61616
  user: admin
  password: admin123
```

```java
// Camel route
from("jms:queue:claims.invoice-requests")
    .to("direct:process-invoice-request");
```

## Spec de referencia

- [spec.yml](../../../infra/activemq/spec.yml)
- Issue: [#32](../../../../issues/32)
