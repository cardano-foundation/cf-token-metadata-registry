#!/usr/bin/env bash
# Bring up the docker compose stack on the remote. Always rebuilds the
# API image from the checked-out branch source — we want THIS branch's
# code under test, not whatever published `:latest` happens to be on
# Docker Hub.
#
# `--pull` on build refreshes the base image (FROM line in Dockerfile)
# so we don't carry an outdated JRE / OS layer across runs.

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

ssh_exec() { ssh -o BatchMode=yes "$REMOTE" "$@"; }

echo "[start] verifying $ENV_FILE present in $REMOTE_PROJECT_DIR ..."
if ! ssh_exec "test -f $REMOTE_PROJECT_DIR/$ENV_FILE"; then
  echo "ERROR: $REMOTE_PROJECT_DIR/$ENV_FILE missing on remote." >&2
  exit 1
fi

echo "[start] building API image from source (refreshing base image)..."
ssh_exec "cd $REMOTE_PROJECT_DIR && docker compose --env-file $ENV_FILE build --pull api"

echo "[start] docker compose up -d ..."
ssh_exec "cd $REMOTE_PROJECT_DIR && docker compose --env-file $ENV_FILE up -d"

echo "[start] running containers:"
ssh_exec "cd $REMOTE_PROJECT_DIR && docker compose --env-file $ENV_FILE ps"
