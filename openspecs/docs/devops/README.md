# DevOps / CI-CD — Documentación

## Pipeline CI/CD (GitHub Actions)

### Trigger

- Push a `main` o `develop`
- Pull request hacia `main`

### Jobs

```
┌─────────────────────┐     ┌────────────────────┐
│ build-java-services │     │ build-node-service  │
│ (matrix: 4 services)│     │ (customers-service) │
└──────────┬──────────┘     └──────────┬──────────┘
           │                           │
           └────────────┬──────────────┘
                        │
              ┌─────────┴──────────┐
              │ contract-validation │
              │ security-scan       │
              └─────────┬──────────┘
                        │
              ┌─────────┴──────────┐
              │   docker-build     │
              │ (matrix: 5 images) │
              └─────────┬──────────┘
                        │
              ┌─────────┴──────────┐
              │    e2e-tests       │
              │ (docker compose +  │
              │  newman)           │
              └────────────────────┘
```

### Validación de Contratos

```bash
# OpenAPI
spectral lint contracts/openapi/*.yml

# AsyncAPI
asyncapi validate contracts/asyncapi/*.yml

# AVRO
avro-tools compile schema contracts/avro/ /tmp/avro-output
```

### Seguridad

- **OWASP Dependency Check**: vulnerabilidades en dependencias
- **Trivy**: vulnerabilidades en imágenes Docker
- **Gitleaks**: detección de secrets en el código

## Spec de referencia

- [spec.yml](../../devops/ci-cd/spec.yml)
- Issue: [#69](../../../issues/69)
