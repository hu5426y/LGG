#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/miniapp/config.js"

resolve_default_url() {
  if [[ -n "${MINIAPP_API_BASE_URL:-}" ]]; then
    printf '%s\n' "$MINIAPP_API_BASE_URL"
    return
  fi

  if grep -qi microsoft /proc/version 2>/dev/null; then
    local wsl_ip
    wsl_ip="$(ip -4 addr show eth0 2>/dev/null | awk '/inet / {print $2}' | cut -d/ -f1 | head -n 1)"
    if [[ -n "$wsl_ip" ]]; then
      printf 'http://%s:8080/api\n' "$wsl_ip"
      return
    fi
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

cat > "$CONFIG_FILE" <<EOF
module.exports = {
  apiBaseUrl: '$API_URL'
}
EOF

printf 'miniapp apiBaseUrl => %s\n' "$API_URL"
if [[ "$API_URL" == https://* ]]; then
  printf '当前地址适合真机调试。请确认它已加入微信小程序 request 合法域名，或在开发环境中关闭校验。\n'
else
  printf '当前地址更适合开发者工具调试。真机调试建议改成 HTTPS 公网地址。\n'
fi
