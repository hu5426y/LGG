#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/miniapp/config.js"

is_wsl() {
  grep -qi microsoft /proc/version 2>/dev/null
}

resolve_default_url() {
  if [[ -n "${MINIAPP_API_BASE_URL:-}" ]]; then
    printf '%s\n' "$MINIAPP_API_BASE_URL"
    return
  fi

  printf 'http://127.0.0.1:8080/api\n'
}

normalize_api_url() {
  local raw_url="$1"
  if [[ "$raw_url" != http://* && "$raw_url" != https://* ]]; then
    raw_url="https://$raw_url"
  fi
  raw_url="${raw_url%/}"
  if [[ "$raw_url" != */api ]]; then
    raw_url="$raw_url/api"
  fi
  printf '%s\n' "$raw_url"
}

API_URL="$(normalize_api_url "${1:-$(resolve_default_url)}")"
CLOUD_ENV_ID="${MINIAPP_CLOUD_ENV_ID:-cloud1-8g7ph5n7f6515ead}"
CLOUD_SERVICE="${MINIAPP_CLOUD_SERVICE:-}"
TENCENT_MAP_KEY="${MINIAPP_TENCENT_MAP_KEY:-QN6BZ-OUWCH-5TODB-WUDD6-XQAXV-U7FKQ}"
MAP_SUB_KEY="${MINIAPP_MAP_SUB_KEY:-}"
SIMULATION_ENABLED="${MINIAPP_SIMULATION_ENABLED:-true}"

cat > "$CONFIG_FILE" <<EOF
module.exports = {
  apiBaseUrl: '$API_URL',
  cloudEnvId: '$CLOUD_ENV_ID',
  cloudService: '$CLOUD_SERVICE',
  tencentMapKey: '$TENCENT_MAP_KEY',
  mapSubKey: '$MAP_SUB_KEY',
  simulationEnabled: $SIMULATION_ENABLED
}
EOF

printf 'miniapp apiBaseUrl => %s\n' "$API_URL"
printf 'miniapp cloudEnvId => %s\n' "${CLOUD_ENV_ID:-<empty>}"
printf 'miniapp cloudService => %s\n' "${CLOUD_SERVICE:-<empty>}"

if [[ "$API_URL" == https://* ]]; then
  printf '当前地址适合真机调试。请确认它已加入微信小程序 request 合法域名，或在开发环境中关闭校验。\n'
else
  printf '当前地址默认用于开发者工具本地联调。真机调试时请改成手机可访问的 HTTPS 公网地址。\n'
  printf '如需覆盖，执行：./scripts/configure-miniapp-api.sh <custom-http-or-https-url>\n'
fi
