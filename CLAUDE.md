# CLAUDE.md — Guidewire Integration POC

## Estado actual del proyecto

**Revisa `memory/crc-migration.md`** en tu directorio de memoria para saber por dónde íbamos.
Ruta completa: `/home/monghit/.claude/projects/-home-monghit-git-pocs-guidewire/memory/crc-migration.md`

La sección "Pendiente para mañana" tiene las tareas concretas a continuar.

## Estructura del proyecto

- `lab/openshift/` — Manifiestos K8s/OpenShift (entorno principal)
- `lab/podman/` — Podman Compose (alternativa legacy, no tocar)
- `components/` — 5 microservicios (billing, camel, incidents, customers, drools)
- `openspecs/` — Specs, docs, contracts, resoluciones
- `contracts/` — OpenAPI, AsyncAPI, Avro schemas

## Convenciones

- Idioma docs/specs: español
- Idioma código/commits: inglés
- Commits: conventional commits (`feat:`, `fix:`, `docs:`)
- Entorno lab: Red Hat OpenShift Local (CRC), dos namespaces (guidewire-infra, guidewire-apps)
- **Apicurio Registry**: toda API diseñada, añadida o modificada debe registrarse en Apicurio Registry
  - Grupos por aplicación: `guidewire.<aplicacion>` (ej: `guidewire.billing-service`)
  - Labels obligatorios: `communication:sync|async`, `layer:<capa>`, `domain:<dominio>`
  - Cada artifact debe tener nombre y descripción
  - Issue CI/CD: #88
