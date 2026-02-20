#!/bin/bash
###############################################################################
# install-dev-tools.sh — Optional development tools for the lab VM (Fedora 41)
#
# Run manually inside the VM when you need to build/compile services locally:
#   sudo bash ~/lab-guidewire/provision/install-dev-tools.sh
#
# Installs: JDK 21 (Temurin), Maven 3.9, Node.js 20 LTS, kcat, jq, httpie
###############################################################################

set -euo pipefail

echo "============================================================"
echo " Guidewire POC Lab — Installing Development Tools (Fedora)"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1. JDK 21 (Eclipse Temurin / Adoptium)
# ---------------------------------------------------------------------------
echo "[1/6] Installing JDK 21 (Eclipse Temurin)..."

# Add Adoptium repo for Fedora
cat > /etc/yum.repos.d/adoptium.repo <<'EOF'
[Adoptium]
name=Adoptium
baseurl=https://packages.adoptium.net/artifactory/rpm/fedora/$releasever/$basearch
enabled=1
gpgcheck=1
gpgkey=https://packages.adoptium.net/artifactory/api/gpg/key/public
EOF

dnf install -y -q temurin-21-jdk

echo "    Java: $(java -version 2>&1 | head -1)"

# ---------------------------------------------------------------------------
# 2. Maven 3.9
# ---------------------------------------------------------------------------
echo "[2/6] Installing Maven 3.9..."

MAVEN_VERSION="3.9.9"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

wget -q "${MAVEN_URL}" -O /tmp/maven.tar.gz
tar -xzf /tmp/maven.tar.gz -C /opt/
ln -sf /opt/apache-maven-${MAVEN_VERSION} /opt/maven
rm -f /tmp/maven.tar.gz

# Set up Maven environment for all users
cat > /etc/profile.d/maven.sh <<'EOF'
export MAVEN_HOME=/opt/maven
export PATH="${MAVEN_HOME}/bin:${PATH}"
EOF
source /etc/profile.d/maven.sh

echo "    Maven: $(mvn --version 2>&1 | head -1)"

# ---------------------------------------------------------------------------
# 3. Node.js 20 LTS
# ---------------------------------------------------------------------------
echo "[3/6] Installing Node.js 20 LTS..."

dnf module install -y -q nodejs:20/default 2>/dev/null \
  || dnf install -y -q nodejs

echo "    Node.js: $(node --version)"
echo "    npm: $(npm --version)"

# ---------------------------------------------------------------------------
# 4. kcat (kafkacat) — Kafka CLI tool
# ---------------------------------------------------------------------------
echo "[4/6] Installing kcat (kafkacat)..."

dnf install -y -q kcat 2>/dev/null \
  || dnf install -y -q kafkacat 2>/dev/null \
  || echo "    WARNING: kcat not available in repos, skipping."

if command -v kcat &>/dev/null; then
  echo "    kcat: $(kcat -V 2>&1 | head -1)"
fi

# ---------------------------------------------------------------------------
# 5. jq — JSON processor
# ---------------------------------------------------------------------------
echo "[5/6] Installing jq..."
dnf install -y -q jq
echo "    jq: $(jq --version)"

# ---------------------------------------------------------------------------
# 6. HTTPie — Human-friendly HTTP client
# ---------------------------------------------------------------------------
echo "[6/6] Installing HTTPie..."
dnf install -y -q httpie 2>/dev/null \
  || pip3 install httpie

echo "    HTTPie: $(http --version 2>&1 | head -1)"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " Development tools installed successfully!"
echo "============================================================"
echo ""
echo " JAVA_HOME:  $(dirname $(dirname $(readlink -f $(which java))))"
echo " MAVEN_HOME: /opt/maven"
echo ""
echo " Available commands:"
echo "   java -version       — JDK 21 (Temurin)"
echo "   mvn --version       — Maven 3.9"
echo "   node --version      — Node.js 20 LTS"
echo "   kcat / kafkacat     — Kafka CLI"
echo "   jq --version        — JSON processor"
echo "   http --version      — HTTPie"
echo "============================================================"
