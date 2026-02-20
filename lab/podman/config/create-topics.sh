#!/bin/bash
###############################################################################
# create-topics.sh — Create Kafka topics for Guidewire POC
#
# This script creates all required Kafka topics. Run it after the Kafka broker
# is healthy and accepting connections.
#
# Usage (from inside the VM):
#   podman exec gw-kafka /bin/bash /opt/kafka/config/create-topics.sh
#
# Or run directly if kafka CLI tools are installed:
#   bash ~/lab-guidewire/podman/config/create-topics.sh
###############################################################################

set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"
KAFKA_BIN="${KAFKA_BIN_DIR:-/opt/kafka/bin}"

echo "============================================================"
echo " Creating Kafka topics for Guidewire POC"
echo " Bootstrap server: ${BOOTSTRAP_SERVER}"
echo "============================================================"

# Function to create a topic with error handling
create_topic() {
  local topic_name="$1"
  local partitions="$2"
  local replication_factor="${3:-1}"
  local extra_config="${4:-}"

  echo -n "  Creating topic: ${topic_name} (partitions=${partitions}, rf=${replication_factor})... "

  local cmd="${KAFKA_BIN}/kafka-topics.sh \
    --bootstrap-server ${BOOTSTRAP_SERVER} \
    --create \
    --topic ${topic_name} \
    --partitions ${partitions} \
    --replication-factor ${replication_factor} \
    --if-not-exists"

  if [ -n "${extra_config}" ]; then
    cmd="${cmd} --config ${extra_config}"
  fi

  if eval "${cmd}" 2>/dev/null; then
    echo "OK"
  else
    echo "ALREADY EXISTS or ERROR"
  fi
}

# ---------------------------------------------------------------------------
# Billing topics (3 partitions each)
# ---------------------------------------------------------------------------
echo ""
echo "--- Billing topics ---"
create_topic "billing.invoice-created" 3
create_topic "billing.invoice-status-changed" 3

# ---------------------------------------------------------------------------
# Incidents topics (3 partitions each)
# ---------------------------------------------------------------------------
echo ""
echo "--- Incidents topics ---"
create_topic "incidents.incident-created" 3
create_topic "incidents.incident-status-changed" 3

# ---------------------------------------------------------------------------
# Customers topics (3 partitions each)
# ---------------------------------------------------------------------------
echo ""
echo "--- Customers topics ---"
create_topic "customers.customer-registered" 3
create_topic "customers.customer-status-changed" 3

# ---------------------------------------------------------------------------
# Dead Letter Queue (1 partition, 30-day retention)
# ---------------------------------------------------------------------------
echo ""
echo "--- Dead Letter Queue ---"
create_topic "dlq.errors" 1 1 "retention.ms=2592000000"

# ---------------------------------------------------------------------------
# Verify all topics
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " Verifying topics..."
echo "============================================================"
${KAFKA_BIN}/kafka-topics.sh \
  --bootstrap-server ${BOOTSTRAP_SERVER} \
  --list

echo ""
echo "============================================================"
echo " Topic creation complete!"
echo "============================================================"
