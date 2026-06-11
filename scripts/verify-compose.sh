#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"
ADMIN_USERNAME="${SMOKE_ADMIN_USERNAME:-${BOOTSTRAP_ADMIN_USERNAME:-admin}}"
ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-${BOOTSTRAP_ADMIN_PASSWORD:-}}"

if [ -z "$ADMIN_PASSWORD" ]; then
  echo "SMOKE_ADMIN_PASSWORD or BOOTSTRAP_ADMIN_PASSWORD is required" >&2
  exit 2
fi

echo "Checking frontend at ${BASE_URL}/"
curl -fsS "${BASE_URL}/" | grep -qi "<!doctype html"

echo "Checking backend health through Nginx"
curl -fsS "${BASE_URL}/api/health" | grep -q '"status":"UP"'

echo "Checking login through Nginx"
login_response="$(curl -fsS \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
  "${BASE_URL}/api/auth/login")"

echo "${login_response}" | grep -q '"code":0'
echo "${login_response}" | grep -q '"token"'

echo "Smoke checks passed"
