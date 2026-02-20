# Resolucion Issue #72: Agregar tests unitarios a billing, incidents y customers

> **Issue:** [#72 - Agregar tests unitarios a billing-service, incidents-service y customers-service](https://github.com/monghithub/guidewire/issues/72)
> **Prioridad:** P1
> **Estado:** Resuelta

## Diagnostico

3 de los 5 microservicios no tenian tests unitarios:

| Servicio | Framework | Antes | Despues |
|----------|-----------|-------|---------|
| camel-gateway | Spring Boot | 1 test | 1 test (sin cambios) |
| drools-engine | Spring Boot | 4 clases | 4 clases (sin cambios) |
| **billing-service** | Spring Boot | 0 tests | 4 clases |
| **incidents-service** | Quarkus 3.8 | 0 tests | 4 clases |
| **customers-service** | Node.js/Express | 0 tests | 3 clases + jest config |

## Tests agregados

### billing-service (JUnit 5 + Mockito)

```
src/test/java/com/guidewire/billing/
  BillingServiceApplicationTests.java     # Context loads
  entity/
    InvoiceStatusTest.java                # Transiciones de estado validas/invalidas
  service/
    InvoiceServiceTest.java               # CRUD, transiciones, excepciones
  controller/
    InvoiceControllerTest.java            # MockMvc: endpoints REST
```

**InvoiceStatusTest** — Cubre la maquina de estados:
- PENDING -> PROCESSING, CANCELLED (validas)
- PROCESSING -> COMPLETED, FAILED (validas)
- COMPLETED -> nada (estado terminal)
- FAILED -> PENDING (retry)
- Transiciones invalidas: COMPLETED->PENDING, CANCELLED->PROCESSING, etc.

**InvoiceServiceTest** — Con `@ExtendWith(MockitoExtension.class)`:
- `create_shouldCreateInvoiceWithItems` — verifica creacion y mapeo
- `findById_shouldReturnInvoice_whenExists`
- `findById_shouldThrow_whenNotFound` — ResourceNotFoundException
- `updateStatus_shouldTransitionFromPendingToProcessing` — flujo valido + evento Kafka
- `updateStatus_shouldThrow_whenInvalidTransition` — InvalidStatusTransitionException

**InvoiceControllerTest** — Con `@WebMvcTest`:
- POST /api/v1/invoices -> 201 CREATED
- GET /api/v1/invoices/{id} -> 200 OK
- GET /api/v1/invoices/{id} (inexistente) -> 404
- PATCH /api/v1/invoices/{id} -> 200 OK

### incidents-service (JUnit 5 + Mockito + RESTAssured)

```
src/test/java/com/guidewire/incidents/
  IncidentsServiceApplicationTests.java   # @QuarkusTest context
  entity/
    IncidentStatusTest.java               # Transiciones de estado
  service/
    IncidentServiceTest.java              # CRUD, transiciones, paginacion
  resource/
    IncidentResourceTest.java             # @QuarkusTest + RESTAssured
```

**IncidentStatusTest** — Maquina de estados:
- OPEN -> IN_PROGRESS, ESCALATED, CLOSED
- IN_PROGRESS -> RESOLVED, ESCALATED
- RESOLVED -> CLOSED, IN_PROGRESS
- ESCALATED -> IN_PROGRESS, CLOSED
- CLOSED -> nada (estado terminal)

**IncidentServiceTest** — Con Mockito:
- `create_shouldCreateIncident` — con prioridad por defecto
- `findById_shouldReturnIncident_whenExists`
- `findById_shouldThrow_whenNotFound`
- `update_shouldTransitionFromOpenToInProgress`
- `update_shouldThrow_whenInvalidTransition` — CLOSED no admite transiciones
- `findAll_shouldReturnPagedResponse` — paginacion y filtros

**IncidentResourceTest** — Con `@QuarkusTest` + RESTAssured:
- POST /api/v1/incidents -> 201
- GET /api/v1/incidents/{id} -> 200
- GET /api/v1/incidents -> 200 con paginacion

### customers-service (Jest + ts-jest)

```
src/__tests__/
  customer.service.test.ts     # Business logic, transiciones de estado
  customer.validator.test.ts   # Validacion Zod
  customer.controller.test.ts  # Controller con mocks
```

**customer.service.test.ts** — Con Prisma mockeado:
- `create should create a customer successfully`
- `create should throw on duplicate email` — Prisma P2002
- `findById should return customer when exists`
- `findById should throw 404 when not found`
- `update should validate status transitions` — ACTIVE->INACTIVE ok, BLOCKED->ACTIVE falla
- `findAll should return paginated results`

**customer.validator.test.ts** — Schemas Zod:
- Validacion de input valido completo
- Rechazo de campos requeridos faltantes (firstName, lastName, email, documentType, documentNumber)
- Rechazo de email invalido
- Rechazo de documentType invalido
- UpdateCustomerSchema acepta updates parciales
- QuerySchema aplica defaults (page=1, size=20)

**customer.controller.test.ts** — Con service mockeado:
- `create should return 201 with customer data`
- `findById should return 200`
- `findAll should return paginated list`
- `update should return 200 with updated customer`

## Configuracion de Jest

Se verifico que `package.json` ya incluye:
- `jest: ^29.7.0`
- `@types/jest: ^29.5.14`
- `ts-jest: ^29.2.5`

Se agrego/verifico configuracion de Jest en `package.json`:

```json
{
  "scripts": {
    "test": "jest --passWithNoTests"
  }
}
```

## Dependencias de test utilizadas

| Servicio | Dependencias de test |
|----------|---------------------|
| billing-service | `spring-boot-starter-test` (JUnit 5, Mockito, MockMvc, AssertJ) |
| incidents-service | `quarkus-junit5`, `rest-assured` |
| customers-service | `jest`, `@types/jest`, `ts-jest` |

## Verificacion

```bash
# billing-service
cd components/billing-service && mvn test -B

# incidents-service
cd components/incidents-service && ./mvnw test -B

# customers-service
cd components/customers-service && npm test
```
