# Resolucion Issue #84: Normalizar nombres de specs OpenAPI

> **Issue:** [#84 - Normalizar nombres de specs OpenAPI](https://github.com/monghithub/guidewire/issues/84)
> **Prioridad:** P2
> **Estado:** Resuelta

## Diagnostico

Los nombres de archivos eran inconsistentes entre los pom.xml y los archivos reales:

| Servicio | Referencia en pom.xml | Archivo real |
|----------|----------------------|--------------|
| billing-service | `billing-service.yml` | `billing-service-api.yml` |
| camel-gateway | `guidewire-api.yml` | (no existia) |

## Solucion aplicada

### Convencion adoptada: sufijo `-api.yml`

Todos los archivos en `contracts/openapi/` usan el patron `{nombre}-api.yml`:

```
contracts/openapi/
  billing-service-api.yml      # ya existia
  incidents-service-api.yml    # ya existia
  customers-service-api.yml    # ya existia
  policycenter-api.yml         # ya existia
  claimcenter-api.yml          # ya existia
  billingcenter-api.yml        # ya existia
  drools-engine-api.yml        # nuevo (issue #80)
  camel-gateway-api.yml        # nuevo (issue #77)
```

### Referencias corregidas

| pom.xml | Antes | Despues |
|---------|-------|---------|
| billing-service | `billing-service.yml` | `billing-service-api.yml` (issue #76) |
| camel-gateway | `guidewire-api.yml` | `camel-gateway-api.yml` (issue #77) |
| incidents-service | (no tenia) | `incidents-service-api.yml` (issue #78) |
| drools-engine | (no tenia) | `drools-engine-api.yml` (issue #80) |

No se renombro ningun archivo existente — solo se corrigieron las referencias en pom.xml
y se crearon los archivos nuevos con la convencion correcta.

### Enforcement en CI

El step de drift detection (issue #83) verifica que los 8 archivos existen,
garantizando que la convencion se mantiene.

## Verificacion

```bash
ls contracts/openapi/*-api.yml | wc -l
# Debe mostrar: 8
```
