#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_FILE="$ROOT_DIR/backend/container.config.json"

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    printf 'missing required env var: %s\n' "$name" >&2
    exit 1
  fi
}

require_var CLOUDRUN_DB_URL
require_var CLOUDRUN_DB_USERNAME
require_var CLOUDRUN_DB_PASSWORD
require_var CLOUDRUN_REDIS_HOST
require_var CLOUDRUN_JWT_SECRET

SPRING_PROFILE="${CLOUDRUN_SPRING_PROFILE:-dev}"
REDIS_PORT="${CLOUDRUN_REDIS_PORT:-6379}"
SWAGGER_ENABLED="${CLOUDRUN_SWAGGER_ENABLED:-true}"
SIMULATED_RUNS="${CLOUDRUN_SIMULATED_RUNS:-true}"
CPU="${CLOUDRUN_CPU:-1}"
MEM="${CLOUDRUN_MEM:-2}"
MIN_NUM="${CLOUDRUN_MIN_NUM:-0}"
MAX_NUM="${CLOUDRUN_MAX_NUM:-2}"
POLICY_TYPE="${CLOUDRUN_POLICY_TYPE:-cpu}"
POLICY_THRESHOLD="${CLOUDRUN_POLICY_THRESHOLD:-80}"

cat > "$OUTPUT_FILE" <<EOF
{
  "containerPort": 8080,
  "dockerfilePath": "Dockerfile",
  "buildDir": "",
  "cpu": $CPU,
  "mem": $MEM,
  "minNum": $MIN_NUM,
  "maxNum": $MAX_NUM,
  "policyType": "$POLICY_TYPE",
  "policyThreshold": $POLICY_THRESHOLD,
  "envParams": {
    "SPRING_PROFILES_ACTIVE": "$SPRING_PROFILE",
    "SPRING_DATASOURCE_URL": "$CLOUDRUN_DB_URL",
    "SPRING_DATASOURCE_USERNAME": "$CLOUDRUN_DB_USERNAME",
    "SPRING_DATASOURCE_PASSWORD": "$CLOUDRUN_DB_PASSWORD",
    "SPRING_DATA_REDIS_HOST": "$CLOUDRUN_REDIS_HOST",
    "SPRING_DATA_REDIS_PORT": "$REDIS_PORT",
    "CAMPUSRUN_JWT_SECRET": "$CLOUDRUN_JWT_SECRET",
    "CAMPUSRUN_SWAGGER_ENABLED": "$SWAGGER_ENABLED",
    "CAMPUSRUN_RUN_ALLOW_SIMULATED_RUNS": "$SIMULATED_RUNS"
  }
}
EOF

printf 'generated %s\n' "$OUTPUT_FILE"
