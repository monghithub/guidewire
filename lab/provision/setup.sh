#!/bin/bash
###############################################################################
# setup.sh — Base provisioning for the Guidewire POC lab VM (Fedora 41)
#
# This script runs once during 'vagrant up' (first time) or when explicitly
# re-provisioned with 'vagrant provision'. It installs Podman, podman-compose,
# configures container registries, and prepares the environment.
###############################################################################

set -euo pipefail

echo "============================================================"
echo " Guidewire POC Lab — Base Provisioning (Fedora 41)"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1. System update
# ---------------------------------------------------------------------------
echo "[1/6] Updating system packages..."
dnf update -y -q

# ---------------------------------------------------------------------------
# 2. Install basic tools
# ---------------------------------------------------------------------------
echo "[2/6] Installing basic tools..."
dnf install -y -q \
  curl \
  wget \
  jq \
  git \
  unzip \
  ca-certificates \
  python3-pip

# ---------------------------------------------------------------------------
# 3. Install Podman (comes pre-installed on Fedora, ensure latest)
# ---------------------------------------------------------------------------
echo "[3/6] Installing Podman..."
dnf install -y -q podman podman-plugins

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
dnf install -y -q podman-compose 2>/dev/null \
  || pip3 install podman-compose

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
