# Instalacion del Laboratorio — CRC (OpenShift Local)

Guia para montar el laboratorio de integracion Guidewire desde cero. Al terminar tendras un cluster OpenShift local con todo el stack desplegado en dos namespaces.

---

## Requisitos del Host

| Requisito | Minimo | Recomendado |
|-----------|--------|-------------|
| RAM | 16 GB | 32 GB+ |
| CPU | 4 cores | 8 cores+ |
| Disco | 60 GB libres | 100 GB+ |
| SO | Ubuntu 22.04+ / Fedora 38+ / RHEL 9+ / macOS 13+ | Fedora 40+ |
| CPU Virtualization | VT-x / AMD-V habilitado en BIOS | — |
| Cuenta Red Hat | Gratuita (para pull secret) | — |

### Verificar soporte de virtualizacion

```bash
# Linux: debe retornar un numero > 0
grep -c -E '(vmx|svm)' /proc/cpuinfo

# macOS: debe mostrar "VMX"
sysctl -a | grep machdep.cpu.features | grep VMX
```

Si retorna 0, habilita **VT-x** (Intel) o **AMD-V** (AMD) en la BIOS/UEFI de tu maquina.

---

## Paso 1: Descargar CRC

1. Ve a la consola de Red Hat: https://console.redhat.com/openshift/create/local
2. Inicia sesion con tu cuenta Red Hat (gratuita)
3. Descarga el binario de CRC para tu sistema operativo
4. Descarga tambien el **pull secret** (lo necesitaras en el paso 4)

### Linux

```bash
# Descargar (ajustar version segun disponible)
wget https://developers.redhat.com/content-gateway/rest/mirror/pub/openshift-v4/clients/crc/latest/crc-linux-amd64.tar.xz
```

### macOS

```bash
wget https://developers.redhat.com/content-gateway/rest/mirror/pub/openshift-v4/clients/crc/latest/crc-macos-amd64.tar.xz
```

---

## Paso 2: Instalar CRC

### Linux

```bash
# Descomprimir
tar xvf crc-linux-amd64.tar.xz

# Mover el binario al PATH
sudo mv crc-linux-*-amd64/crc /usr/local/bin/

# Verificar
crc version
```

### macOS

```bash
# Descomprimir
tar xvf crc-macos-amd64.tar.xz

# Mover el binario al PATH
sudo mv crc-macos-*-amd64/crc /usr/local/bin/

# Verificar
crc version
```

---

## Paso 3: Configurar CRC (setup)

```bash
# Configura libvirt/HyperKit, resuelve DNS, etc.
# Solo se ejecuta una vez
crc setup
```

Este comando:
- Instala y configura el hypervisor (libvirt en Linux, HyperKit en macOS)
- Configura la resolucion DNS para `*.crc.testing` y `*.apps-crc.testing`
- Descarga la imagen de la VM de OpenShift (~4GB)
- Configura los certificados necesarios

> **Nota**: En Linux puede pedir sudo para configurar NetworkManager y libvirt. En macOS configura la resolucion DNS automaticamente.

---

## Paso 4: Iniciar CRC

```bash
# Iniciar con recursos suficientes para el stack Guidewire
crc start \
  --cpus 6 \
  --memory 14336 \
  --disk-size 60
```

La primera vez:
1. Te pedira el **pull secret** (descargado en el paso 1)
2. Crea la VM con OpenShift 4.x (~5 minutos)
3. Arranca todos los operadores del cluster (~3-5 minutos)
4. Muestra las credenciales de acceso al terminar

Resultado esperado:

```
Started the OpenShift cluster.

The server is accessible via web console at:
  https://console-openshift-console.apps-crc.testing

Log in as administrator:
  Username: kubeadmin
  Password: xxxxx-xxxxx-xxxxx-xxxxx

Log in as user:
  Username: developer
  Password: developer
```

> **Importante**: Guarda la contrasena de `kubeadmin`. La necesitaras para instalar operadores.

---

## Paso 5: Configurar el CLI `oc`

```bash
# Configurar el PATH para el binario oc que viene con CRC
eval $(crc oc-env)

# Agregar a tu .bashrc / .zshrc para que persista
echo 'eval $(crc oc-env)' >> ~/.bashrc

# Login como developer (para desplegar aplicaciones)
oc login -u developer -p developer https://api.crc.testing:6443

# Verificar
oc whoami
# developer

oc project
# Using project "default" on server "https://api.crc.testing:6443"
```

