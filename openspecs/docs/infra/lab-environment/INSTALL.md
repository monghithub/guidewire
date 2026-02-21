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
Fase 2:  Operadores (Strimzi, Apicurio)
Fase 3:  PostgreSQL (Secret, ConfigMap, PVC, Deployment, Service)
Fase 4:  Kafka (Strimzi Kafka CR + Topics)
Fase 5:  Apicurio Registry (ApicurioRegistry CR)
Fase 6:  Kafdrop (Deployment, Service, Route)
Fase 7:  3Scale APIcast (ConfigMap, Deployment, Service, Route)
Fase 8:  Application BuildConfigs
Fase 9:  Build images (oc start-build --from-dir)
Fase 10: Deploy applications (Deployment, Service, Route)
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

## Paso 9: Configurar acceso desde la LAN

El daemon proxy de CRC tiene un bug en `network-mode: user` (modo por defecto) que impide el acceso HTTP/HTTPS a las rutas de OpenShift. Ademas, si K3s esta instalado en el host, sus reglas iptables interceptan los puertos 80/443. Los siguientes pasos configuran el workaround.

### 9.1 Parar K3s (si esta instalado)

```bash
sudo systemctl stop k3s
sudo systemctl disable k3s
```

### 9.2 Limpiar reglas iptables de K3s

```bash
sudo iptables -t nat -F KUBE-SERVICES
sudo iptables -t nat -F CNI-HOSTPORT-DNAT
sudo iptables -t nat -F KUBE-EXT-UQMCRMJZLI3FTLDP 2>/dev/null || true
sudo iptables -t nat -F KUBE-EXT-CVG3OEGEH7H5P3HQ 2>/dev/null || true
```

### 9.3 Permitir bind a puertos privilegiados

```bash
sudo sysctl net.ipv4.ip_unprivileged_port_start=80
```

Para hacerlo persistente entre reinicios:

```bash
echo 'net.ipv4.ip_unprivileged_port_start=80' | sudo tee /etc/sysctl.d/99-crc-unprivileged-ports.conf
```

### 9.4 Desactivar los forwarders rotos del daemon CRC

```bash
curl -s -X POST --unix-socket ~/.crc/crc-http.sock \
  -H 'Content-Type: application/json' \
  -d '{"local":":80","remote":"192.168.127.2:80","protocol":"tcp"}' \
  http://localhost/network/services/forwarder/unexpose

curl -s -X POST --unix-socket ~/.crc/crc-http.sock \
  -H 'Content-Type: application/json' \
  -d '{"local":":443","remote":"192.168.127.2:443","protocol":"tcp"}' \
  http://localhost/network/services/forwarder/unexpose
```

### 9.5 Lanzar los port-forwards del router

```bash
oc port-forward --address 0.0.0.0 -n openshift-ingress svc/router-internal-default 80:80 &
oc port-forward --address 0.0.0.0 -n openshift-ingress svc/router-internal-default 443:443 &
```

O usar el script que automatiza los pasos 9.2 a 9.5:

```bash
lab/openshift/scripts/start.sh
```

### 9.6 Configurar /etc/hosts en las maquinas cliente

En cada maquina desde la que se quiera acceder (sustituir `192.168.1.135` por la IP del servidor):

```
192.168.1.135  console-openshift-console.apps-crc.testing
192.168.1.135  oauth-openshift.apps-crc.testing
192.168.1.135  kafdrop-guidewire-infra.apps-crc.testing
192.168.1.135  apicurio-guidewire-infra.apps-crc.testing
192.168.1.135  apicast-guidewire-infra.apps-crc.testing
192.168.1.135  billing-service-guidewire-apps.apps-crc.testing
192.168.1.135  camel-gateway-guidewire-apps.apps-crc.testing
192.168.1.135  incidents-service-guidewire-apps.apps-crc.testing
192.168.1.135  customers-service-guidewire-apps.apps-crc.testing
192.168.1.135  drools-engine-guidewire-apps.apps-crc.testing
```

### 9.7 Verificar acceso

