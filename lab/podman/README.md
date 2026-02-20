# Podman Compose — Alternativa Legacy

> **Nota:** El entorno principal del laboratorio es **Red Hat OpenShift Local (CRC)**.
> Los manifiestos OpenShift están en `lab/openshift/`. Consulta la
> [guía de instalación](../../openspecs/docs/infra/lab-environment/INSTALL.md).

Este directorio contiene la configuración de **Podman Compose** como alternativa
para entornos donde CRC no esté disponible (recursos insuficientes, sin cuenta
Red Hat, o para pruebas rápidas sin Kubernetes).

## Uso

```bash
cd lab/podman
podman-compose up -d       # Levantar los 12 servicios
podman-compose ps          # Verificar estado
podman-compose down        # Detener (datos persisten)
podman-compose down -v     # Detener y eliminar volúmenes
```

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `podman-compose.yml` | Stack completo: 12 servicios con healthchecks |
| `.env` | Variables de entorno (versiones, credenciales, puertos) |
| `config/init-db.sql` | Inicialización PostgreSQL (4 bases de datos) |
| `config/create-topics.sh` | Creación de 7 topics Kafka |
| `config/apicast-config.json` | Configuración declarativa de 3Scale |

## Diferencias con CRC

| Aspecto | Podman Compose | CRC (OpenShift) |
|---------|---------------|-----------------|
| Orquestación | Compose file | Kubernetes manifests |
| Operadores | No disponible | Strimzi, AMQ Broker, Apicurio |
| Service discovery | Docker DNS | Kubernetes DNS (cross-namespace) |
| Builds | `podman build` | BuildConfig + ImageStream |
| Acceso | `localhost:<port>` | Routes (`*.apps-crc.testing`) |
| Persistencia | Volumes | PersistentVolumeClaims |
