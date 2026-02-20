# AsyncAPI Code Generation - Evaluation

## Current State

The AsyncAPI 3.0 spec (`guidewire-events.yml`) documents all Kafka channels but does
not generate code. Producers/consumers are hand-coded in each service.

## Evaluated Options

| Tool | Maturity | Java Support | TypeScript Support | Verdict |
|------|----------|-------------|-------------------|---------|
| @asyncapi/generator | Stable | Spring Cloud Stream | NATS (not Kafka) | Partial fit |
| @asyncapi/modelina | Stable | Yes | Yes | Models only |

## Recommendation

**Phase 1 (current):** Use AsyncAPI spec for documentation and CI validation only.
The Avro schemas already generate Java classes via `avro-maven-plugin`.

**Phase 2 (future):** When `@asyncapi/generator` has mature Kafka + Quarkus templates,
adopt code generation for producer/consumer interfaces.

## Validation

The CI pipeline validates AsyncAPI spec syntax. Kafka topic names in code should be
kept in sync manually with the `channels` defined in the spec.