> **Nota**: Para instalar operadores necesitas login como `kubeadmin`:
> ```bash
> oc login -u kubeadmin -p <password> https://api.crc.testing:6443
> ```

---

## Paso 6: Verificar la instalacion

```bash
echo "=== CRC ==="
crc status

echo "=== Cluster ==="
oc cluster-info

echo "=== Nodes ==="
oc get nodes

echo "=== oc version ==="
oc version
```

Resultado esperado:

```
=== CRC ===
CRC VM:          Running
OpenShift:       Running (v4.x.x)
RAM Usage:       8.5GB of 14GB
Disk Usage:      15GB of 60GB (Inside the CRC VM)
Cache Usage:     18GB
Cache Directory: ~/.crc/cache

=== Cluster ===
Kubernetes control plane is running at https://api.crc.testing:6443

=== Nodes ===
NAME                 STATUS   ROLES                         AGE   VERSION
crc-xxxxx-master-0   Ready    control-plane,master,worker   ...   v1.xx.x

=== oc version ===
Client Version: 4.x.x
Server Version: 4.x.x
Kubernetes Version: v1.xx.x
```

---

## Paso 7: Desplegar el stack

```bash
# Ir al directorio de manifiestos OpenShift
cd lab/openshift

# Desplegar todo (infraestructura + aplicaciones)
./deploy-all.sh

# O desplegar por fases
./deploy-all.sh --infra   # Solo infraestructura
./deploy-all.sh --apps    # Solo aplicaciones
```

El script despliega en este orden:

```
Fase 1:  Namespaces (guidewire-infra, guidewire-apps)
Fase 2:  Operadores (Strimzi, AMQ Broker, Apicurio)
Fase 3:  PostgreSQL (Secret, ConfigMap, PVC, Deployment, Service)
Fase 4:  Kafka (Strimzi Kafka CR + Topics)
Fase 5:  ActiveMQ (AMQ Broker CR + Addresses)
Fase 6:  Apicurio Registry (ApicurioRegistry CR)
Fase 7:  Kafdrop (Deployment, Service, Route)
Fase 8:  3Scale APIcast (ConfigMap, Deployment, Service, Route)
Fase 9:  Application BuildConfigs
Fase 10: Build images (oc start-build --from-dir)
Fase 11: Deploy applications (Deployment, Service, Route)
```

> **Nota**: La primera vez tarda ~15-20 minutos (descarga de operadores e imagenes). Las siguientes veces ~5 minutos.

---

## Paso 8: Verificar servicios

### Pods

```bash
# Infraestructura
oc get pods -n guidewire-infra
# NAME                                             READY   STATUS    AGE
# postgres-xxxx                                    1/1     Running   5m
# kafka-cluster-kafka-pool-0                       1/1     Running   4m
# kafka-cluster-entity-operator-xxxx               1/1     Running   3m
# activemq-broker-ss-0                             1/1     Running   3m
# apicurio-registry-xxxx                           1/1     Running   3m
# kafdrop-xxxx                                     1/1     Running   2m
# apicast-xxxx                                     1/1     Running   2m

# Aplicaciones
oc get pods -n guidewire-apps
# NAME                        READY   STATUS    RESTARTS   AGE
# billing-service-xxxx        1/1     Running   0          2m
# camel-gateway-xxxx          1/1     Running   0          2m
# incidents-service-xxxx      1/1     Running   0          2m
# customers-service-xxxx      1/1     Running   0          2m
# drools-engine-xxxx          1/1     Running   0          2m
```

> **Nota**: Ya no hay pod de ZooKeeper — Strimzi v0.50.0 usa Kafka en modo KRaft con `KafkaNodePool`. El pod se llama `kafka-cluster-kafka-pool-0`.

### Routes

```bash
oc get routes -n guidewire-infra
oc get routes -n guidewire-apps
```

### Verificar conectividad

