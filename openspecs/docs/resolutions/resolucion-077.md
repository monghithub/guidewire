# Resolucion Issue #77: Activar camel-gateway OpenAPI codegen

> **Issue:** [#77 - Activar camel-gateway OpenAPI codegen](https://github.com/monghithub/guidewire/issues/77)
> **Prioridad:** P0
> **Estado:** Resuelta

## Diagnostico

El plugin `openapi-generator-maven-plugin` en camel-gateway tenia dos problemas:

1. `<skip>true</skip>` — generacion deshabilitada
2. `<inputSpec>` apuntaba a `src/main/resources/openapi/guidewire-api.yml` que no existia

## Solucion aplicada

### 1. Creada spec del gateway

Nuevo archivo `contracts/openapi/camel-gateway-api.yml` (652 lineas, OpenAPI 3.1.0):

| Tag | Endpoints | Backend |
|-----|-----------|---------|
| Policies | GET/POST/PATCH /api/v1/policies | PolicyCenter |
| Claims | GET/POST /api/v1/claims | ClaimCenter |
| GW Invoices | GET/POST /api/v1/gw-invoices | BillingCenter |

10 endpoints totales con schemas para Policy, Claim, GwInvoice y sus requests.

### 2. Corregido pom.xml

4 cambios en `components/camel-gateway/pom.xml`:

```xml
<!-- ANTES -->
<skip>true</skip>
<inputSpec>${project.basedir}/src/main/resources/openapi/guidewire-api.yml</inputSpec>
<generatorName>java</generatorName>
<library>resttemplate</library>

<!-- DESPUES -->
<skip>false</skip>
<inputSpec>${project.basedir}/../../contracts/openapi/camel-gateway-api.yml</inputSpec>
<generatorName>spring</generatorName>
<!-- library eliminado -->
<configOptions>
    <interfaceOnly>true</interfaceOnly>
    <useSpringBoot3>true</useSpringBoot3>
    <useJakartaEe>true</useJakartaEe>
</configOptions>
```

## Verificacion

```bash
cd components/camel-gateway
mvn generate-sources -B
ls target/generated-sources/openapi/
```
