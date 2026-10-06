#!/usr/bin/env bash
# Run CIP-68 regression tests against the API. Uses the COMMITTED fixture
# snapshot under regression-tests/mainnet/fixtures/cip68_tokens.json —
# do NOT regenerate. Assumes wait-onchain-tip.sh has already returned.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

PROJECT_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"
REGRESSION_DIR="$PROJECT_ROOT/regression-tests"

if [[ ! -s "$REGRESSION_DIR/mainnet/fixtures/cip68_tokens.json" ]]; then
  echo "ERROR: committed CIP-68 fixture missing or empty: $REGRESSION_DIR/mainnet/fixtures/cip68_tokens.json" >&2
  exit 1
fi

cd "$REGRESSION_DIR"
echo "[cip68] running pytest -m cip68 against http://localhost:${LOCAL_API_PORT} (using committed fixtures)..."
cd mainnet
API_BASE_URL="http://localhost:${LOCAL_API_PORT}" \
  uv run pytest -m cip68 -v --alluredir=../allure-results
