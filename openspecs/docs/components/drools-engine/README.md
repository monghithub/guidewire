# Drools Rules Engine — Documentación

> [Volver a OpenSpecs](../../README.md) · [Volver al README principal](../../../../README.md)

## Descripción

Motor de reglas de negocio basado en Drools 8.x. Evalúa reglas de fraude, validaciones de pólizas, cálculo de comisiones y enrutamiento de incidencias. Invocado por Camel Gateway via REST.

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.x |
| Reglas | Drools 8.x |
| Build | Maven |
| Puerto | **8086** |

## API REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/rules/fraud-check` | Evalúa riesgo de fraude |
| POST | `/api/v1/rules/policy-validation` | Valida elegibilidad de póliza |
| POST | `/api/v1/rules/commission` | Calcula comisión |
| POST | `/api/v1/rules/incident-routing` | Asigna equipo a incidencia |

## Reglas de Negocio

### Detección de Fraude (`fraud-detection.drl`)

| Regla | Condición | Resultado |
|-------|-----------|-----------|
| HighAmountClaim | claimedAmount > 500,000 MXN | HIGH_RISK |
| FrequentClaimant | > 3 claims en 12 meses | MEDIUM_RISK |
| NewCustomerImmediateClaim | Claim dentro de 30 días del registro | HIGH_RISK |
| SuspiciousAmount | claimedAmount > 1,000,000 MXN | CRITICAL_RISK |

### Validación de Pólizas (`policy-validation.drl`)

| Regla | Condición | Resultado |
|-------|-----------|-----------|
| MaxAutoLimit | AUTO y prima > 100,000 | Rechazar |
| CustomerNotActive | Status != ACTIVE | Rechazar |

### Comisiones (`commission-calculation.drl`)

| Producto | Base | Directo | Broker |
|----------|------|---------|--------|
| AUTO | 5% | 4% | 7% |
| HOME | 7% | 6% | 9% |
| LIFE | 10% | 9% | 12% |
| HEALTH | 8% | 7% | 10% |
| COMMERCIAL | 6% | 5% | 8% |

### Enrutamiento (`incident-routing.drl`)

| Condición | Equipo Asignado |
|-----------|----------------|
| Priority CRITICAL | senior-adjusters |
| Monto > 1,000,000 MXN | specialist |
| Default | standard-adjusters |

## Flujo de Evaluación de Reglas

```mermaid
flowchart TD
    REQ[Request desde Camel Gateway] --> API[REST API :8086]
    API --> ROUTE{Endpoint}
    ROUTE -->|/fraud-check| FC[fraud-detection.drl]
    ROUTE -->|/policy-validation| PV[policy-validation.drl]
    ROUTE -->|/commission| CC[commission-calculation.drl]
    ROUTE -->|/incident-routing| IR[incident-routing.drl]

    FC --> FC1{claimedAmount<br/>> 1,000,000?}
    FC1 -->|Si| CRIT[CRITICAL_RISK]
    FC1 -->|No| FC2{claimedAmount<br/>> 500,000?}
    FC2 -->|Si| HIGH[HIGH_RISK]
    FC2 -->|No| FC3{> 3 claims<br/>en 12 meses?}
    FC3 -->|Si| MED[MEDIUM_RISK]
    FC3 -->|No| FC4{Claim < 30 dias<br/>del registro?}
    FC4 -->|Si| HIGH
    FC4 -->|No| LOW[LOW_RISK]

    IR --> IR1{Priority<br/>CRITICAL?}
    IR1 -->|Si| SENIOR[senior-adjusters]
    IR1 -->|No| IR2{Monto<br/>> 1,000,000?}
    IR2 -->|Si| SPEC[specialist]
    IR2 -->|No| STD[standard-adjusters]

    style REQ fill:#ff9,stroke:#333
    style CRIT fill:#f66,stroke:#333,color:#fff
    style HIGH fill:#f96,stroke:#333
    style MED fill:#fc9,stroke:#333
    style LOW fill:#9f9,stroke:#333
```

## Ejemplo de Invocación

```bash
curl -sk -X POST https://drools-engine-guidewire-apps.apps-crc.testing/api/v1/rules/fraud-check \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "uuid-here",
    "customerId": "uuid-here",
    "claimedAmount": 750000,
    "claimCount": 5,
    "incidentDate": "2024-06-15",
    "customerRegistrationDate": "2024-06-01"
  }'
```

Respuesta:
```json
{
  "riskLevel": "HIGH",
  "flaggedReasons": ["HighAmountClaim", "FrequentClaimant", "NewCustomerImmediateClaim"]
}
```

## Notas de Implementación

### Safety net: `fireAllRules(100)`

El `RulesService.java` limita la ejecución a un máximo de 100 activaciones de reglas por sesión. Esto previene loops infinitos en caso de reglas mal escritas.

### Reglas DRL — prevención de loops

Las reglas DRL evitan el uso de `update(fact)` que puede causar re-evaluación infinita entre reglas. En su lugar:

- Se usan setters directos (`fact.setField(value)`) para campos que no afectan condiciones de otras reglas
- Se usa `modify(fact) { ... }` solo cuando es necesario propagar cambios al motor
- Las reglas de asignación de nivel de riesgo están consolidadas en una sola regla con lógica Java (`if/else`) para evitar cross-activation

## Base de Datos

El Drools Engine utiliza una base de datos PostgreSQL para almacenar el audit trail de las evaluaciones de reglas.

| Parámetro | Valor |
|-----------|-------|
| Base de datos | `drools_audit` |
| Usuario | `drools_user` |
| Password | `drools123` |
| JDBC URL | `jdbc:postgresql://postgres.guidewire-infra.svc.cluster.local:5432/drools_audit` |

La base se crea automáticamente en el init script de PostgreSQL (`configmap-init-db.yml`).

## Documentacion relacionada

- [Camel Integration Gateway](../camel-gateway/README.md)
- [OpenAPI](../../design/openapi/README.md)

## Spec de referencia

- [spec.yml](../../../components/drools-engine/spec.yml)
- Issues: [#52](../../../../issues/52), [#53](../../../../issues/53)
