# Instalación del Laboratorio — Guía paso a paso

Guía para montar el laboratorio de integración Guidewire desde cero. Al terminar tendrás una VM aislada con todo el stack corriendo en contenedores Podman.

---

## Requisitos del Host

| Requisito | Mínimo | Recomendado |
|-----------|--------|-------------|
| RAM | 24 GB | 32 GB+ |
| CPU | 4 cores | 8 cores+ |
| Disco | 100 GB libres | 150 GB+ |
| SO | Ubuntu 22.04+ / Fedora 38+ / Debian 12+ | Ubuntu 24.04+ |
| CPU Virtualization | VT-x / AMD-V habilitado en BIOS | — |

### Verificar soporte de virtualización

```bash
# Debe retornar un número > 0
grep -c -E '(vmx|svm)' /proc/cpuinfo

# O con kvm-ok (si está disponible)
kvm-ok
```

Si retorna 0, habilita **VT-x** (Intel) o **AMD-V** (AMD) en la BIOS/UEFI de tu máquina.

---

## Paso 1: Instalar dependencias del sistema

### Ubuntu / Debian

```bash
sudo apt update
sudo apt install -y \
  qemu-system-x86 \
  libvirt-daemon-system \
  libvirt-dev \
  virt-manager \
  bridge-utils \
  ebtables \
  dnsmasq-base \
  libguestfs-tools \
  build-essential \
  ruby-dev \
  ruby-libvirt
```

### Fedora

```bash
sudo dnf install -y \
  @virtualization \
  libvirt-devel \
  virt-manager \
  bridge-utils \
  ruby-devel \
  gcc \
  make
```

### Verificar que libvirt funciona

```bash
sudo systemctl enable --now libvirtd
sudo systemctl status libvirtd

# Debe mostrar "active (running)"
```

---

## Paso 2: Instalar Vagrant

Vagrant **no está en los repos** de Ubuntu 24.04+ ni de versiones recientes. Se instala desde HashiCorp directamente.

### Opción A: Descarga directa (recomendado)

```bash
# Descargar el .deb de HashiCorp
wget https://releases.hashicorp.com/vagrant/2.4.3/vagrant_2.4.3-1_amd64.deb

# Instalar
sudo dpkg -i vagrant_2.4.3-1_amd64.deb

# Limpiar
rm vagrant_2.4.3-1_amd64.deb

# Verificar
vagrant --version
# Vagrant 2.4.3
```

### Opción B: Repo de HashiCorp

```bash
wget -O - https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update
sudo apt install -y vagrant
```

> **Nota**: La opción B puede fallar si tu versión de Ubuntu es muy reciente y HashiCorp aún no tiene el repo para tu codename. En ese caso usa la opción A.

### Opción C: Fedora

```bash
sudo dnf install -y dnf-plugins-core
sudo dnf config-manager --add-repo https://rpm.releases.hashicorp.com/fedora/hashicorp.repo
sudo dnf install -y vagrant
```

---

## Paso 3: Instalar plugin vagrant-libvirt

```bash
# Instalar el plugin
vagrant plugin install vagrant-libvirt

# Verificar
vagrant plugin list
# vagrant-libvirt (0.12.x, global)
```

### Troubleshooting del plugin

Si falla la instalación del plugin con errores de compilación:

```bash
# Ubuntu/Debian: instalar dependencias de build
sudo apt install -y \
  libxslt-dev \
  libxml2-dev \
  zlib1g-dev \
  ruby-dev \
  libvirt-dev

# Reintentar
vagrant plugin install vagrant-libvirt
```

---

## Paso 4: Permisos de usuario

Tu usuario necesita pertenecer a los grupos `libvirt` y `kvm` para gestionar VMs sin sudo.

```bash
# Agregar tu usuario a los grupos
sudo usermod -aG libvirt $(whoami)
sudo usermod -aG kvm $(whoami)

# Verificar (requiere logout/login o newgrp)
newgrp libvirt
groups
# ... libvirt kvm ...
```

> **Importante**: Si no haces logout/login o `newgrp`, Vagrant no tendrá permisos para crear VMs y dará error de conexión a libvirt.

---

## Paso 5: Verificar la instalación

```bash
# Todo debe responder con versión o estado OK
echo "=== Vagrant ==="
vagrant --version

echo "=== libvirt ==="
virsh --version

echo "=== KVM ==="
virsh capabilities | grep -o 'kvm' | head -1

echo "=== Plugin ==="
vagrant plugin list | grep libvirt

echo "=== Grupos ==="
groups | grep -o 'libvirt\|kvm'
```

Resultado esperado:

```
=== Vagrant ===
Vagrant 2.4.3
=== libvirt ===
10.x.x
=== KVM ===
kvm
=== Plugin ===
vagrant-libvirt (0.12.x, global)
=== Grupos ===
libvirt
kvm
```

---

## Paso 6: Levantar la VM

```bash
# Ir al directorio del laboratorio
cd lab/

# Crear y provisionar la VM (primera vez tarda ~10-15 min)
vagrant up

# Verificar estado
vagrant status
# default                   running (libvirt)
```

La primera vez:
1. Descarga la box `generic/ubuntu2404` (~800MB)
2. Crea la VM con 20GB RAM, 8 CPUs, 80GB disco
3. Ejecuta `provision/setup.sh` (instala Podman, crea dirs)
4. Sincroniza la carpeta del proyecto a `/home/vagrant/lab-guidewire`

---

## Paso 7: Instalar herramientas de desarrollo (opcional)

Si necesitas compilar código dentro de la VM:

```bash
vagrant ssh
sudo /home/vagrant/lab-guidewire/lab/provision/install-dev-tools.sh
```

