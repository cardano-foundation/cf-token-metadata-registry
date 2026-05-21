#!/usr/bin/env bash
# Report chain sync progress WITHOUT touching /actuator/health (which
# triggers TipFinder and would add to the connection storm if the JVM
# DNS issue is live).
#
# Reads the cursor straight from Postgres on the remote, and pulls the
# current mainnet tip from Koios (free, no key). Computes % and rough ETA.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

cursor_row=$(ssh -o BatchMode=yes "$REMOTE" \
  "docker exec cf-token-metadata-registry-db-1 psql -U cardano -d cf_token_metadata_registry -tA -c 'SELECT slot, block_number, era, update_datetime FROM cursor_ ORDER BY update_datetime DESC LIMIT 1;'")

cursor_slot=$(echo "$cursor_row" | awk -F'|' '{print $1}')
cursor_block=$(echo "$cursor_row" | awk -F'|' '{print $2}')
cursor_era=$(echo "$cursor_row" | awk -F'|' '{print $3}')
cursor_time=$(echo "$cursor_row" | awk -F'|' '{print $4}')

tip=$(curl -fsS --max-time 10 "https://api.koios.rest/api/v1/tip")
tip_slot=$(echo "$tip" | python3 -c 'import json,sys;print(json.load(sys.stdin)[0]["abs_slot"])')
tip_block=$(echo "$tip" | python3 -c 'import json,sys;print(json.load(sys.stdin)[0]["block_no"])')
tip_epoch=$(echo "$tip" | python3 -c 'import json,sys;print(json.load(sys.stdin)[0]["epoch_no"])')

# Slots-behind based purely on what's on-chain right now (no startup-time
# guess), which makes the percent number meaningful and self-correcting.
sync_start_slot="${STORE_CARDANO_SYNC_START_SLOT:-65836843}"
slots_total=$(( tip_slot - sync_start_slot ))
slots_done=$(( cursor_slot - sync_start_slot ))
slots_left=$(( tip_slot - cursor_slot ))
pct=$(python3 -c "print(f'{100*$slots_done/$slots_total:.2f}')")

echo "[sync-progress]"
echo "  cursor:  slot=$cursor_slot  block=$cursor_block  era=$cursor_era  updated=$cursor_time"
echo "  tip:     slot=$tip_slot  block=$tip_block  epoch=$tip_epoch  (Koios mainnet)"
echo "  window:  start=$sync_start_slot  span=$slots_total slots"
echo "  done:    $slots_done slots  (${pct}%)"
echo "  left:    $slots_left slots"