```bash
# Web UIs
curl -s -o /dev/null -w '%{http_code}' http://kafdrop-guidewire-infra.apps-crc.testing/     # 200
curl -s -o /dev/null -w '%{http_code}' http://apicurio-guidewire-infra.apps-crc.testing/    # 302
curl -sk -o /dev/null -w '%{http_code}' https://console-openshift-console.apps-crc.testing/ # 200

# Health checks
curl -s -o /dev/null -w '%{http_code}' http://billing-service-guidewire-apps.apps-crc.testing/actuator/health   # 200
curl -s -o /dev/null -w '%{http_code}' http://incidents-service-guidewire-apps.apps-crc.testing/q/health         # 200
curl -s -o /dev/null -w '%{http_code}' http://customers-service-guidewire-apps.apps-crc.testing/health           # 200
curl -s -o /dev/null -w '%{http_code}' http://camel-gateway-guidewire-apps.apps-crc.testing/actuator/health      # 200
curl -s -o /dev/null -w '%{http_code}' http://drools-engine-guidewire-apps.apps-crc.testing/actuator/health      # 200
```

---

## URLs y credenciales

### Consola OpenShift

| URL | Usuario | Password | Rol |
|-----|---------|----------|-----|
| https://console-openshift-console.apps-crc.testing | `kubeadmin` | `xtLsK-LLIzY-6UVEd-UESLR` | Administrador |
| https://console-openshift-console.apps-crc.testing | `developer` | `developer` | Developer |

> El navegador mostrara un aviso de certificado autofirmado. Aceptar la excepcion.

### Web UIs

| Componente | URL | Credenciales |
|-----------|-----|--------------|
| Kafdrop (Kafka UI) | http://kafdrop-guidewire-infra.apps-crc.testing | Sin auth |
| Apicurio (Schema Registry) | http://apicurio-guidewire-infra.apps-crc.testing | Sin auth |

### APIs de microservicios

| Servicio | URL base | Endpoints | Health |
|----------|----------|-----------|--------|
| Billing Service | http://billing-service-guidewire-apps.apps-crc.testing | `/api/v1/invoices` | `/actuator/health` |
| Camel Gateway | http://camel-gateway-guidewire-apps.apps-crc.testing | `/api/v1/gw-invoices` | `/actuator/health` |
| Incidents Service | http://incidents-service-guidewire-apps.apps-crc.testing | `/api/v1/incidents` | `/q/health` |
| Customers Service | http://customers-service-guidewire-apps.apps-crc.testing | `/api/v1/customers` | `/health` |
| Drools Engine | http://drools-engine-guidewire-apps.apps-crc.testing | `/api/v1/rules/evaluate` | `/actuator/health` |

### API Gateway (APIcast)

Base: `http://apicast-guidewire-infra.apps-crc.testing` (requiere `user_key`)

| Ruta | Backend |
|------|---------|
| `/api/v1/invoices` | Billing Service |
| `/api/v1/gw-invoices` | Camel Gateway |
| `/api/v1/incidents` | Incidents Service |
| `/api/v1/customers` | Customers Service |
| `/api/v1/rules/evaluate` | Drools Engine |
| `/api/v1/policies` | Camel Gateway (PolicyCenter) |
| `/api/v1/claims` | Camel Gateway (ClaimCenter) |

### Credenciales de infraestructura

| Servicio | Usuario | Password |
|----------|---------|----------|
| OpenShift (admin) | `kubeadmin` | `xtLsK-LLIzY-6UVEd-UESLR` |
| OpenShift (dev) | `developer` | `developer` |
| PostgreSQL | `guidewire` | `guidewire123` |

---

## Apagar y reiniciar

### Apagar

```bash
lab/openshift/scripts/stop.sh
```

### Reiniciar

```bash
lab/openshift/scripts/start.sh
```

> Ver tambien: [UNINSTALL.md](../../../../lab/openshift/UNINSTALL.md) para desinstalacion completa.

---

## Operaciones del dia a dia

### Desde el HOST

