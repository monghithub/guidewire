#!/bin/bash
# Port-forward the OpenShift router to 0.0.0.0:80
# This bypasses the CRC daemon proxy (broken in user network-mode)
# and exposes all OpenShift routes on the LAN interface.
#
# Prerequisites:
#   sudo sysctl net.ipv4.ip_unprivileged_port_start=80
#   sudo systemctl stop k3s  (if K3s is installed)
#   sudo iptables -t nat -F  (flush K3s DNAT rules that steal port 80/443)
#
# Usage: ./pf-router.sh
# Access: http://<route-hostname>/ from any machine on the LAN
# Requires /etc/hosts entries pointing *.apps-crc.testing to this machine's IP

set -euo pipefail

export KUBECONFIG="${KUBECONFIG:-$HOME/.crc/machines/crc/kubeconfig}"
OC="${OC:-$HOME/.crc/bin/oc/oc}"
PORT="${PORT:-80}"

echo "Starting router port-forward on 0.0.0.0:${PORT}..."
echo "Press Ctrl+C to stop"

exec "$OC" port-forward --address 0.0.0.0 \
  -n openshift-ingress svc/router-internal-default \
  "${PORT}:80"
