# Lab Environment — Documentación

> [Volver a OpenSpecs](../../../README.md) · [Volver al README principal](../../../../README.md)

## Descripción

Entorno de laboratorio sobre CRC (OpenShift Local). Dos namespaces organizan el stack: `guidewire-infra` para infraestructura (PostgreSQL, Kafka, Apicurio, 3Scale, Kafdrop) y `guidewire-apps` para los microservicios de negocio. Todo se despliega con manifiestos Kubernetes nativos via `oc apply`.

> **Nota**: ActiveMQ fue eliminado del stack. Kafka es el unico backbone de eventos. Ver [ActiveMQ — ELIMINADO](../../infra/activemq/README.md).

## Arquitectura

```mermaid
graph TD
    HOST["HOST (tu maquina)"]
    HOST --> CRC["CRC (OpenShift Local)<br/>20GB RAM / 8 CPUs / 80GB"]
    CRC --> NS_INFRA["Namespace: guidewire-infra"]
    CRC --> NS_APPS["Namespace: guidewire-apps"]

    NS_INFRA --> PG["PostgreSQL"]
    NS_INFRA --> KAFKA["Kafka (Strimzi v0.50.0, KRaft)"]
    NS_INFRA --> KAFDROP["Kafdrop"]
    NS_INFRA --> APICURIO["Apicurio Registry"]
    NS_INFRA --> THREESCALE["3Scale (APIcast)"]

    NS_APPS --> CAMEL["Camel Gateway"]
    NS_APPS --> DROOLS["Drools Engine"]
    NS_APPS --> BILLING["Billing Service"]
    NS_APPS --> INCIDENTS["Incidents Service"]
    NS_APPS --> CUSTOMERS["Customers Service"]
    NS_APPS --> SIMULATOR["Guidewire Simulator (Angular)"]

    style HOST fill:#f9f,stroke:#333
    style CRC fill:#bbf,stroke:#333
    style NS_INFRA fill:#fdb,stroke:#333
    style NS_APPS fill:#bdf,stroke:#333
```

## Requisitos del Host

Solo necesitas instalar:

```bash
# 1. Descargar CRC desde la consola de Red Hat
#    https://console.redhat.com/openshift/create/local

# 2. Instalar y configurar
crc setup
crc start --cpus 8 --memory 20480 --disk-size 80
```

**Necesitas**: Cuenta gratuita en Red Hat (para descargar CRC y el pull secret).

**NO necesitas**: Docker, Podman, Java, Node.js, Maven, ni ninguna herramienta de desarrollo.

## Ciclo de Vida

### Desde el HOST

| Accion | Comando | Descripcion |
|--------|---------|-------------|
| Encender | `crc start` | Inicia el cluster OpenShift |
| Apagar | `crc stop` | Detiene el cluster (datos persisten) |
| Estado | `crc status` | Muestra estado del cluster |
| Consola web | `crc console` | Abre la consola OpenShift en el navegador |
| Destruir | `crc delete` | Elimina el cluster completo |
| Config oc | `eval $(crc oc-env)` | Configura el CLI `oc` en tu shell |
| Login | `oc login -u developer -p developer https://api.crc.testing:6443` | Autenticarse (o usar `KUBECONFIG=~/.crc/machines/crc/kubeconfig`) |

### Operaciones con oc

| Accion | Comando |
|--------|---------|
| Ver pods | `oc get pods -n guidewire-infra` |
| Ver logs | `oc logs -f deploy/billing-service -n guidewire-apps` |
| Shell en pod | `oc exec -it deploy/postgres -n guidewire-infra -- bash` |
| Reconstruir imagen | `oc start-build billing-service -n guidewire-apps --from-dir=../../components/billing-service --follow` |
| Escalar | `oc scale deploy/billing-service --replicas=2 -n guidewire-apps` |
| Borrar namespace | `oc delete project guidewire-infra guidewire-apps` |
| Desplegar todo | `cd lab/openshift && ./deploy-all.sh` |
| Solo infra | `./deploy-all.sh --infra` |
| Solo apps | `./deploy-all.sh --apps` |

## Routes (servicios accesibles)

Los servicios se exponen como Routes de OpenShift con TLS automatico:

| Servicio | URL |
|----------|-----|
| OpenShift Console | https://console-openshift-console.apps-crc.testing |
| 3Scale Gateway | https://apicast-guidewire-infra.apps-crc.testing |
| Apicurio UI | https://apicurio-guidewire-infra.apps-crc.testing |
| Kafdrop | https://kafdrop-guidewire-infra.apps-crc.testing |
| Billing Service | https://billing-service-guidewire-apps.apps-crc.testing |
| Incidents Service | https://incidents-service-guidewire-apps.apps-crc.testing |
| Customers Service | https://customers-service-guidewire-apps.apps-crc.testing |
| Camel Gateway | https://camel-gateway-guidewire-apps.apps-crc.testing |
| Drools Engine | https://drools-engine-guidewire-apps.apps-crc.testing |
| Guidewire Simulator | http://guidewire-simulator-guidewire-apps.apps-crc.testing |

## Recursos

| Recurso | CRC (cluster) | Stack estimado | Libre |
|---------|---------------|----------------|-------|
| RAM | 20 GB | ~10 GB | ~10 GB |
| CPU | 8 cores | ~5 cores | ~3 cores |
| Disco | 80 GB | ~20 GB | ~60 GB |

> **Nota**: CRC reserva recursos para el propio OpenShift (~5-6GB RAM para etcd, API server, ingress, etc.). El stack Guidewire completo (5 microservicios + 1 frontend + infra) usa ~10GB adicionales. Con 14GB se puede quedar corto; 20GB es el minimo recomendado.

## Estructura de Archivos

```
lab/openshift/
├── deploy-all.sh                  # Script de despliegue completo
├── namespaces.yml                 # guidewire-infra + guidewire-apps
├── operators/
│   ├── strimzi-subscription.yml   # Operador Strimzi (Kafka)
│   └── apicurio-subscription.yml  # Operador Apicurio Registry
├── infra/
│   ├── postgres/
│   │   ├── secret.yml
│   │   ├── configmap-init-db.yml
│   │   ├── pvc.yml
│   │   ├── deployment.yml
│   │   └── service.yml
│   ├── kafka/
│   │   ├── kafka-cluster.yml      # Strimzi Kafka CR (KRaft + KafkaNodePool)
│   │   └── kafka-topics.yml       # KafkaTopic CRs
│   ├── apicurio/
│   │   └── apicurio-registry.yml  # ApicurioRegistry CR
│   ├── kafdrop/
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   └── route.yml
│   └── threescale/
│       ├── configmap-apicast.yml
│       ├── deployment.yml
│       ├── service.yml
│       └── route.yml
└── apps/
    ├── billing-service/
    │   ├── buildconfig.yml
    │   ├── deployment.yml
    │   ├── service.yml
    │   └── route.yml
    ├── camel-gateway/
    │   └── ...
    ├── incidents-service/
    │   └── ...
    ├── customers-service/
    │   └── ...
    ├── drools-engine/
    │   └── ...
    └── guidewire-simulator/
        ├── buildconfig.yaml
        └── deployment.yaml
```

## Spec de referencia

- [spec.yml](../../../infra/lab-environment/spec.yml)
- Issue: [#34](../../../../issues/34)

---

## Documentación relacionada

- [UNINSTALL.md](../../../../../lab/openshift/UNINSTALL.md) — Guía de desinstalación del entorno OpenShift