| Accion | Comando |
|--------|---------|
| Iniciar entorno | `lab/openshift/scripts/start.sh` |
| Detener entorno | `lab/openshift/scripts/stop.sh` |
| Estado | `crc status` |
| Consola web | https://console-openshift-console.apps-crc.testing |
| Configurar oc | `eval $(crc oc-env)` |
| Login developer | `oc login -u developer -p developer https://api.crc.testing:6443` |
| Login admin | `oc login -u kubeadmin -p xtLsK-LLIzY-6UVEd-UESLR https://api.crc.testing:6443` |

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
grep -c -E '(vmx|svm)' /proc/cpuinfo
free -h
df -h

crc delete
crc cleanup
crc setup
crc start --cpus 6 --memory 14336 --disk-size 60
```

### Pods en estado Pending

**Solucion**: Generalmente falta de recursos:

```bash
oc describe pod <nombre-pod> -n guidewire-apps
oc adm top nodes

crc stop
crc start --memory 16384
```

### Operador no se instala

**Solucion**: Los operadores tardan unos minutos:

```bash
oc login -u kubeadmin -p xtLsK-LLIzY-6UVEd-UESLR https://api.crc.testing:6443
oc get csv -n openshift-operators
oc get events -n openshift-operators --sort-by='.lastTimestamp'
```

### Builds fallan

```bash
oc logs build/billing-service-1 -n guidewire-apps
oc start-build billing-service -n guidewire-apps \
  --from-dir=../../components/billing-service --follow
```

### DNS no resuelve *.apps-crc.testing

```bash
cat /etc/NetworkManager/conf.d/crc-nm-dnsmasq.conf
sudo systemctl restart NetworkManager
```

### Puerto 80/443 devuelve "404 page not found"

Este es el bug del daemon proxy de CRC en `network-mode: user`. Ver [Paso 9](#paso-9-configurar-acceso-desde-la-lan) para la solucion completa.

Si K3s esta activo, sus reglas iptables DNAT interceptan los puertos 80/443:

```bash
sudo systemctl stop k3s
sudo iptables -t nat -F KUBE-SERVICES
sudo iptables -t nat -F CNI-HOSTPORT-DNAT
```

### Kafka no esta listo

```bash
oc get kafka -n guidewire-infra
oc get pods -n guidewire-infra -l strimzi.io/cluster=kafka-cluster
oc logs -f deploy/strimzi-cluster-operator -n openshift-operators
```

### Pull secret expirado (ImagePullBackOff en marketplace)

```bash
oc set data secret/pull-secret -n openshift-config \
  --from-file=.dockerconfigjson=pull-secret.json

ssh -i ~/.crc/machines/crc/id_ecdsa -o StrictHostKeyChecking=no \
  core@$(crc ip) "sudo systemctl restart crio"

oc delete pods -n openshift-marketplace --all
```

### Puerto 6443 ocupado (K3s)

```bash
sudo systemctl stop k3s
```

### `oc login` devuelve error

Alternativa: usar KUBECONFIG directamente:

```bash
export KUBECONFIG=~/.crc/machines/crc/kubeconfig
oc whoami   # system:admin
```

### virtiofsd no encontrado

```bash
# Ubuntu/Debian
sudo apt install virtiofsd

# Fedora/RHEL
sudo dnf install virtiofsd
```

---

## Desinstalacion completa

Ver [UNINSTALL.md](../../../../lab/openshift/UNINSTALL.md) para la guia detallada de desinstalacion, incluyendo la reversion de todos los cambios en el sistema host (sysctl, iptables, systemd, /etc/hosts, NetworkManager).

---

## Referencias

- [CRC (OpenShift Local) Documentation](https://crc.dev/crc/)
- [Red Hat OpenShift Documentation](https://docs.openshift.com)
- [Strimzi (Kafka on Kubernetes)](https://strimzi.io/documentation/)
- [Apicurio Registry Operator](https://www.apicur.io/registry/docs/)
- [UNINSTALL.md](../../../../lab/openshift/UNINSTALL.md) — Guia de desinstalacion
- [Lab Environment spec.yml](../../../infra/lab-environment/spec.yml)
- [Lab Environment docs](README.md)
