# Billing Service — Documentación

## Descripción

Microservicio de facturación implementado con Spring Boot 3.3. Gestiona facturas internas alimentadas por eventos de Guidewire BillingCenter via Kafka.

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.x |
| ORM | Spring Data JPA / Hibernate |
| DB | PostgreSQL 16 (base: `billing`) |
| Migración | Flyway |
| Kafka | Spring Kafka + AVRO |
| Puerto | **8082** |

## API REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/invoices` | Listar facturas (paginado, filtrable) |
| GET | `/api/v1/invoices/{id}` | Obtener factura por ID |
| POST | `/api/v1/invoices` | Crear factura |
| PATCH | `/api/v1/invoices/{id}` | Actualizar factura |

### Filtros disponibles (query params)

`status`, `policyId`, `customerId`, `dateFrom`, `dateTo`, `page`, `size`

## Modelo de Datos

### Tabla `invoices`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| id | UUID (PK) | Identificador único |
| policy_id | UUID | Póliza asociada |
| customer_id | UUID | Cliente |
| status | VARCHAR(20) | Estado actual |
| total_amount | DECIMAL(10,2) | Monto total |
| currency | VARCHAR(3) | Moneda (default MXN) |
| source_event | VARCHAR(255) | ID del evento Kafka origen |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Tabla `invoice_items`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| id | UUID (PK) | Identificador |
| invoice_id | UUID (FK) | Referencia a factura |
| description | VARCHAR(255) | Descripción del ítem |
| quantity | INTEGER | Cantidad |
| unit_price | DECIMAL(10,2) | Precio unitario |
| subtotal | DECIMAL(10,2) | Subtotal |

## Arquitectura de Capas

```mermaid
graph LR
    CLIENT[Cliente HTTP] --> CTRL[Controller<br/>REST API]
    CTRL --> SVC[Service<br/>Lógica de negocio]
    SVC --> REPO[Repository<br/>Spring Data JPA]
    REPO --> DB[(PostgreSQL<br/>billing)]
    KAFKA[Kafka Consumer] --> SVC

    style CLIENT fill:#f9d,stroke:#333
    style CTRL fill:#ff9,stroke:#333
    style SVC fill:#9cf,stroke:#333
    style REPO fill:#9f9,stroke:#333
    style DB fill:#ccc,stroke:#333
    style KAFKA fill:#fc9,stroke:#333
```

## Estados y Transiciones

```
PENDING → PROCESSING → COMPLETED
   ↓          ↓
CANCELLED   FAILED → PENDING (retry)
```

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> PROCESSING
    PENDING --> CANCELLED
    PROCESSING --> COMPLETED
    PROCESSING --> FAILED
    FAILED --> PENDING : retry
    COMPLETED --> [*]
    CANCELLED --> [*]
```

## Kafka Consumers

| Topic | Acción |
|-------|--------|
| `billing.invoice-created` | Crear factura desde evento BillingCenter |
| `customers.customer-registered` | Registrar referencia de cliente |

## Validaciones

- `totalAmount` debe ser > 0
- Al menos un item requerido
- Transiciones de estado validadas (409 si inválida)

## Spec de referencia

- [spec.yml](../../../components/billing-service/spec.yml)
- Issues: [#54](../../../../issues/54) - [#57](../../../../issues/57)
