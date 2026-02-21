#!/bin/bash
# Stop the Guidewire POC environment
#
# Kills port-forwards and suspends the CRC VM.
# Data and pods are preserved inside the VM.

set -euo pipefail

echo "Stopping port-forwards..."
pkill -f "oc port-forward.*openshift-ingress" 2>/dev/null && echo "  Port-forwards stopped" || echo "  No port-forwards running"

echo "Stopping CRC..."
crc stop

echo ""
echo "Environment stopped. Run ./start.sh to restart."
