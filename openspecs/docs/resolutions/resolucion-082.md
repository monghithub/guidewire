# Resolucion Issue #82: Evaluar AsyncAPI codegen para Kafka

> **Issue:** [#82 - Evaluar AsyncAPI codegen para productores/consumidores Kafka](https://github.com/monghithub/guidewire/issues/82)
> **Prioridad:** P1
> **Estado:** Resuelta (evaluacion completada)

## Diagnostico

La spec AsyncAPI 3.0 (`contracts/asyncapi/guidewire-events.yml`) define 7 canales Kafka
pero no se genera codigo desde ella. Los productores/consumidores estan hand-coded.

## Evaluacion realizada

| Herramienta | Madurez | Java | TypeScript | Veredicto |
|-------------|---------|------|------------|-----------|
| @asyncapi/generator | Estable | Spring Cloud Stream | NATS (no Kafka) | Parcialmente viable |
| @asyncapi/modelina | Estable | Si | Si | Solo modelos |

### Limitaciones encontradas

1. No hay template maduro para **Quarkus SmallRye** (incidents-service)
2. El template de Java Spring Cloud Stream no se ajusta al stack Camel del gateway
3. Los schemas Avro ya se generan con `avro-maven-plugin` — duplicar generacion no aporta

## Decision

**Fase 1 (actual):** Usar AsyncAPI para documentacion y validacion en CI unicamente.
**Fase 2 (futuro):** Adoptar codegen cuando haya templates maduros para Kafka + Quarkus.

## Entregable

Creado `contracts/asyncapi/README.md` con la evaluacion completa y recomendacion
por fases, para que el equipo tenga la decision documentada.

## Verificacion

```bash
cat contracts/asyncapi/README.md
```