```bash
# Kafdrop
curl -sk https://kafdrop-guidewire-infra.apps-crc.testing

# Apicurio
curl -sk https://apicurio-guidewire-infra.apps-crc.testing/health/ready

# Health checks (cada framework tiene su ruta)
curl -sk https://billing-service-guidewire-apps.apps-crc.testing/actuator/health    # Spring Boot
curl -sk https://incidents-service-guidewire-apps.apps-crc.testing/q/health          # Quarkus
curl -sk https://customers-service-guidewire-apps.apps-crc.testing/health            # Node.js/Express
curl -sk https://camel-gateway-guidewire-apps.apps-crc.testing/actuator/health       # Spring Boot
curl -sk https://drools-engine-guidewire-apps.apps-crc.testing/actuator/health       # Spring Boot
```

---

## Operaciones del dia a dia

### Desde el HOST

| Accion | Comando |
|--------|---------|
| Iniciar cluster | `crc start` |
| Detener cluster | `crc stop` |
| Estado | `crc status` |
| Consola web | `crc console` |
| Configurar oc | `eval $(crc oc-env)` |
| Login developer | `oc login -u developer -p developer https://api.crc.testing:6443` |
| Login admin | `oc login -u kubeadmin -p <password> https://api.crc.testing:6443` |

### Operaciones con el stack

| Accion | Comando |
|--------|---------|
| Ver pods infra | `oc get pods -n guidewire-infra` |
| Ver pods apps | `oc get pods -n guidewire-apps` |
| Logs de un servicio | `oc logs -f deploy/billing-service -n guidewire-apps` |
| Shell en un pod | `oc exec -it deploy/postgres -n guidewire-infra -- bash` |
| Reconstruir imagen | `oc start-build billing-service -n guidewire-apps --from-dir=../../components/billing-service --follow` |
| Escalar un servicio | `oc scale deploy/billing-service --replicas=2 -n guidewire-apps` |
| Restart un deploy | `oc rollout restart deploy/billing-service -n guidewire-apps` |
| Ver routes | `oc get routes --all-namespaces` |
| Borrar todo | `oc delete project guidewire-infra guidewire-apps` |
| Redesplegar todo | `./deploy-all.sh` |

---

## Troubleshooting

### CRC no arranca

```
Error: Failed to start the CRC VM
```

**Solucion**: Verificar que la virtualizacion esta habilitada y que hay recursos suficientes:

```bash
# Verificar virtualizacion
grep -c -E '(vmx|svm)' /proc/cpuinfo

# Verificar recursos disponibles
free -h
df -h

# Reintentar con cleanup
crc delete
crc cleanup
crc setup
crc start --cpus 6 --memory 14336 --disk-size 60
```

### Pods en estado Pending

```
NAME              READY   STATUS    RESTARTS   AGE
billing-service   0/1     Pending   0          5m
```

**Solucion**: Generalmente es falta de recursos. Verificar:

```bash
# Ver eventos del pod
oc describe pod <nombre-pod> -n guidewire-apps

# Ver recursos del nodo
oc adm top nodes

# Si es falta de memoria, parar CRC y reiniciar con mas recursos
crc stop
crc start --memory 16384
```

### Operador no se instala

```
Operator strimzi may not be fully ready yet
```

**Solucion**: Los operadores pueden tardar unos minutos. Verificar:

```bash
# Login como admin
oc login -u kubeadmin -p <password> https://api.crc.testing:6443

# Ver estado de los operadores
oc get csv -n openshift-operators

# Ver eventos
oc get events -n openshift-operators --sort-by='.lastTimestamp'

# Ver subscriptions
oc get subscriptions -n openshift-operators
```

### Builds fallan

```
Build billing-service-1 failed
```

**Solucion**: Verificar los logs del build:

```bash
# Ver logs del build
oc logs build/billing-service-1 -n guidewire-apps

# Reintentar el build
oc start-build billing-service -n guidewire-apps \
  --from-dir=../../components/billing-service --follow

# Si es problema de memoria, los builds Java necesitan ~1GB
# Verificar recursos disponibles
oc adm top nodes
```

### DNS no resuelve *.apps-crc.testing

```
curl: (6) Could not resolve host: kafdrop-guidewire-infra.apps-crc.testing
```

**Solucion**: CRC configura el DNS automaticamente, pero a veces falla:

