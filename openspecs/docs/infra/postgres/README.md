# PostgreSQL — Documentación

## Descripción

Instancia PostgreSQL 16 compartida que aloja 4 bases de datos lógicas, una por cada servicio que requiere persistencia. Se respeta el patrón **database-per-service**: cada servicio tiene su propia base de datos y usuario con permisos aislados.

## Configuración

| Parámetro | Valor |
|-----------|-------|
| Imagen | `postgres:16-alpine` |
| Puerto interno | 5432 |
| Puerto expuesto | **15432** (remapeado para evitar conflictos) |
| Superusuario | `postgres` / `postgres123` |
| Volumen | `pgdata` → `/var/lib/postgresql/data` |

## Bases de Datos

| Base de Datos | Usuario | Password | Servicio |
|---------------|---------|----------|----------|
| `apicurio` | `apicurio` | `apicurio123` | Apicurio Service Registry |
| `billing` | `billing_user` | `billing123` | Billing Service (Spring Boot) |
| `incidents` | `incidents_user` | `incidents123` | Incidents Service (Quarkus) |
| `customers` | `customers_user` | `customers123` | Customers Service (Node.js) |

## Inicialización

El script `init-db.sql` se ejecuta automáticamente en el primer arranque del contenedor. Crea las bases de datos, usuarios, permisos y habilita la extensión `uuid-ossp`.

Ruta en el contenedor: `/docker-entrypoint-initdb.d/init-db.sql`

## Conexión desde el Host

```bash
psql -h localhost -p 15432 -U postgres
psql -h localhost -p 15432 -U billing_user -d billing
```

## Conexión desde otros contenedores

```
jdbc:postgresql://postgres:5432/billing     # Java (Spring/Quarkus)
postgresql://customers_user:customers123@postgres:5432/customers  # Node.js (Prisma)
```

## Healthcheck

```bash
pg_isready -U postgres   # Retorna 0 si acepta conexiones
```

## Persistencia

Los datos se almacenan en el volumen nombrado `pgdata`. Sobreviven `podman-compose down` pero se eliminan con `podman-compose down -v`.

## Spec de referencia

- [spec.yml](../../../infra/postgres/spec.yml)
- Issue: [#33](../../../../issues/33)
