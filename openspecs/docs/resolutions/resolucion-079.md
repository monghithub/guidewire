# Resolucion Issue #79: Agregar OpenAPI codegen a customers-service

> **Issue:** [#79 - Agregar OpenAPI codegen a customers-service](https://github.com/monghithub/guidewire/issues/79)
> **Prioridad:** P0
> **Estado:** Resuelta

## Diagnostico

El customers-service (Node.js/Express/TypeScript) tenia todos los types escritos a mano
en `src/types/index.ts` y validacion Zod manual en `src/validators/customer.validator.ts`.
No habia herramientas de generacion de tipos desde la spec OpenAPI.

## Solucion aplicada

### 1. Agregado openapi-typescript

En `components/customers-service/package.json`:

```json
{
  "devDependencies": {
    "openapi-typescript": "^7.4.0"
  },
  "scripts": {
    "generate:types": "openapi-typescript ../../contracts/openapi/customers-service-api.yml -o src/types/api.generated.ts",
    "build": "npm run generate:types && tsc"
  }
}
```

- `generate:types` genera tipos TypeScript desde la spec OpenAPI
- `build` ejecuta la generacion antes de compilar TypeScript

### 2. Creado placeholder de tipos generados

`src/types/api.generated.ts`:
- Header explicando que es auto-generado
- Re-exporta los types manuales para compatibilidad hasta que se ejecute `generate:types`

### Tipos que se generaran

Desde `contracts/openapi/customers-service-api.yml`:

| Schema OpenAPI | Type generado |
|----------------|---------------|
| Customer | `components['schemas']['Customer']` |
| CustomerStatus | Enum type |
| DocumentType | Enum type |
| CreateCustomerRequest | Request body type |
| UpdateCustomerRequest | Request body type |
| PaginatedCustomerList | Paginated response type |
| Address | Nested object type |
| ErrorResponse | Error shape type |

## Siguiente paso

```bash
cd components/customers-service
npm install
npm run generate:types
# Luego migrar src/types/index.ts para usar los tipos generados
```

## Verificacion

```bash
cd components/customers-service
npm run generate:types
cat src/types/api.generated.ts | head -20
npm run build
```
