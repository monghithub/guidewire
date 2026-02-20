# Integración y Testing E2E — Documentación

## 3Scale API Registration

Registro de las 6 APIs en el gateway 3Scale:

| API | OpenAPI Source | Plan | Rate Limit |
|-----|---------------|------|------------|
| PolicyCenter | `policycenter-api.yml` | guidewire-standard | 100 req/min |
| ClaimCenter | `claimcenter-api.yml` | guidewire-standard | 100 req/min |
| BillingCenter | `billingcenter-api.yml` | guidewire-standard | 100 req/min |
| Billing Service | `billing-service-api.yml` | microservices-standard | 200 req/min |
| Incidents Service | `incidents-service-api.yml` | microservices-standard | 200 req/min |
| Customers Service | `customers-service-api.yml` | microservices-standard | 200 req/min |

## Postman E2E Tests

### Flujos de prueba

1. **Registro de Cliente** — POST cliente → GET verificar
2. **Creación de Póliza** — POST póliza via Camel → verificar estado QUOTED
3. **Creación de Factura** — POST factura → verificar items y total
4. **Creación de Siniestro** — POST claim via Camel → verificar incidencia creada
5. **Verificación Kafka** — Consultar Kafdrop → mensajes en topics
6. **Verificación Apicurio** — Consultar registry → schemas registrados

### Ejecución con Newman

```bash
newman run contracts/postman/guidewire-e2e.postman_collection.json \
  -e contracts/postman/local.postman_environment.json \
  --reporters cli,junit \
  --reporter-junit-export reports/e2e-results.xml
```

## Specs de referencia

- [3Scale Registration spec.yml](../../integration/threescale-registration/spec.yml)
- [Postman E2E spec.yml](../../integration/postman-e2e/spec.yml)
- Issues: [#66](../../../issues/66), [#67](../../../issues/67)