```bash
# Verificar que el DNS esta configurado
cat /etc/NetworkManager/conf.d/crc-nm-dnsmasq.conf

# Reiniciar NetworkManager
sudo systemctl restart NetworkManager

# Alternativa: agregar manualmente a /etc/hosts
echo "$(crc ip) kafdrop-guidewire-infra.apps-crc.testing" | sudo tee -a /etc/hosts
```

### Kafka no esta listo

```
Kafka cluster may still be starting
```

**Solucion**: Strimzi tarda en desplegar Kafka (~3-5 minutos). Verificar:

```bash
# Estado del Kafka CR
oc get kafka -n guidewire-infra

# Pods de Kafka (con KafkaNodePool, el pod se llama kafka-pool)
oc get pods -n guidewire-infra -l strimzi.io/cluster=kafka-cluster

# Logs del operador Strimzi
oc logs -f deploy/strimzi-cluster-operator -n openshift-operators
```

### Pull secret expirado (ImagePullBackOff en marketplace)

```
marketplace-operator   ImagePullBackOff
```

**Solucion**: El pull secret de CRC expira periodicamente. Hay que renovarlo:

```bash
# 1. Crear service account en https://access.redhat.com/terms-based-registry/
# 2. Obtener el token de autenticacion
# 3. Actualizar el pull secret en el cluster
oc set data secret/pull-secret -n openshift-config \
  --from-file=.dockerconfigjson=pull-secret.json

# 4. Reiniciar CRI-O para que tome las nuevas credenciales
ssh -i ~/.crc/machines/crc/id_ecdsa -o StrictHostKeyChecking=no \
  core@$(crc ip) "sudo systemctl restart crio"

# 5. Borrar los pods fallidos para que se recreen
oc delete pods -n openshift-marketplace --all
```

### Puerto 6443 ocupado (k3s u otro servicio)

```
ERRO CRC requires port 6443
```

**Solucion**: Si tienes k3s o minikube, detenerlos antes de iniciar CRC:

```bash
sudo systemctl stop k3s
# o
minikube stop
```

### `oc login` devuelve 404

```
error: the server is currently unable to handle the request
```

**Solucion**: El oauth-server de CRC puede tardar en estar listo. Alternativa: usar KUBECONFIG directamente:

```bash
export KUBECONFIG=~/.crc/machines/crc/kubeconfig
oc whoami   # system:admin
```

### Acceso desde otra maquina en la red (sin GUI)

Si la maquina con CRC no tiene interfaz grafica, puedes acceder desde otra maquina en la misma LAN:

```bash
# CRC ya expone los puertos 443 y 80 en 0.0.0.0
# Solo necesitas exponer el API server (6443):
socat TCP-LISTEN:6443,fork,reuseaddr TCP:127.0.0.1:6443 &

# Desde la otra maquina, agregar entradas en /etc/hosts apuntando a la IP LAN:
# <IP_LAN>  api.crc.testing console-openshift-console.apps-crc.testing
#           kafdrop-guidewire-infra.apps-crc.testing ...
```

### virtiofsd no encontrado

```
virtiofsd not found in PATH
```

**Solucion**: Instalar virtiofsd (necesario para libvirt):

```bash
# Ubuntu/Debian
sudo apt install virtiofsd

# Fedora/RHEL
sudo dnf install virtiofsd
```

---

## Desinstalacion completa

```bash
# 1. Eliminar los namespaces del proyecto
oc delete project guidewire-infra guidewire-apps

# 2. Eliminar el cluster CRC
crc stop
crc delete

# 3. Limpiar configuracion de CRC
crc cleanup

# 4. Eliminar el binario (opcional)
sudo rm /usr/local/bin/crc

# 5. Eliminar cache y configuracion (opcional)
rm -rf ~/.crc
```

---

## Referencias

- [CRC (OpenShift Local) Documentation](https://crc.dev/crc/)
- [Red Hat OpenShift Documentation](https://docs.openshift.com)
- [Strimzi (Kafka on Kubernetes)](https://strimzi.io/documentation/)
- [AMQ Broker Operator](https://access.redhat.com/documentation/en-us/red_hat_amq_broker)
- [Apicurio Registry Operator](https://www.apicur.io/registry/docs/)
- [Lab Environment spec.yml](../../../infra/lab-environment/spec.yml)
- [Lab Environment docs](README.md)
