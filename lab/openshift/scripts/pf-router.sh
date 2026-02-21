#!/bin/bash
# Port-forward the OpenShift router to 0.0.0.0:8090
# This bypasses the CRC daemon proxy (which doesn't forward properly)
# and exposes all OpenShift routes on the LAN interface.
#
# Usage: ./pf-router.sh
# Access: http://<route-hostname>:8090/ from any machine on the LAN
# Requires /etc/hosts entries pointing *.apps-crc.testing to this machine's IP

set -euo pipefail

export KUBECONFIG="${KUBECONFIG:-$HOME/.crc/machines/crc/kubeconfig}"
OC="${OC:-$HOME/.crc/bin/oc/oc}"
PORT="${PORT:-8090}"

echo "Starting router port-forward on 0.0.0.0:${PORT}..."
echo "Press Ctrl+C to stop"

exec "$OC" port-forward --address 0.0.0.0 \
  -n openshift-ingress svc/router-internal-default \
  "${PORT}:80"
