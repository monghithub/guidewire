# Apache Kafka (KRaft) — Documentación

## Descripción

Broker de eventos en modo **KRaft** (sin ZooKeeper). Actúa como backbone de la arquitectura event-driven (EDA). Todos los eventos del ecosistema Guidewire fluyen por Kafka.

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Imagen | `apache/kafka:3.7.0` |
| Modo | KRaft (controller + broker en un nodo) |
| Puerto | **9092** |
| Retención | 7 días (168 horas) |
| Particiones por defecto | 3 |
| Auto-create topics | **Deshabilitado** |

## Topics

| Topic | Particiones | Retención | Descripción |
|-------|-------------|-----------|-------------|
| `billing.invoice-created` | 3 | 7 días | Factura creada en BillingCenter |
| `billing.invoice-status-changed` | 3 | 7 días | Cambio de estado de factura |
| `incidents.incident-created` | 3 | 7 días | Siniestro abierto en ClaimCenter |
| `incidents.incident-status-changed` | 3 | 7 días | Cambio de estado de siniestro |
| `customers.customer-registered` | 3 | 7 días | Nuevo cliente registrado |
| `customers.customer-status-changed` | 3 | 7 días | Cambio de estado de cliente |
| `dlq.errors` | 1 | 30 días | Dead Letter Queue |

## Flujo de eventos

```
Guidewire → Camel Gateway → Kafka Topics → Microservicios
                                 ↓
                          Drools (fraude)
```

## Monitoreo — Kafdrop

| Parámetro | Valor |
|-----------|-------|
| Imagen | `obsidiandynamics/kafdrop:4.0.1` |
| Puerto | **9000** |
| URL | http://localhost:9000 |

Kafdrop permite inspeccionar topics, particiones, consumer groups y mensajes individuales.

## Comandos útiles (dentro de la VM)

```bash
# Listar topics
podman exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Describir un topic
podman exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic billing.invoice-created

# Producir mensaje de prueba
echo '{"test": true}' | podman exec -i kafka kafka-console-producer.sh --bootstrap-server localhost:9092 --topic billing.invoice-created

# Consumir mensajes
podman exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic billing.invoice-created --from-beginning

# Ver consumer groups
podman exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

## Garantías de entrega

| Configuración | Valor | Descripción |
|--------------|-------|-------------|
| `acks` | `all` | Todos los replicas confirman |
| `min.insync.replicas` | `1` | POC (en prod: 2+) |
| `enable.auto.commit` | `false` | Commit manual de offsets |
| `auto.offset.reset` | `earliest` | Consumir desde el inicio |

## Spec de referencia

- [spec.yml](../../../infra/kafka/spec.yml)
- Issue: [#29](../../../../issues/29)
