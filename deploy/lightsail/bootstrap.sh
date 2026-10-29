#!/usr/bin/env bash
# First-boot setup for a fresh Ubuntu 24.04 Lightsail instance.
# Run as the default Ubuntu user: bash bootstrap.sh
set -euo pipefail

if [[ "$(id -u)" -eq 0 ]]; then
  echo "Run this as the regular Ubuntu user, not root." >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

echo "==> Installing base packages"
sudo apt-get update
sudo apt-get install -y --no-install-recommends \
  ca-certificates \
  curl \
  gnupg \
  git \
  ufw

echo "==> Installing Docker Engine and Compose plugin"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
fi
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

echo "==> Adding 2 GB swap for the 2 GB instance"
if ! swapon --show | grep -q .; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
  echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-travel-translator.conf >/dev/null
  sudo sysctl --system >/dev/null
fi

echo "==> Restricting inbound ports"
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "==> Creating deployment directory"
sudo mkdir -p /opt/travel-translator
sudo chown "$USER":"$USER" /opt/travel-translator

echo
echo "Docker version: $(sudo docker --version)"
echo "Compose version: $(sudo docker compose version)"
echo
echo "Setup complete. Log out and back in once so your user can run Docker"
echo "without sudo, then copy the deployment files to /opt/travel-translator."
