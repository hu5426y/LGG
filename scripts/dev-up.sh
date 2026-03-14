#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
"$ROOT_DIR/scripts/configure-miniapp-api.sh" "${1:-}"
DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker compose up -d --build

cat <<EOF

done.

- miniapp config: $ROOT_DIR/miniapp/config.js

next:
  1. open admin: http://127.0.0.1:5173
  2. open swagger: http://127.0.0.1:8080/swagger-ui.html
  3. import miniapp into WeChat DevTools
  4. if using Windows WeChat DevTools + WSL backend, re-run:
     ./scripts/configure-miniapp-api.sh
EOF
