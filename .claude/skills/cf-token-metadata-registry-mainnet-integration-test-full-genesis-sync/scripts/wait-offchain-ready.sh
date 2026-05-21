#!/usr/bin/env bash
# Poll the aggregated /actuator/health endpoint until
# components.offchainSync.status == "UP" (GitHub CIP-26 sync done).
# Default check interval is 60s. Pass an integer (seconds) to override.
#
# NOTE: offchainSync is exposed in the *readiness* group and the aggregated
# /actuator/health endpoint, not in /actuator/health/liveness — the liveness
# group only carries livenessState + onchainConnection. We use the aggregated
# endpoint to keep this future-proof against group reconfiguration.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

INTERVAL="${1:-60}"
URL="http://localhost:${LOCAL_API_PORT}/actuator/health"
start=$(date +%s)

while true; do
  body=$(curl -fsS --max-time 10 "$URL" 2>/dev/null || true)
  if [[ -z "$body" ]]; then
    echo "[$(date '+%H:%M:%S')] liveness endpoint not responding yet"
  else
    offchain=$(echo "$body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("components",{}).get("offchainSync",{}).get("status","UNKNOWN"))' 2>/dev/null || echo "PARSE_ERROR")
    detail=$(echo "$body" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("components",{}).get("offchainSync",{}).get("details",{}).get("syncStatus",""))' 2>/dev/null || echo "")
    elapsed=$(( $(date +%s) - start ))
    echo "[$(date '+%H:%M:%S')] offchainSync=$offchain ($detail)  elapsed=${elapsed}s"
    if [[ "$offchain" == "UP" ]]; then
      echo "[wait-offchain] offchainSync is UP after ${elapsed}s"
      exit 0
    fi
  fi
  sleep "$INTERVAL"
done
