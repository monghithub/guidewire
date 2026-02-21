# Desinstalación y limpieza de CRC (OpenShift Local)

> [Volver al README principal](../../README.md) · [INSTALL.md](../../openspecs/docs/infra/lab-environment/INSTALL.md)

Este documento recoge todos los cambios realizados en el sistema host durante la
instalación y configuración de CRC, para poder revertirlos limpiamente.

## 1. Parar CRC y eliminar la VM

```bash
crc stop
crc delete        # elimina la VM y discos
crc cleanup       # revierte cambios de red (dnsmasq, NetworkManager, libvirt)
```

## 2. Desinstalar CRC

```bash
rm -rf ~/.crc                          # cache, kubeconfig, binarios (oc, crc)
rm -f ~/.local/bin/crc                 # symlink al binario
```

## 3. Revertir systemd user units

CRC crea sockets y servicios de usuario:

```bash
systemctl --user stop crc-daemon.service
systemctl --user disable crc-http.socket crc-vsock.socket
rm -f ~/.config/systemd/user/crc-daemon.service
rm -f ~/.config/systemd/user/crc-http.socket
rm -f ~/.config/systemd/user/crc-vsock.socket
systemctl --user daemon-reload
```

## 4. Limpiar /etc/hosts

CRC añade entradas con `# Added by CRC` al final de `/etc/hosts`:

```bash
sudo sed -i '/# Added by CRC/d' /etc/hosts
# También borrar las líneas con *.apps-crc.testing y *.crc.testing
sudo sed -i '/apps-crc\.testing/d' /etc/hosts
sudo sed -i '/api\.crc\.testing/d' /etc/hosts
```

## 5. Revertir sysctl (ip_unprivileged_port_start)

Se bajó el puerto mínimo para que `oc port-forward` pudiera usar el puerto 80:

```bash
sudo sysctl net.ipv4.ip_unprivileged_port_start=1024
```

Si se persistió en `/etc/sysctl.d/`, eliminar:

```bash
sudo rm -f /etc/sysctl.d/99-crc-unprivileged-ports.conf
```

## 6. Revertir iptables (si se flusharon reglas de K3s)

Si se paró K3s y se flushearon sus reglas NAT para liberar puertos 80/443:

```bash
# Re-arrancar K3s para que regenere sus reglas
sudo systemctl start k3s
```

Si K3s ya no se necesita:

```bash
sudo systemctl stop k3s
sudo systemctl disable k3s
# K3s tiene su propio uninstall:
# /usr/local/bin/k3s-uninstall.sh
```

## 7. Limpiar red libvirt

`crc cleanup` debería encargarse, pero verificar:

```bash
# Comprobar que no quedan redes CRC
virsh -c qemu:///system net-list --all | grep crc

# Comprobar que no quedan pools CRC
virsh -c qemu:///system pool-list --all | grep crc
```

## 8. Limpiar NetworkManager/dnsmasq

`crc cleanup` revierte esto, pero verificar:

```bash
# No debería existir:
ls /etc/NetworkManager/dnsmasq.d/crc.conf
ls /etc/NetworkManager/conf.d/crc-nm-dnsmasq.conf
```

Si existen, eliminarlos y reiniciar NetworkManager:

```bash
sudo rm -f /etc/NetworkManager/dnsmasq.d/crc.conf
sudo rm -f /etc/NetworkManager/conf.d/crc-nm-dnsmasq.conf
sudo systemctl restart NetworkManager
```

---

## Resumen de cambios en el sistema host

| Cambio | Archivo/Ubicación | Revertir |
|--------|-------------------|----------|
| Binarios CRC | `~/.crc/bin/`, `~/.local/bin/crc` | Borrar |
| VM + discos | `~/.crc/machines/`, `~/.crc/cache/` | `crc delete` + borrar `~/.crc` |
| systemd units | `~/.config/systemd/user/crc-*` | disable + borrar |
| /etc/hosts | Entradas `*.apps-crc.testing` | Borrar líneas |
| dnsmasq | `/etc/NetworkManager/dnsmasq.d/crc.conf` | `crc cleanup` o borrar |
| NetworkManager | `/etc/NetworkManager/conf.d/crc-nm-dnsmasq.conf` | `crc cleanup` o borrar |
| sysctl | `ip_unprivileged_port_start=80` | Revertir a 1024 |
| iptables NAT | Flush de reglas K3s (`KUBE-*`, `CNI-*`) | Reiniciar K3s |
| K3s parado | `systemctl stop k3s` | `systemctl start k3s` |

## Workarounds aplicados y por qué

### CRC daemon proxy roto (network-mode: user)

**Problema**: En `network-mode: user` (por defecto), el daemon de CRC usa
gvisor-tap-vsock para crear una red virtual (`192.168.127.x`). El daemon
registra forwarders TCP (`:80` → `192.168.127.2:80`) pero el tcpproxy no
puede conectar a esa IP porque no hay ruta TCP real — solo vsock. Resultado:
puertos 80/443 devuelven "404 page not found" del Go HTTP mux.

**Workaround**: `oc port-forward --address 0.0.0.0` directo al servicio
`router-internal-default` en `openshift-ingress`, que sí funciona a través
del túnel vsock del API server (6443).

### K3s Traefik interceptando puertos 80/443

**Problema**: K3s instala Traefik como ingress controller con reglas iptables
DNAT que redirigen TODO el tráfico a puertos 80/443 hacia su pod Traefik
(`10.42.0.x:80`), incluso si hay otro proceso escuchando en esos puertos.

**Workaround**: Parar K3s (`systemctl stop k3s`) y flushear las reglas NAT
residuales (`iptables -t nat -F KUBE-SERVICES`, `-F CNI-HOSTPORT-DNAT`, etc.).

### Puerto 80 privilegiado

**Problema**: `oc port-forward` no puede hacer bind al puerto 80 (privilegiado,
requiere root o `CAP_NET_BIND_SERVICE`).

**Workaround**: `sudo sysctl net.ipv4.ip_unprivileged_port_start=80` permite
que cualquier usuario haga bind a partir del puerto 80.
