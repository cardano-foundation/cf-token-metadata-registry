#!/usr/bin/env bash
# Source this script; do not execute it.
# Loads config.env (if present) and applies CLI overrides via env variables
# the caller already exported. Exits with a clear error if required values
# are missing.

set -eo pipefail

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$SKILL_DIR/config.env" ]]; then
  # shellcheck disable=SC1091
  set -a
  source "$SKILL_DIR/config.env"
  set +a
fi

: "${REMOTE_PROJECT_DIR:=\$HOME/Develop/cf-token-metadata-registry}"
: "${BRANCH:=develop}"
: "${GIT_REPO_URL:=git@github.com:cardano-foundation/cf-token-metadata-registry.git}"
: "${ENV_FILE:=.env}"
: "${LOCAL_API_PORT:=8080}"
: "${LOCAL_DB_PORT:=5432}"

if [[ -z "${REMOTE_SSH_HOST:-}" ]]; then
  echo "ERROR: required config missing: REMOTE_SSH_HOST" >&2
  echo "Fill it in $SKILL_DIR/config.env (copy from config.env.example) or export it before running." >&2
  return 1 2>/dev/null || exit 1
fi

# REMOTE_SSH_USER is OPTIONAL. If it's empty, we connect by host only — this
# lets ssh_config aliases (e.g. an entry named 'mczeladka' with its own User
# directive) take effect. If it's set, we force user@host.
if [[ -n "${REMOTE_SSH_USER:-}" ]]; then
  REMOTE="${REMOTE_SSH_USER}@${REMOTE_SSH_HOST}"
else
  REMOTE="${REMOTE_SSH_HOST}"
fi

export REMOTE_SSH_HOST REMOTE_SSH_USER REMOTE_PROJECT_DIR BRANCH GIT_REPO_URL ENV_FILE LOCAL_API_PORT LOCAL_DB_PORT
export SKILL_DIR REMOTE

echo "[config] remote = $REMOTE"
echo "[config] dir    = $REMOTE_PROJECT_DIR"
echo "[config] branch = $BRANCH"
echo "[config] env    = $ENV_FILE"
echo "[config] ports  = API:$LOCAL_API_PORT  DB:$LOCAL_DB_PORT"
