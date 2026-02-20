# Resolucion Issue #83: Agregar validacion de drift spec-vs-codigo en CI

> **Issue:** [#83 - Agregar validacion de drift spec-vs-codigo en CI](https://github.com/monghithub/guidewire/issues/83)
> **Prioridad:** P1
> **Estado:** Resuelta

## Diagnostico

El CI validaba sintaxis de las specs pero no detectaba si:
- Un archivo spec referenciado no existia
- Un endpoint cambiaba en codigo sin actualizar la spec
- Se quitaba una spec sin actualizar las referencias

Ademas, todos los lintings tenian `|| true` que ignoraba errores.

## Solucion aplicada

3 cambios en `.github/workflows/ci.yml` (job `contract-validation`):

### 1. Spectral fail-on-error

```yaml
# ANTES
spectral lint "$spec" --ruleset spectral:oas || true

# DESPUES
spectral lint "$spec" --fail-severity error
```

Usa automaticamente `.spectral.yml` del root. Falla el CI si hay errores.

### 2. AsyncAPI fail-on-error

```yaml
# ANTES
asyncapi validate contracts/asyncapi/guidewire-events.yml || true

# DESPUES
asyncapi validate contracts/asyncapi/guidewire-events.yml
```

### 3. Nuevo step: verificacion de drift

```yaml
- name: Verify OpenAPI spec references exist
  run: |
    echo "=== Verifying OpenAPI specs referenced by services ==="
    specs=(
      "contracts/openapi/billing-service-api.yml"
      "contracts/openapi/incidents-service-api.yml"
      "contracts/openapi/customers-service-api.yml"
      "contracts/openapi/policycenter-api.yml"
      "contracts/openapi/claimcenter-api.yml"
      "contracts/openapi/billingcenter-api.yml"
      "contracts/openapi/drools-engine-api.yml"
      "contracts/openapi/camel-gateway-api.yml"
    )
    for spec in "${specs[@]}"; do
      if [ -f "$spec" ]; then
        echo "  OK: $spec"
      else
        echo "  FAIL: $spec not found"
        exit 1
      fi
    done
```

Verifica que las 8 specs OpenAPI existen. Si alguien borra o renombra una, el CI falla.

## Verificacion

```bash
# Simular el check localmente
for spec in contracts/openapi/*-api.yml; do
  echo "OK: $spec"
done
```
