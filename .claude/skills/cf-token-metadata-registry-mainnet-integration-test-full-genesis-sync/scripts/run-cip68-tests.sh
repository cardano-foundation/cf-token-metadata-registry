#!/usr/bin/env bash
# Run CIP-68 regression tests against the API. Uses the COMMITTED fixture
# snapshots under regression-tests/mainnet/fixtures/ — do NOT regenerate:
#   cip68_tokens.json          CIP-68 fungible tokens (333), DB snapshot
#   cip68_validation_errors_tokens.json  tokens served with known validation errors (ADR-019)
#   cip68_rft_tokens.json      CIP-68 rich fungible tokens (444), chain snapshot via Koios
# Assumes wait-onchain-tip.sh (or monitor-sync-progress.sh --until-tip) has already returned.
#
# Usage: run-cip68-tests.sh [pytest marker expression]   (default: "cip68 or validation": FTs, RFTs and the ADR-019 contract)
#   run-cip68-tests.sh rft                    RFT (444) tests only
#   run-cip68-tests.sh "cip68 and not rft"    for a build without RFT support (before PR #110 / ADR-018)
#   run-cip68-tests.sh "cip68 and not validation"   for a build or database without validation errors (before ADR-019)

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

MARKERS="${1:-cip68 or validation}"
PROJECT_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"
REGRESSION_DIR="$PROJECT_ROOT/regression-tests"
FIXTURES_DIR="$REGRESSION_DIR/mainnet/fixtures"

for fixture in cip68_tokens.json cip68_rft_tokens.json; do
  if [[ ! -s "$FIXTURES_DIR/$fixture" ]]; then
    echo "ERROR: committed CIP-68 fixture missing or empty: $FIXTURES_DIR/$fixture" >&2
    exit 1
  fi
done

cd "$REGRESSION_DIR"
echo "[cip68] running pytest -m \"$MARKERS\" against http://localhost:${LOCAL_API_PORT} (using committed fixtures)..."
cd mainnet
API_BASE_URL="http://localhost:${LOCAL_API_PORT}" \
  uv run pytest -m "$MARKERS" -v --alluredir=../allure-results