Esto instala: JDK 21 (Temurin), Maven 3.9, Node.js 20, kcat, HTTPie.

---

## Paso 8: Levantar el stack de servicios

```bash
# Entrar a la VM
vagrant ssh

# Ir al directorio de Podman Compose
cd ~/lab-guidewire/lab/podman

# Levantar todos los servicios
podman-compose up -d

# Verificar que todos están corriendo
podman-compose ps

# Ver logs en tiempo real (opcional)
podman-compose logs -f
```

### Orden de arranque automático

El `podman-compose.yml` define dependencias con healthchecks:

```
Fase 1: PostgreSQL, Kafka, ActiveMQ          (sin dependencias)
Fase 2: Apicurio, Kafdrop, 3Scale            (dependen de Fase 1)
Fase 3: Camel Gateway, Drools Engine         (dependen de Fase 2)
Fase 4: Billing, Incidents, Customers        (dependen de Fase 1+2)
```

### Verificar servicios

```bash
# Desde dentro de la VM
curl -s http://localhost:8080/health/ready    # Apicurio
curl -s http://localhost:9000                  # Kafdrop
curl -s http://localhost:8161                  # ActiveMQ Console

# Desde tu HOST (puertos forwarded)
curl -s http://localhost:8081                  # Apicurio UI
curl -s http://localhost:9000                  # Kafdrop UI
```

---

## Paso 9: Crear topics de Kafka

```bash
# Dentro de la VM
podman exec kafka /home/vagrant/lab-guidewire/lab/podman/config/create-topics.sh

# Verificar
podman exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Debe mostrar los 7 topics:
```
billing.invoice-created
billing.invoice-status-changed
customers.customer-registered
customers.customer-status-changed
dlq.errors
incidents.incident-created
incidents.incident-status-changed
```

---

## Operaciones del día a día

### Desde el HOST

| Acción | Comando |
|--------|---------|
| Encender VM | `cd lab && vagrant up` |
| Apagar VM | `vagrant halt` |
| Suspender VM | `vagrant suspend` |
| Reanudar VM | `vagrant resume` |
| Entrar a VM | `vagrant ssh` |
| Destruir VM | `vagrant destroy -f` |
| Estado VM | `vagrant status` |

### Dentro de la VM

| Acción | Comando |
|--------|---------|
| Levantar stack | `cd ~/lab-guidewire/lab/podman && podman-compose up -d` |
| Detener stack | `podman-compose down` |
| Ver logs | `podman-compose logs -f [servicio]` |
| Reconstruir | `podman-compose up -d --build [servicio]` |
| Limpiar todo | `podman-compose down -v && podman system prune -af` |

### Snapshots (puntos de restauración)

```bash
# Desde el HOST — guardar estado
virsh snapshot-create-as guidewire_default snap-infra-ok --description "Infra levantada OK"

# Listar
virsh snapshot-list guidewire_default

# Restaurar
virsh snapshot-revert guidewire_default snap-infra-ok
```

---

## Troubleshooting

### Vagrant no conecta a libvirt

```
Error: Failed to connect to libvirt
```

**Solución**: Verifica permisos de grupo y que libvirtd está corriendo:

```bash
groups | grep libvirt    # ¿Aparece libvirt?
sudo systemctl status libvirtd
newgrp libvirt           # Aplica permisos sin logout
```

### La box no descarga

```
Error: The box could not be found
```

**Solución**: Descargar manualmente:

```bash
vagrant box add generic/ubuntu2404 --provider libvirt
```

### Kafka no arranca

```
Error: kafka exited with code 1
```

**Solución**: Verificar que el puerto 9092 está libre y que hay suficiente memoria:

```bash
podman logs kafka
free -h                  # ¿Hay al menos 1.5GB libres?
ss -tlnp | grep 9092    # ¿Puerto ocupado?
```

### Apicurio no conecta a PostgreSQL

```
Error: Connection refused
```

**Solución**: Esperar a que PostgreSQL pase el healthcheck:

```bash
podman-compose ps        # ¿PostgreSQL está "healthy"?
podman logs postgres     # ¿Hay errores?
```

### Plugin vagrant-libvirt no compila

**Solución**: Instalar todas las dependencias de build:

```bash
sudo apt install -y libxslt-dev libxml2-dev zlib1g-dev ruby-dev libvirt-dev
vagrant plugin install vagrant-libvirt
```

### Puertos no accesibles desde el host

**Solución**: Verificar port forwarding en Vagrantfile y que la VM está corriendo:

```bash
vagrant port             # Lista puertos forwarded
vagrant status           # ¿VM está running?
```

---

## Desinstalación completa

```bash
# 1. Destruir la VM
cd lab && vagrant destroy -f

# 2. Eliminar box descargada
vagrant box remove generic/ubuntu2404

# 3. Desinstalar plugin
vagrant plugin uninstall vagrant-libvirt

# 4. Desinstalar Vagrant
sudo dpkg -r vagrant                     # Debian/Ubuntu
# sudo dnf remove vagrant               # Fedora

# 5. Desinstalar libvirt (opcional)
sudo apt remove -y libvirt-daemon-system qemu-system-x86 virt-manager
```

---

## Referencias

- [Vagrant Documentation](https://developer.hashicorp.com/vagrant/docs)
- [vagrant-libvirt Plugin](https://github.com/vagrant-libvirt/vagrant-libvirt)
- [Podman Documentation](https://docs.podman.io)
- [Podman Compose](https://github.com/containers/podman-compose)
- [Lab Environment spec.yml](../../../infra/lab-environment/spec.yml)
- [Lab Environment docs](README.md)
