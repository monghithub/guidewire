#!/bin/bash
###############################################################################
# register-contracts.sh — Register API contracts in Apicurio Registry
#
# Registers all OpenAPI, AsyncAPI, and Avro contracts from the contracts/
# directory into the Apicurio Registry using the REST API v2.
#
# Usage:
#   ./register-contracts.sh                           # Uses internal cluster URL
#   ./register-contracts.sh http://my-registry:8080   # Custom base URL
#
# Idempotent: safe to run multiple times (uses ifExists=UPDATE).
#
# NOTE: Make this script executable with: chmod +x register-contracts.sh
###############################################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
CONTRACTS_DIR="$PROJECT_ROOT/contracts"

# Default to internal cluster URL; override with first argument
REGISTRY_URL="${1:-http://apicurio-registry.guidewire-infra.svc.cluster.local:8080}"
# Strip trailing slash if present
REGISTRY_URL="${REGISTRY_URL%/}"

API_BASE="$REGISTRY_URL/apis/registry/v2"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error() { echo -e "${RED}[FAIL]${NC}  $1"; }

# Counters
SUCCESS=0
FAILED=0

###############################################################################
# register_artifact — Register a single artifact in Apicurio Registry v2
#
# Uses: POST /groups/{groupId}/artifacts with ifExists=UPDATE header
#
# Args:
#   $1 — group ID (e.g., "openapi", "asyncapi", "avro")
#   $2 — artifact ID (filename without extension)
#   $3 — file path
#   $4 — content type (application/x-yaml or application/json)
#   $5 — artifact type (OPENAPI, ASYNCAPI, AVRO)
###############################################################################
register_artifact() {
  local group_id="$1"
  local artifact_id="$2"
  local file_path="$3"
  local content_type="$4"
  local artifact_type="$5"

  if [ ! -f "$file_path" ]; then
    error "$group_id/$artifact_id — file not found: $file_path"
    FAILED=$((FAILED + 1))
    return 1
  fi

  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST \
    "${API_BASE}/groups/${group_id}/artifacts" \
    -H "Content-Type: ${content_type}" \
    -H "X-Registry-ArtifactId: ${artifact_id}" \
    -H "X-Registry-ArtifactType: ${artifact_type}" \
    -H "X-Registry-Version: 1" \
    -H "If-Exists: UPDATE" \
    --data-binary "@${file_path}" \
    --max-time 30)

  if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
    info "$group_id/$artifact_id (HTTP $http_code)"
    SUCCESS=$((SUCCESS + 1))
  else
    error "$group_id/$artifact_id (HTTP $http_code)"
    FAILED=$((FAILED + 1))
  fi
}

###############################################################################
# Main
###############################################################################

echo ""
echo "==========================================================="
echo " Registering API contracts in Apicurio Registry"
echo " Registry: $REGISTRY_URL"
echo "==========================================================="
echo ""

# Verify registry is reachable
echo -n "Checking registry health... "
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${REGISTRY_URL}/health/ready" --max-time 10 2>/dev/null || echo "000")
if [[ "$HEALTH_CODE" =~ ^2[0-9][0-9]$ ]]; then
  echo -e "${GREEN}OK${NC}"
else
  echo -e "${RED}UNREACHABLE (HTTP $HEALTH_CODE)${NC}"
  echo ""
  echo "Registry at $REGISTRY_URL is not reachable."
  echo "If running from outside the cluster, use the external route:"
  echo "  $0 http://apicurio-guidewire-infra.apps-crc.testing"
  exit 1
fi
echo ""

# -------------------------------------------------------
#  OpenAPI specs (8 files)
# -------------------------------------------------------
echo "--- OpenAPI specs ---"
for file in "$CONTRACTS_DIR"/openapi/*.yml; do
  artifact_id="$(basename "$file" .yml)"
  register_artifact "openapi" "$artifact_id" "$file" "application/x-yaml" "OPENAPI"
done
echo ""

# -------------------------------------------------------
#  AsyncAPI specs (1 file)
# -------------------------------------------------------
echo "--- AsyncAPI specs ---"
for file in "$CONTRACTS_DIR"/asyncapi/*.yml; do
  artifact_id="$(basename "$file" .yml)"
  register_artifact "asyncapi" "$artifact_id" "$file" "application/x-yaml" "ASYNCAPI"
done
echo ""

# -------------------------------------------------------
#  Avro schemas (6 files)
# -------------------------------------------------------
echo "--- Avro schemas ---"
for file in "$CONTRACTS_DIR"/avro/*.avsc; do
  artifact_id="$(basename "$file" .avsc)"
  register_artifact "avro" "$artifact_id" "$file" "application/json" "AVRO"
done
echo ""

# -------------------------------------------------------
#  Summary
# -------------------------------------------------------
echo "==========================================================="
echo -e " Results: ${GREEN}${SUCCESS} succeeded${NC}, ${RED}${FAILED} failed${NC}"
echo "==========================================================="
echo ""

if [ "$FAILED" -gt 0 ]; then
  exit 1
fi
