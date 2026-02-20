#!/bin/bash
###############################################################################
# setup.sh — Base provisioning for the Guidewire POC lab VM
#
# This script runs once during 'vagrant up' (first time) or when explicitly
# re-provisioned with 'vagrant provision'. It installs Podman, podman-compose,
# configures container registries, and prepares the environment.
###############################################################################

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "============================================================"
echo " Guidewire POC Lab — Base Provisioning"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1. System update
# ---------------------------------------------------------------------------
echo "[1/6] Updating system packages..."
apt-get update -qq
apt-get upgrade -y -qq

# ---------------------------------------------------------------------------
# 2. Install basic tools
# ---------------------------------------------------------------------------
echo "[2/6] Installing basic tools..."
apt-get install -y -qq \
  curl \
  wget \
  jq \
  git \
  unzip \
  ca-certificates \
  gnupg \
  lsb-release \
  python3-pip \
  python3-venv \
  software-properties-common \
  apt-transport-https

# ---------------------------------------------------------------------------
# 3. Install Podman 4.9+
# ---------------------------------------------------------------------------
echo "[3/6] Installing Podman..."
apt-get install -y -qq podman

# Verify Podman version
PODMAN_VERSION=$(podman --version | awk '{print $3}')
echo "    Podman version: ${PODMAN_VERSION}"

# Enable Podman socket for the vagrant user (rootless mode)
loginctl enable-linger vagrant
su - vagrant -c "systemctl --user enable podman.socket || true"
su - vagrant -c "systemctl --user start podman.socket || true"

# ---------------------------------------------------------------------------
# 4. Install podman-compose
# ---------------------------------------------------------------------------
echo "[4/6] Installing podman-compose..."
# Use pipx or pip to install podman-compose to get a recent version
python3 -m pip install --break-system-packages podman-compose 2>/dev/null \
  || pip3 install podman-compose 2>/dev/null \
  || apt-get install -y -qq podman-compose

COMPOSE_VERSION=$(podman-compose --version 2>/dev/null | head -1 || echo "installed")
echo "    podman-compose: ${COMPOSE_VERSION}"

# ---------------------------------------------------------------------------
# 5. Configure container registries
# ---------------------------------------------------------------------------
echo "[5/6] Configuring container registries..."
cat > /etc/containers/registries.conf <<'REGISTRIES'
# Container image registries for Guidewire POC lab
# Search order: Docker Hub, Quay.io (Red Hat), GitHub Container Registry

unqualified-search-registries = ["docker.io", "quay.io", "ghcr.io"]

[[registry]]
location = "docker.io"
prefix = "docker.io"

[[registry]]
location = "quay.io"
prefix = "quay.io"

[[registry]]
location = "ghcr.io"
prefix = "ghcr.io"
REGISTRIES

# ---------------------------------------------------------------------------
# 6. Create volume directories and set ownership
# ---------------------------------------------------------------------------
echo "[6/6] Creating volume directories..."
mkdir -p /home/vagrant/volumes/pgdata
mkdir -p /home/vagrant/volumes/kafka-data
mkdir -p /home/vagrant/volumes/artemis-data
chown -R vagrant:vagrant /home/vagrant/volumes

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " Provisioning complete!"
echo "============================================================"
echo ""
echo " Podman:         $(podman --version)"
echo " podman-compose: $(podman-compose --version 2>/dev/null | head -1)"
echo ""
echo " Next steps:"
echo "   1. vagrant ssh"
echo "   2. cd ~/lab-guidewire/podman"
echo "   3. podman-compose up -d"
echo ""
echo " Optional: install dev tools with:"
echo "   sudo bash ~/lab-guidewire/provision/install-dev-tools.sh"
echo "============================================================"
