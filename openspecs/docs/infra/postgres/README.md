# PostgreSQL — Documentación

> [Volver a OpenSpecs](../../../README.md) · [Volver al README principal](../../../../README.md)

## Descripción

Instancia PostgreSQL 16 compartida que aloja 5 bases de datos lógicas, una por cada servicio que requiere persistencia. Se respeta el patrón **database-per-service**: cada servicio tiene su propia base de datos y usuario con permisos aislados.

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Imagen | `docker.io/library/postgres:16-alpine` |
| Puerto interno | 5432 |
| Puerto expuesto | **15432** (remapeado para evitar conflictos) |
| Superusuario | `postgres` / `postgres123` |
| Volumen | `pgdata` → `/var/lib/postgresql/data` |

## DNS (OpenShift)

| Contexto | Dirección |
|----------|-----------|
| Mismo namespace | `postgres:5432` |
| Cross-namespace | `postgres.guidewire-infra.svc.cluster.local:5432` |

## Bases de Datos

| Base de Datos | Usuario | Password | Servicio |
|---------------|---------|----------|----------|
| `apicurio` | `apicurio` | `apicurio123` | Apicurio Service Registry |
| `billing` | `billing_user` | `billing123` | Billing Service (Spring Boot) |
| `incidents` | `incidents_user` | `incidents123` | Incidents Service (Quarkus) |
| `customers` | `customers_user` | `customers123` | Customers Service (Node.js) |
| `drools_audit` | `drools_user` | `drools123` | Drools Engine — audit trail (Spring Boot) |

## Inicialización

El script `init-db.sql` se ejecuta automáticamente en el primer arranque del contenedor. Crea las bases de datos, usuarios, permisos y habilita la extensión `uuid-ossp`.

Ruta en el contenedor: `/docker-entrypoint-initdb.d/init-db.sql`

## Conexión desde el Host

```bash
# Via oc port-forward
oc port-forward -n guidewire-infra svc/postgres 15432:5432 &
psql -h localhost -p 15432 -U postgres

# O directamente via oc exec
oc exec -it -n guidewire-infra deploy/postgres -- psql -U postgres
oc exec -it -n guidewire-infra deploy/postgres -- psql -U billing_user -d billing
```

## Conexión desde otros contenedores

```
jdbc:postgresql://postgres.guidewire-infra.svc.cluster.local:5432/billing     # Java (Spring/Quarkus)
postgresql://customers_user:customers123@postgres.guidewire-infra.svc.cluster.local:5432/customers  # Node.js (Prisma)
```

## Healthcheck

```bash
pg_isready -U postgres   # Retorna 0 si acepta conexiones
```

## Persistencia

Los datos se almacenan en el PVC `postgres-data`. Sobreviven `oc delete deploy/postgres` pero se eliminan con `oc delete pvc postgres-data -n guidewire-infra`.

## Diagrama de Bases de Datos

```mermaid
erDiagram
    POSTGRESQL_INSTANCE ||--o{ APICURIO_DB : contains
    POSTGRESQL_INSTANCE ||--o{ BILLING_DB : contains
    POSTGRESQL_INSTANCE ||--o{ INCIDENTS_DB : contains
    POSTGRESQL_INSTANCE ||--o{ CUSTOMERS_DB : contains

    APICURIO_DB {
        string user "apicurio"
        string password "apicurio123"
        string service "Apicurio Service Registry"
    }

    BILLING_DB {
        string user "billing_user"
        string password "billing123"
        string service "Billing Service (Spring Boot)"
    }

    INCIDENTS_DB {
        string user "incidents_user"
        string password "incidents123"
        string service "Incidents Service (Quarkus)"
    }

    CUSTOMERS_DB {
        string user "customers_user"
        string password "customers123"
        string service "Customers Service (Node.js)"
    }

    POSTGRESQL_INSTANCE ||--o{ DROOLS_AUDIT_DB : contains

    DROOLS_AUDIT_DB {
        string user "drools_user"
        string password "drools123"
        string service "Drools Engine (Spring Boot)"
    }
```

## Spec de referencia

- [spec.yml](../../../infra/postgres/spec.yml)
- Issue: [#33](../../../../issues/33)
