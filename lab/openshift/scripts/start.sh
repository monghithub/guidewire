#!/bin/bash
# Start the Guidewire POC environment
#
# Starts CRC, removes broken daemon forwarders, cleans K3s iptables rules,
# and launches port-forwards for HTTP (80) and HTTPS (443).
#
# Prerequisites (one-time):
#   sudo sysctl net.ipv4.ip_unprivileged_port_start=80
#   sudo systemctl stop k3s
#   sudo systemctl disable k3s

set -euo pipefail

export KUBECONFIG="${KUBECONFIG:-$HOME/.crc/machines/crc/kubeconfig}"
OC="${OC:-$HOME/.crc/bin/oc/oc}"

# ── 1. Start CRC ──────────────────────────────────────────────────────
echo "=== Starting CRC ==="
crc start

# ── 2. Remove broken daemon forwarders (user network-mode bug) ────────
echo ""
echo "=== Removing broken CRC daemon forwarders ==="
for port_local_remote in ":80,192.168.127.2:80" ":443,192.168.127.2:443"; do
  local_addr="${port_local_remote%%,*}"
  remote_addr="${port_local_remote##*,}"
  curl -s -X POST --unix-socket "$HOME/.crc/crc-http.sock" \
    -H 'Content-Type: application/json' \
    -d "{\"local\":\"${local_addr}\",\"remote\":\"${remote_addr}\",\"protocol\":\"tcp\"}" \
    http://localhost/network/services/forwarder/unexpose >/dev/null 2>&1 || true
done
echo "  Done"

# ── 3. Flush K3s iptables DNAT rules (if K3s was installed) ──────────
echo ""
echo "=== Flushing K3s iptables rules ==="
if sudo iptables -t nat -L KUBE-SERVICES -n >/dev/null 2>&1; then
  sudo iptables -t nat -F KUBE-SERVICES 2>/dev/null || true
  sudo iptables -t nat -F CNI-HOSTPORT-DNAT 2>/dev/null || true
  # Flush traefik-specific chains
  sudo iptables -t nat -F KUBE-EXT-UQMCRMJZLI3FTLDP 2>/dev/null || true
  sudo iptables -t nat -F KUBE-EXT-CVG3OEGEH7H5P3HQ 2>/dev/null || true
  echo "  Flushed"
else
  echo "  No K3s rules found (skipped)"
fi

# ── 4. Wait for pods ─────────────────────────────────────────────────
echo ""
echo "=== Waiting for pods ==="
$OC wait --for=condition=Ready pod -l strimzi.io/kind=Kafka -n guidewire-infra --timeout=120s 2>/dev/null || true
echo "  Infra pods:"
$OC get pods -n guidewire-infra --no-headers 2>/dev/null | grep -v Completed | grep -v Error
echo ""
echo "  App pods:"
$OC get pods -n guidewire-apps --no-headers 2>/dev/null | grep -v Completed | grep -v Error | grep -v build

# ── 5. Kill stale port-forwards and start fresh ──────────────────────
echo ""
echo "=== Starting port-forwards ==="
pkill -f "oc port-forward.*openshift-ingress" 2>/dev/null || true
sleep 1

$OC port-forward --address 0.0.0.0 -n openshift-ingress svc/router-internal-default 80:80 >/dev/null 2>&1 &
PF_HTTP=$!
$OC port-forward --address 0.0.0.0 -n openshift-ingress svc/router-internal-default 443:443 >/dev/null 2>&1 &
PF_HTTPS=$!
sleep 2

echo "  HTTP  (port 80)  — PID $PF_HTTP"
echo "  HTTPS (port 443) — PID $PF_HTTPS"

# ── 6. Verify ────────────────────────────────────────────────────────
echo ""
echo "=== Verifying routes ==="
for host in \
  kafdrop-guidewire-infra.apps-crc.testing \
  apicurio-guidewire-infra.apps-crc.testing \
  console-openshift-console.apps-crc.testing; do
  code=$(curl -sk -o /dev/null -w '%{http_code}' -H "Host: $host" https://127.0.0.1/ 2>/dev/null || \
         curl -s -o /dev/null -w '%{http_code}' -H "Host: $host" http://127.0.0.1/ 2>/dev/null)
  printf "  %-55s %s\n" "$host" "$code"
done

echo ""
echo "Environment ready."
echo "OpenShift console: https://console-openshift-console.apps-crc.testing"
echo "  kubeadmin / xtLsK-LLIzY-6UVEd-UESLR"
