# Resolucion Issue #81: Crear .spectral.yml con reglas custom

> **Issue:** [#81 - Crear .spectral.yml con reglas custom y hacer CI fail-on-error](https://github.com/monghithub/guidewire/issues/81)
> **Prioridad:** P1
> **Estado:** Resuelta

## Diagnostico

1. No existia archivo `.spectral.yml` — el CI usaba el ruleset default `spectral:oas`
2. Las fallas se ignoraban con `|| true`

## Solucion aplicada

### 1. Creado `.spectral.yml` en la raiz del proyecto

```yaml
extends: ["spectral:oas"]

rules:
  # --- Enforce documentation ---
  info-description: error
  info-contact: warn
  operation-description: warn
  operation-tag-defined: error
  operation-operationId: error

  # --- Naming conventions ---
  path-keys-no-trailing-slash: error
  no-eval-in-markdown: error
  no-script-tags-in-markdown: error

  # --- Schema validation ---
  oas3-valid-media-example: warn
  oas3-valid-schema-example: warn
  oas3-schema: error
  oas3-unused-component: warn

  # --- Request/Response ---
  operation-success-response: error
  no-$ref-siblings: error
```

### 2. CI actualizado (ver issue #83)

```bash
# ANTES
spectral lint "$spec" --ruleset spectral:oas || true

# DESPUES
spectral lint "$spec" --fail-severity error
```

Spectral detecta automaticamente `.spectral.yml` en el root del proyecto.

### Reglas clave

| Regla | Severidad | Que valida |
|-------|-----------|------------|
| info-description | error | Toda spec debe tener descripcion |
| operation-tag-defined | error | Todo endpoint debe tener tag |
| operation-operationId | error | Todo endpoint debe tener operationId |
| oas3-schema | error | Schemas validos segun OAS 3.x |
| operation-success-response | error | Todo endpoint debe definir respuesta exitosa |

## Verificacion

```bash
# Lint local
npm install -g @stoplight/spectral-cli
spectral lint contracts/openapi/*.yml --fail-severity error
```
