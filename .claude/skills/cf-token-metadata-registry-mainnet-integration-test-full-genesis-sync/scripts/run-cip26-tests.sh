#!/usr/bin/env bash
# Run CIP-26 regression tests against the API. Uses the COMMITTED fixture
# snapshot under regression-tests/mainnet/fixtures/cip26_tokens.json —
# this is intentional: regenerating fixtures every run would defeat the
# point of a regression test (compare current API behavior to a fixed
# baseline). Assumes wait-offchain-ready.sh has already returned UP.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

PROJECT_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"
REGRESSION_DIR="$PROJECT_ROOT/regression-tests"

if [[ ! -d "$REGRESSION_DIR" ]]; then
  echo "ERROR: $REGRESSION_DIR missing locally — check out the same branch you're testing on this machine." >&2
  exit 1
fi
if [[ ! -s "$REGRESSION_DIR/mainnet/fixtures/cip26_tokens.json" ]]; then
  echo "ERROR: committed CIP-26 fixture missing or empty: $REGRESSION_DIR/mainnet/fixtures/cip26_tokens.json" >&2
  exit 1
fi

cd "$REGRESSION_DIR"

if [[ ! -d ".venv" ]]; then
  echo "[cip26] creating uv venv..."
  uv venv
fi
echo "[cip26] installing requirements..."
uv pip install -r requirements.txt

echo "[cip26] running pytest -m cip26 against http://localhost:${LOCAL_API_PORT} (using committed fixtures)..."
cd mainnet
API_BASE_URL="http://localhost:${LOCAL_API_PORT}" \
  uv run pytest -m cip26 -v --alluredir=../allure-results
