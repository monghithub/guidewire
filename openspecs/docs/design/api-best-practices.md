# Buenas Prácticas para OpenAPI y AsyncAPI

Guía de convenciones, patrones y herramientas para el diseño de APIs
en el ecosistema Guidewire.

---

## Tabla de contenido

1. [OpenAPI (APIs REST síncronas)](#1-openapi--apis-rest-síncronas)
2. [AsyncAPI (APIs event-driven asíncronas)](#2-asyncapi--apis-event-driven-asíncronas)
3. [Herramientas y plugins](#3-herramientas-y-plugins)
4. [Errores comunes a evitar](#4-errores-comunes-a-evitar)

---

## 1. OpenAPI — APIs REST síncronas

### 1.1 Enfoque design-first

El spec OpenAPI se diseña **antes** de implementar. Es el contrato entre
equipos. Nunca generar el spec a partir del código.

```
Diseño (spec) → Revisión → Registro en Apicurio → Implementación → Tests de contrato
```

### 1.2 Nomenclatura

**Paths:**
- Sustantivos en plural: `/customers`, `/policies`, `/incidents`
- Nunca verbos: ~~`/getCustomers`~~ → `GET /customers`
- Kebab-case para multi-palabra: `/billing-accounts`
- Jerarquía de recursos: `/customers/{customerId}/policies`

**operationId:**
- Formato `verbo + Recurso` en camelCase: `listCustomers`, `getPolicy`, `createIncident`
- Singular para un recurso, plural para colecciones

**Schemas:**
- PascalCase para nombres: `Customer`, `InvoiceStatus`, `CreatePolicyRequest`
- camelCase para propiedades: `firstName`, `policyNumber`, `createdAt`
- Consistencia total: un concepto = un nombre en toda la API

**Tags:**
- Agrupar operaciones por recurso o dominio funcional
- Facilita la navegación en Swagger UI y Redoc

### 1.3 Reutilización con `$ref`

Definir una vez en `components/`, referenciar en todas partes:

```yaml
components:
  schemas:
    Customer:
      type: object
      properties:
        customerId:
          type: string
          format: uuid
        firstName:
          type: string

  parameters:
    PageSize:
      name: pageSize
      in: query
      schema:
        type: integer
        default: 20

  responses:
    NotFound:
      description: Recurso no encontrado
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/Problem'

paths:
  /customers/{id}:
    get:
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
        '404':
          $ref: '#/components/responses/NotFound'
```

### 1.4 Respuestas de error (RFC 9457)

Usar el estándar **Problem Details** en todas las respuestas de error:

```yaml
components:
  schemas:
    Problem:
      type: object
      properties:
        type:
          type: string
          format: uri
          description: URI que identifica el tipo de problema
        title:
          type: string
          description: Resumen legible del problema
        status:
          type: integer
          description: Código HTTP
        detail:
          type: string
          description: Explicación específica de esta ocurrencia
        instance:
          type: string
          format: uri
```

- Content-Type: `application/problem+json`
- El campo `status` debe coincidir con el HTTP status code real
- Reutilizar el schema `Problem` via `$ref` en todas las respuestas de error

### 1.5 Códigos HTTP correctos

| Operación | Éxito | Errores comunes |
|-----------|-------|-----------------|
| `GET` recurso | `200` | `404` no encontrado |
| `GET` colección | `200` | `400` parámetros inválidos |
| `POST` crear | `201` | `400` validación, `409` conflicto |
| `PATCH` actualizar | `200` | `404`, `409` transición inválida |
| `DELETE` eliminar | `204` | `404` |
| Validación | — | `422` entidad no procesable |

### 1.6 Versionado

| Estrategia | Ejemplo | Cuándo usar |
|------------|---------|-------------|
| **URL/Path** | `/v1/customers` | Recomendado. Simple, explícito, bien soportado |
| **Header** | `X-API-Version: 2` | URL limpia, menos visible |
| **Content negotiation** | `Accept: application/vnd.acme.v2+json` | Elegante, complejo |

- Solo versionar ante **breaking changes** reales
- Documentar la versión en `info.version`
- En este proyecto usamos versionado por path (`/api/v1/...`)

### 1.7 Seguridad

```yaml
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - BearerAuth: []
```

- Preferir OAuth2/JWT para entornos enterprise
- API keys en header, nunca en query params (quedan en logs)
- Siempre HTTPS
- Aplicar seguridad a nivel global, sobreescribir por operación si es necesario

### 1.8 Documentación dentro del spec

- Cada operación: `summary` (breve) + `description` (detallada)
- Cada parámetro y propiedad: `description`
- Usar `example` o `examples` en schemas
- Las descripciones soportan **Markdown** (enlaces, listas, tablas)

---

## 2. AsyncAPI — APIs event-driven asíncronas

### 2.1 Versión del spec

Usar **AsyncAPI 2.6.0** (Apicurio 2.5.x no soporta la visualización de 3.0).

### 2.2 Nomenclatura de canales

Formato jerárquico con puntos: `dominio.tipo-evento`

```
billing.invoice-created
billing.invoice-status-changed
incidents.incident-created
customers.customer-registered
dlq.errors
```

- Prefijo por dominio funcional
- Kebab-case para el tipo de evento
- Consistencia en profundidad y estructura

### 2.3 Diseño de mensajes

```yaml
components:
  messages:
    InvoiceCreated:
      name: InvoiceCreated
      title: Invoice Created Event
      summary: Evento emitido cuando se crea una factura
      contentType: application/avro
      schemaFormat: application/vnd.apache.avro;version=1.9.0
      payload:
        # Schema Avro inlineado (no usar $ref a archivos externos)
        type: record
        name: InvoiceCreated
        namespace: com.guidewire.events.billing
        fields:
          - name: eventId
            type: string
          - name: eventTimestamp
            type:
              type: long
              logicalType: timestamp-millis
```

**Principios:**
- Mensajes auto-contenidos (toda la información necesaria en el payload)
- Especificar `contentType` explícitamente
- En Apicurio: **inlinear** los schemas Avro (los `$ref` externos no resuelven)
- Cada mensaje: `name`, `title`, `summary` y `description`

### 2.4 Documentar productores y consumidores

En AsyncAPI 2.6.0, documentar con `publish` (produce) y `subscribe` (consume):

```yaml
channels:
  billing.invoice-created:
    description: Emitido cuando se crea una factura en BillingCenter
    publish:
      operationId: publishInvoiceCreated
      summary: Publicar evento de factura creada
      description: Producido por camel-gateway.
      message:
        $ref: '#/components/messages/InvoiceCreated'
    subscribe:
      operationId: consumeInvoiceCreated
      summary: Consumir evento de factura creada
      description: Consumido por billing-service.
      message:
        $ref: '#/components/messages/InvoiceCreated'
```

### 2.5 Correlation IDs y trazabilidad

```yaml
components:
  messageTraits:
    commonHeaders:
      headers:
        type: object
        properties:
          correlationId:
            type: string
            format: uuid
            description: ID para correlacionar mensajes relacionados
          causationId:
            type: string
            format: uuid
            description: ID del evento que causó este mensaje
          timestamp:
            type: string
            format: date-time
```

- Propagar `correlationId` en toda la cadena de eventos
- Incluir `causationId` para rastrear causalidad
- Usar UUID v4 para IDs
- Preferir headers de mensaje sobre payload para IDs de correlación

### 2.6 Evolución de schemas (compatibilidad)

| Nivel | Descripción |
|-------|-------------|
| **BACKWARD** | Nuevos consumidores leen datos de productores antiguos |
| **FORWARD** | Consumidores antiguos leen datos de productores nuevos |
| **FULL** | Ambas direcciones (usado en este proyecto) |

**Reglas para evolución segura con Avro:**
- Solo agregar campos opcionales (con `default`)
- Nunca eliminar campos requeridos
- Nunca cambiar tipos de campos existentes
- Usar Apicurio con regla FULL para validar automáticamente

### 2.7 Dead Letter Queue (DLQ)

Documentar el DLQ como un canal más en la spec:

```yaml
channels:
  dlq.errors:
    description: Dead Letter Queue para mensajes que fallaron procesamiento.
    publish:
      message:
        $ref: '#/components/messages/ErrorEnvelope'
```

El `ErrorEnvelope` debe incluir:
- Error original y stack trace
- Mensaje original (base64)
- Topic de origen
- Número de reintentos
- Timestamp del fallo
- Servicio productor

---

## 3. Herramientas y plugins

### 3.1 Plugins de VS Code

#### OpenAPI

| Plugin | Descripción |
|--------|-------------|
| **42Crunch OpenAPI Editor** | Edición, preview, validación y análisis de seguridad OWASP. El más completo. |
| **Redocly OpenAPI** | Validación, linting con reglas Spectral, autocompletado inteligente. |
| **OpenAPI Viewer** | Visualización tipo Swagger UI dentro de VS Code. |
| **Spectral** (Stoplight) | Linting en tiempo real de OpenAPI y AsyncAPI con reglas personalizables. |

#### AsyncAPI

| Plugin | Descripción |
|--------|-------------|
| **AsyncAPI Preview** | Preview y validación de documentos AsyncAPI directamente en VS Code. |

#### YAML

| Plugin | Descripción |
|--------|-------------|
| **YAML** (Red Hat) | Validación de YAML contra JSON Schema. Soporte de esquemas personalizados. |

### 3.2 Herramientas de visualización y documentación

#### OpenAPI (REST)

| Herramienta | Tipo | Descripción |
|-------------|------|-------------|
| **Swagger UI** | Open source | Panel interactivo con "try it out". El estándar de facto. |
| **Redoc** | Open source | Documentación estilo Stripe (tres paneles). Solo lectura. |
| **Scalar** | Open source | Alternativa moderna a Swagger UI con interfaz limpia. |
| **Stoplight Elements** | Open source | Componente React similar a Swagger UI con mejor UX. |

#### AsyncAPI (Eventos)

| Herramienta | Tipo | Descripción |
|-------------|------|-------------|
| **AsyncAPI Studio** | Web | Editor, validador, conversor de versiones, preview. Todo en uno. URL: https://studio.asyncapi.com |
| **AsyncAPI React** | Componente React | Para integrar documentación AsyncAPI en aplicaciones web. |
| **AsyncAPI Generator** | CLI | Genera documentación HTML, Markdown y SDKs. |

### 3.3 Linting: Spectral

Linter de referencia para OpenAPI y AsyncAPI.

```bash
# Instalación
npm install -g @stoplight/spectral-cli

# Lint de un spec
spectral lint contracts/openapi/billing-service-api.yml
spectral lint contracts/asyncapi/guidewire-events.yml
```

Configuración personalizada en `.spectral.yaml`:

```yaml
extends: ["spectral:oas", "spectral:asyncapi"]
rules:
  operation-description:
    severity: warn
  oas3-api-servers:
    severity: error
```

Se integra en CI/CD (GitHub Actions, GitLab CI) y tiene extensión de VS Code.

### 3.4 Mocking y contract testing: Microcks

Proyecto sandbox de la CNCF para mocking y testing de APIs.

- Soporta OpenAPI, AsyncAPI, gRPC, GraphQL, SOAP
- Importar un spec → mocks funcionales en segundos
- Contract testing: verificar que la implementación cumple su contrato
- Despliegue: Docker, Kubernetes, OpenShift (existe operator)

```
Importar spec → Levantar mocks → Desarrollo paralelo → Contract tests → Deploy
```

### 3.5 Resumen de herramientas recomendadas

| Necesidad | Herramienta |
|-----------|-------------|
| Edición OpenAPI en VS Code | 42Crunch + Redocly |
| Edición AsyncAPI en VS Code | AsyncAPI Preview |
| Linting en CI/CD | Spectral CLI |
| Documentación REST interactiva | Swagger UI o Scalar |
| Documentación REST estática | Redoc |
| Documentación event-driven | AsyncAPI Studio |
| Mocking y contract testing | Microcks |
| Schema Registry | Apicurio (ya desplegado) |

---

## 4. Errores comunes a evitar

### OpenAPI

1. **Generar el spec después de implementar** — usar enfoque design-first
2. **No usar `components/`** — schemas duplicados, difíciles de mantener
3. **APIs "chatty"** — demasiados endpoints con recursos pequeños
4. **Descripciones ausentes** — toda operación, parámetro y schema necesita documentación
5. **Inconsistencia en nomenclatura** — mezclar camelCase con snake_case
6. **No validar el spec** — verificar que sea válido y que los `$ref` resuelvan
7. **Códigos HTTP incorrectos** — 201 para creación, 204 para eliminación, 422 para validación
8. **No registrar en Apicurio** — el spec debe subirse al registry al diseñarse o modificarse

### AsyncAPI

1. **No documentar el flujo de eventos** — sin spec, el conocimiento queda implícito en código
2. **Mensajes sin metadata** — correlationId, timestamp, eventType deben ir siempre
3. **Ignorar evolución de schemas** — cambios incompatibles rompen consumidores
4. **No definir DLQ** — los mensajes que fallan se pierden silenciosamente
5. **Solo documentar el productor** — los consumidores necesitan su propia documentación
6. **Usar `$ref` externos en Apicurio** — inlinear schemas Avro en el spec
7. **Usar AsyncAPI 3.0 con Apicurio 2.5.x** — no soporta visualización, usar 2.6.0
8. **No registrar en Apicurio** — misma regla que OpenAPI

---

## Referencias

- [OpenAPI Best Practices](https://learn.openapis.org/best-practices.html)
- [RFC 9457 — Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc9457)
- [AsyncAPI Documentation](https://www.asyncapi.com/docs)
- [Spectral — Stoplight](https://stoplight.io/open-source/spectral)
- [Microcks — API Mocking and Testing](https://microcks.io/)
- [Apicurio Registry REST API v2](https://www.apicur.io/registry/docs/apicurio-registry/2.x/assets-attachments/registry-rest-api.htm)
- [Documentación Apicurio del proyecto](../infra/apicurio/README.md)
