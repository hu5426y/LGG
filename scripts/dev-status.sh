#!/usr/bin/env bash
set -euo pipefail

echo "[docker]"
docker compose ps

echo
echo "[ports]"
ss -ltn | grep -E ':8080|:5173|:3307|:6380' || true
