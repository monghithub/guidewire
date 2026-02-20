# Resolucion Issue #80: Crear contrato OpenAPI para drools-engine

> **Issue:** [#80 - Crear contrato OpenAPI para drools-engine](https://github.com/monghithub/guidewire/issues/80)
> **Prioridad:** P0
> **Estado:** Resuelta

## Diagnostico

El drools-engine era el unico servicio sin contrato OpenAPI. Sus 4 endpoints existian
solo en codigo (`RulesController.java`) sin spec que los documentara.

## Solucion aplicada

### 1. Creada spec OpenAPI

Nuevo archivo `contracts/openapi/drools-engine-api.yml` (517 lineas, OpenAPI 3.1.0):

| Endpoint | Request | Response |
|----------|---------|----------|
| POST /api/v1/rules/fraud-check | ClaimFactRequest | ClaimFactResponse (riskLevel, fraudScore, flaggedReasons) |
| POST /api/v1/rules/policy-validation | PolicyFactRequest | PolicyFactResponse (eligible, rejectionReason, validationErrors) |
| POST /api/v1/rules/commission | CommissionFactRequest | CommissionFactResponse (commissionPercentage, commissionAmount, commissionTier) |
| POST /api/v1/rules/incident-routing | IncidentRoutingFactRequest | IncidentRoutingFactResponse (assignedTeam, slaHours, escalated) |

Enums definidos como schemas reutilizables:
- ClaimType: COLLISION, THEFT, FIRE, FLOOD, LIABILITY
- RiskLevel: CRITICAL_RISK, HIGH_RISK, MEDIUM_RISK, LOW_RISK
- AgentTier: JUNIOR, SENIOR, EXECUTIVE
- CommissionTier: BASE, SILVER, GOLD, PLATINUM
- Priority: LOW, MEDIUM, HIGH, CRITICAL
- Severity: MINOR, MODERATE, MAJOR, CATASTROPHIC
- CustomerTier: STANDARD, PREMIUM, VIP

Response schemas usan `allOf` para componer input + output fields.

### 2. Configurado codegen

Agregado `openapi-generator-maven-plugin` a `components/drools-engine/pom.xml`:

```xml
<inputSpec>${project.basedir}/../../contracts/openapi/drools-engine-api.yml</inputSpec>
<generatorName>spring</generatorName>
<apiPackage>com.guidewire.rules.api</apiPackage>
<modelPackage>com.guidewire.rules.model.generated</modelPackage>
```

Nota: `modelPackage` usa `.model.generated` para no colisionar con los modelos Lombok
existentes en `com.guidewire.rules.model` (ClaimFact, PolicyFact, etc.).

## Verificacion

```bash
cd components/drools-engine
mvn generate-sources -B
ls target/generated-sources/openapi/
```
