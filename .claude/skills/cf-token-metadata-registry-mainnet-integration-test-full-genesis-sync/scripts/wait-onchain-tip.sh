#!/usr/bin/env bash
# Poll /actuator/health/readiness until components.onchainReadiness.status == "UP"
# (which means syncPercentage ~ 100 and syncStatus == "Synced").
# Default interval is 120s. Pass an integer to override.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

INTERVAL="${1:-120}"
URL="http://localhost:${LOCAL_API_PORT}/actuator/health/readiness"
start=$(date +%s)

while true; do
  body=$(curl -fsS --max-time 10 "$URL" 2>/dev/null || true)
  if [[ -z "$body" ]]; then
    echo "[$(date '+%H:%M:%S')] readiness endpoint not responding yet"
  else
    status=$(echo "$body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("components",{}).get("onchainReadiness",{}).get("status","UNKNOWN"))' 2>/dev/null || echo "PARSE_ERROR")
    pct=$(echo "$body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("components",{}).get("onchainReadiness",{}).get("details",{}).get("syncPercentage","?"))' 2>/dev/null || echo "?")
    syncStatus=$(echo "$body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("components",{}).get("onchainReadiness",{}).get("details",{}).get("syncStatus",""))' 2>/dev/null || echo "")
    elapsed=$(( $(date +%s) - start ))
    echo "[$(date '+%H:%M:%S')] onchainReadiness=$status syncPercentage=$pct ($syncStatus)  elapsed=${elapsed}s"
    if [[ "$status" == "UP" ]]; then
      echo "[wait-onchain] tip reached after ${elapsed}s"
      exit 0
    fi
  fi
  sleep "$INTERVAL"
done
