#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/miniapp/config.js"

is_wsl() {
  grep -qi microsoft /proc/version 2>/dev/null
}

resolve_windows_lan_ip() {
  if ! is_wsl || ! command -v powershell.exe >/dev/null 2>&1; then
    return
  fi

  local windows_ip
  windows_ip="$(
    powershell.exe -NoProfile -Command "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; Get-NetIPConfiguration | Where-Object { \$_.NetAdapter.Status -eq 'Up' -and \$_.IPv4DefaultGateway -ne \$null -and \$_.IPv4Address -ne \$null -and \$_.InterfaceAlias -notmatch 'WSL|Hyper-V|vEthernet|VMware|VirtualBox|Loopback|Bluetooth|\u84dd\u7259' } | ForEach-Object { \$_.IPv4Address.IPAddress } | Select-Object -First 1" 2>/dev/null | tr -d '\r'
  )"

  if [[ "$windows_ip" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
    printf '%s\n' "$windows_ip"
  fi
}

resolve_default_url() {
  if [[ -n "${MINIAPP_API_BASE_URL:-}" ]]; then
    printf '%s\n' "$MINIAPP_API_BASE_URL"
    return
  fi

  if is_wsl; then
    local windows_lan_ip
    windows_lan_ip="$(resolve_windows_lan_ip)"
    if [[ -n "$windows_lan_ip" ]]; then
      printf 'http://%s:8080/api\n' "$windows_lan_ip"
      return
    fi

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

if is_wsl && [[ "$API_URL" != https://* ]]; then
  printf 'WSL detected: preferring the Windows LAN IP so WeChat DevTools and devices on the same LAN can reach the backend.\n'
  printf 'Override at any time with: ./scripts/configure-miniapp-api.sh <custom-http-or-https-url>\n'
fi

if [[ "$API_URL" == https://* ]]; then
  printf '当前地址适合真机调试。请确认它已加入微信小程序 request 合法域名，或在开发环境中关闭校验。\n'
else
  printf '当前地址更适合本地联调。真机调试建议改成手机可访问的 HTTPS 公网地址，或确保手机和电脑在同一局域网。\n'
fi
