#!/bin/bash
###############################################################################
# deploy-all.sh — Deploy Guidewire POC to OpenShift Local (CRC)
#
# Usage:
#   ./deploy-all.sh          # Full deploy
#   ./deploy-all.sh --infra  # Infrastructure only
#   ./deploy-all.sh --apps   # Applications only
#
# Prerequisites:
#   - CRC running: crc status
#   - Logged in: oc whoami
###############################################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WAIT]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Check prerequisites
command -v oc &>/dev/null || error "oc CLI not found. Run: eval \$(crc oc-env)"
oc whoami &>/dev/null || error "Not logged in. Run: oc login -u developer -p developer https://api.crc.testing:6443"

wait_for_pods() {
  local ns=$1
  local timeout=${2:-300}
  warn "Waiting for pods in $ns to be ready (timeout: ${timeout}s)..."
  oc wait --for=condition=Ready pods --all -n "$ns" --timeout="${timeout}s" 2>/dev/null || true
}

wait_for_operator() {
  local name=$1
  local timeout=${2:-120}
  warn "Waiting for operator $name to be ready (timeout: ${timeout}s)..."
  local count=0
  while [ $count -lt $timeout ]; do
    if oc get csv -n openshift-operators 2>/dev/null | grep -q "$name.*Succeeded"; then
      info "Operator $name is ready"
      return 0
    fi
    sleep 5
    count=$((count + 5))
  done
  warn "Operator $name may not be fully ready yet, continuing..."
}

deploy_infra() {
  info "============================================================"
  info " Phase 1: Namespaces"
  info "============================================================"
  oc apply -f namespaces.yml

  info "============================================================"
  info " Phase 2: Operators"
  info "============================================================"
  oc apply -f operators/strimzi-subscription.yml
  oc apply -f operators/apicurio-subscription.yml

  wait_for_operator "strimzi" 180
  wait_for_operator "apicurio" 180

  info "============================================================"
  info " Phase 3: PostgreSQL"
  info "============================================================"
  oc apply -f infra/postgres/secret.yml
  oc apply -f infra/postgres/configmap-init-db.yml
  oc apply -f infra/postgres/pvc.yml
  oc apply -f infra/postgres/deployment.yml
  oc apply -f infra/postgres/service.yml

  warn "Waiting for PostgreSQL to be ready..."
  oc wait --for=condition=Available deploy/postgres -n guidewire-infra --timeout=120s

  info "============================================================"
  info " Phase 4: Kafka (Strimzi)"
  info "============================================================"
  oc apply -f infra/kafka/kafka-cluster.yml

  warn "Waiting for Kafka cluster to be ready (this may take a few minutes)..."
  oc wait kafka/kafka-cluster --for=condition=Ready -n guidewire-infra --timeout=300s 2>/dev/null || \
    warn "Kafka cluster may still be starting, continuing..."

  oc apply -f infra/kafka/kafka-topics.yml

  info "============================================================"
  info " Phase 5: Apicurio Registry"
  info "============================================================"
  oc apply -f infra/apicurio/apicurio-registry.yml

  info "============================================================"
  info " Phase 6: Kafdrop"
  info "============================================================"
  oc apply -f infra/kafdrop/deployment.yml
  oc apply -f infra/kafdrop/service.yml
  oc apply -f infra/kafdrop/route.yml

  info "============================================================"
  info " Phase 7: 3Scale APIcast"
  info "============================================================"
  oc apply -f infra/threescale/configmap-apicast.yml
  oc apply -f infra/threescale/deployment.yml
  oc apply -f infra/threescale/service.yml
  oc apply -f infra/threescale/route.yml

  wait_for_pods guidewire-infra 300

  info "============================================================"
  info " Infrastructure deployed!"
  info "============================================================"
  oc get pods -n guidewire-infra
  echo ""
  oc get routes -n guidewire-infra
}

deploy_apps() {
  info "============================================================"
  info " Phase 8: Application ImageStreams + BuildConfigs"
  info "============================================================"
  for svc in billing-service camel-gateway incidents-service customers-service drools-engine; do
    info "Applying BuildConfig for $svc..."
    oc apply -f apps/$svc/buildconfig.yml
  done

  info "============================================================"
  info " Phase 9: Build Application Images"
  info "============================================================"
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
  for svc in billing-service camel-gateway incidents-service customers-service drools-engine; do
    info "Building $svc from $PROJECT_ROOT/components/$svc ..."
    oc start-build $svc -n guidewire-apps \
      --from-dir="$PROJECT_ROOT/components/$svc" \
      --follow || warn "Build for $svc may have failed, continuing..."
  done

  info "============================================================"
  info " Phase 10: Deploy Applications"
  info "============================================================"
  for svc in billing-service camel-gateway incidents-service customers-service drools-engine; do
    info "Deploying $svc..."
    oc apply -f apps/$svc/deployment.yml
    oc apply -f apps/$svc/service.yml
    oc apply -f apps/$svc/route.yml
  done

  wait_for_pods guidewire-apps 300

  info "============================================================"
  info " Applications deployed!"
  info "============================================================"
  oc get pods -n guidewire-apps
  echo ""
  oc get routes -n guidewire-apps
}

# Main
case "${1:-all}" in
  --infra)
    deploy_infra
    ;;
  --apps)
    deploy_apps
    ;;
  all|*)
    deploy_infra
    deploy_apps
    info "============================================================"
    info " Full stack deployed successfully!"
    info "============================================================"
    info "Console: $(crc console --url 2>/dev/null || echo 'https://console-openshift-console.apps-crc.testing')"
    info "API Gateway: https://apicast-guidewire-infra.apps-crc.testing"
    info "Kafdrop: https://kafdrop-guidewire-infra.apps-crc.testing"
    info "Apicurio: https://apicurio-guidewire-infra.apps-crc.testing"
    ;;
esac
