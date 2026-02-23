# Tareas pendientes que requieren sudo

## 1. Matar procesos residuales de K3s

K3s fue detenido pero los containerd-shim siguen vivos e interceptan puertos 80/443 via Traefik.

```bash
# 1. Matar procesos huérfanos (Traefik, containerd-shims) y limpiar iptables
sudo bash /usr/local/bin/k3s-killall.sh

# 2. IMPORTANTE: Desactivar k3s (tiene Restart=always y está enabled)
sudo systemctl disable k3s

# 3. Verificar que los puertos estén libres
sudo ss -tlnp | grep -E ':80|:443'
```

## 2. Aumentar memoria de CRC (opcional)

La VM CRC no tiene suficiente RAM para todos los pods. Actualmente muchos están en Pending.

```bash
# Ver configuración actual
crc config get memory

# Aumentar a 14GB (requiere restart de CRC)
crc config set memory 14336
crc stop
crc start
```

## 3. Verificar acceso desde host después de matar K3s

```bash
# Debe devolver la página de OpenShift, NO "404 page not found" de Traefik
curl -sk https://console-openshift-console.apps-crc.testing | head -20
```

## 4. Abrir puerto 4200 para el Angular Simulator (acceso LAN)

```bash
# Con ufw
sudo ufw allow 4200/tcp

# O con iptables
sudo iptables -A INPUT -p tcp --dport 4200 -j ACCEPT

# Verificar
curl http://192.168.1.135:4200
```

---
Generado: 2026-02-22
