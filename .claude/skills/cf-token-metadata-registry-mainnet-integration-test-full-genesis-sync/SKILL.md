---
name: cf-token-metadata-registry-mainnet-integration-test-full-genesis-sync
description: QA Automation Engineer — drives a full mainnet integration test of cf-token-metadata-registry on a remote host via SSH. Provisions a fresh Postgres + API stack from genesis using docker compose, monitors /actuator/health, runs CIP-26 regression tests once offchain sync completes, then waits for tip and runs CIP-68 regression tests. Use when the user wants to validate a branch end-to-end against mainnet on a remote box (typed `/cf-token-metadata-registry-mainnet-integration-test-full-genesis-sync` or asked for a "mainnet genesis sync regression run").
---

# Mainnet Full-Genesis-Sync Integration Test (Remote, via SSH)

You are acting as a **QA Automation Engineer**. The user has a remote Linux box reachable over SSH that will:

1. Pull a fresh checkout of `cf-token-metadata-registry` on a target branch.
2. Spin up a clean Postgres + API stack via the project's `docker-compose.yml`, syncing from **genesis** on mainnet.
3. Sync offchain (CIP-26) metadata first → then onchain (CIP-68) reference NFTs as the indexer catches up to tip.

Your job is to drive this remotely. You forward the remote API (port 8080) and Postgres (port 5432) to localhost via SSH so the project's Python regression tests in `regression-tests/` can run on your local machine against the live remote stack.

## Critical rules

- **Never blow away an existing instance silently.** If the remote already has a stack running, the postgres volume populated, or the target ports occupied, **STOP** and report the situation to the user. Ask whether to clean up or abort. Don't `docker compose down -v` on your own.
- **Fresh genesis means empty data.** A genesis run requires the Postgres volume to be empty. If the project has a named volume with data, surface this and let the user decide.
- **Don't run regression tests too early.** CIP-26 tests need `offchainSync == UP`. CIP-68 tests need the indexer at tip (`onchainReadiness == UP`, syncPercentage ≈ 100). Running them earlier produces meaningless failures.
- **Genesis sync is long.** Mainnet sync from slot 65836843 (current `.env` start) can take hours; from true genesis (slot 0), days. Set realistic expectations with the user before kicking off, and prefer long fallback waits (1200–1800s) when polling.

## Required configuration

Load config from (in priority order):

1. CLI args passed to the skill: `--ssh-host`, `--ssh-user`, `--remote-dir`, `--branch`, `--env-file` (`.env` or `.env.preprod`).
2. `./.claude/skills/cf-token-metadata-registry-mainnet-integration-test-full-genesis-sync/config.env` (gitignored — not committed).
3. Defaults.

| Variable | Default | Required? |
|---|---|---|
| `REMOTE_SSH_HOST` | none | **yes** (can be a host or an ssh_config alias) |
| `REMOTE_SSH_USER` | none | no — leave empty when `REMOTE_SSH_HOST` is an ssh_config alias whose `User` directive already sets the login |
| `REMOTE_PROJECT_DIR` | `$HOME/git/cf-token-metadata-registry` | no |
| `BRANCH` | `develop` | no |
| `GIT_REPO_URL` | `git@github.com:cardano-foundation/cf-token-metadata-registry.git` | no |
| `ENV_FILE` | `.env` (mainnet) — `.env.preprod` for preprod | no |
| `LOCAL_API_PORT` | `8080` | no |
| `LOCAL_DB_PORT` | `5432` | no |

If `REMOTE_SSH_HOST` is missing, refuse to start and tell the user to either supply it as an arg or fill in `config.env`. **Never invent or default a hostname.** `REMOTE_SSH_USER` is optional — when the host is an ssh_config alias (e.g. an entry named `mczeladka` with its own `User devbox` directive), leave the user empty so the alias takes effect.

A template lives at `config.env.example`. Tell the user to `cp config.env.example config.env` and fill it in; the real `config.env` is gitignored.

## Procedure

You orchestrate via the helper scripts under `./scripts/`. They are intentionally small and atomic so each step is auditable. Source `scripts/load-config.sh` first to populate environment variables, then call the steps in order.

### Step 0 — Confirm intent with the user

Before any remote action:

- Echo back the resolved config: host, user, remote dir, branch, env file.
- Ask the user to confirm you may proceed. Genesis sync is destructive to existing data on the remote.

### Step 1 — Preflight check (read-only on remote)

Run `scripts/preflight-remote.sh`. It SSHs to the remote and checks:

- Is a docker compose stack currently up in `$REMOTE_PROJECT_DIR`? (`docker compose ps --status running` → non-empty)
- Are the target ports already bound on the remote? (`ss -ltn` for `:8080` and `:5432`)
- Does `$REMOTE_PROJECT_DIR/.git` exist? (informational, not blocking)
- Is the Docker daemon reachable for this user?

If anything is occupied → **STOP**. Print the findings, and ask:

> "Found an existing instance / occupied ports on `$REMOTE_SSH_HOST`. Options: (a) abort, (b) stop & wipe the existing stack (`docker compose down -v` in `$REMOTE_PROJECT_DIR`) then continue. What do you want me to do?"

Only proceed to step 2 with explicit user approval.

### Step 2 — Prepare repo on remote

Run `scripts/prepare-remote.sh`. It:

- If `$REMOTE_PROJECT_DIR/.git` exists → `git fetch --all --prune`, `git reset --hard`, `git clean -fdx`, `git checkout $BRANCH`, `git pull --ff-only origin $BRANCH`.
- Else → `git clone $GIT_REPO_URL $REMOTE_PROJECT_DIR`, then checkout `$BRANCH`.
- Prints the resolved commit SHA so the user can verify what's being tested.

### Step 3 — Start docker compose stack

Run `scripts/start-remote.sh`. It:

- Confirms the chosen `ENV_FILE` is present in `$REMOTE_PROJECT_DIR`.
- Runs `docker compose --env-file $ENV_FILE pull` (image refresh).
- Runs `docker compose --env-file $ENV_FILE build` (in case the branch needs a local build of the API image).
- Runs `docker compose --env-file $ENV_FILE up -d`.
- Prints the running containers and their ports.

If the project uses a named Postgres volume and it already has data, **don't auto-wipe**. Surface this to the user and ask first.

### Step 4 — Open SSH tunnels

Run `scripts/open-tunnels.sh`. It opens an SSH connection in the background with:

- `-L $LOCAL_API_PORT:localhost:8080`
- `-L $LOCAL_DB_PORT:localhost:5432`

…and stores the PID in `./.claude/skills/.../tunnels.pid` so it can be closed cleanly later. Verify the tunnels work by hitting `http://localhost:$LOCAL_API_PORT/actuator/health` once.

### Step 5 — Wait for CIP-26 sync (offchain)

Run `scripts/wait-offchain-ready.sh`. It polls `http://localhost:$LOCAL_API_PORT/actuator/health/liveness` every ~30s and waits until `components.offchainSync.status == "UP"`. Use the `ScheduleWakeup` tool with **1200–1800s** between polls if you're driving this directly — offchain sync from genesis typically takes 10–30 minutes; polling more often wastes cache TTL for no benefit.

While polling, print the current status periodically so the user sees progress.

### Step 6 — Run CIP-26 regression tests

Once offchain is UP, the `metadata` and `logo` tables are populated. Run `scripts/run-cip26-tests.sh` which:

1. `cd regression-tests && uv sync` (local — resolves dependencies from `pyproject.toml` + `uv.lock`).
2. Runs `cd mainnet && API_BASE_URL=http://localhost:$LOCAL_API_PORT uv run pytest -m cip26 -v --alluredir=../allure-results`.

**Do not regenerate fixtures.** The committed `regression-tests/mainnet/fixtures/cip26_tokens.json` is the frozen regression baseline (1000 tokens). Regenerating from the live DB on every run would turn this into a self-confirming smoke test rather than a regression test. Fixtures are refreshed only as a deliberate, separate operation.

If any CIP-26 test fails → report it to the user immediately (don't wait for CIP-68 to finish).

### Step 7 — Wait for tip (onchain sync complete)

Run `scripts/wait-onchain-tip.sh`. It polls `http://localhost:$LOCAL_API_PORT/actuator/health/readiness` until `components.onchainReadiness.status == "UP"` (which means `syncPercentage` ≈ 100 and `syncStatus == "Synced"`).

From slot 65836843 to tip is hours; from genesis it's days. Use `ScheduleWakeup` with the maximum allowed delay (3600s) for this wait, and warn the user that the loop will run for a long time.

### Step 8 — Run CIP-68 regression tests

When the indexer reaches tip, the `metadata_reference_nft` table is populated. Run `scripts/run-cip68-tests.sh`, which runs `uv run pytest -m cip68 -v --alluredir=../allure-results` against the committed `regression-tests/mainnet/fixtures/cip68_tokens.json` snapshot.

**Same fixture rule as CIP-26: do not regenerate.** The committed snapshot is the regression baseline.

### Step 9 — Report

Summarize for the user:

- Branch + commit tested.
- Time spent: offchain sync, onchain sync, total.
- CIP-26: pass/fail counts + first 5 failures.
- CIP-68: pass/fail counts + first 5 failures.
- Where the Allure results are (`regression-tests/allure-results/`); how to view them (`allure serve regression-tests/allure-results`).
- Remind the user that the docker compose stack is **still running** on the remote so they can poke at it; ask if you should `docker compose down -v` and remove the SSH tunnels, or leave it up for further inspection.

## Handling failures

- **SSH connection lost mid-run.** Re-open tunnels and resume polling. Don't restart the stack.
- **Stack restarts mid-sync.** `STORE_SYNCAUTOSTART=true` is set in `.env`, so the indexer resumes automatically. Just keep polling.
- **A CIP-26 test fails before tip is reached.** Report it. Continue waiting for tip in parallel — it's perfectly fine to surface CIP-26 problems before CIP-68 testing starts.
- **Stuck health check (no UP after a long time).** Pull `docker compose logs --tail=200 api` and `docker compose logs --tail=200 db` remotely and surface the first ERROR line to the user.

## Files this skill owns

```
.claude/skills/cf-token-metadata-registry-mainnet-integration-test-full-genesis-sync/
├── SKILL.md                       # this file
├── config.env.example             # template, committed
├── config.env                     # actual config, gitignored
├── tunnels.pid                    # PID of background SSH tunnel (runtime, gitignored)
└── scripts/
    ├── load-config.sh             # config loader
    ├── preflight-remote.sh        # remote occupancy check
    ├── prepare-remote.sh          # clone / reset / checkout
    ├── start-remote.sh            # docker compose up
    ├── open-tunnels.sh            # SSH port-forward
    ├── close-tunnels.sh           # kill background SSH tunnel
    ├── wait-offchain-ready.sh     # poll /actuator/health/liveness
    ├── wait-onchain-tip.sh        # poll /actuator/health/readiness
    ├── run-cip26-tests.sh         # generate fixtures + pytest -m cip26
    └── run-cip68-tests.sh         # generate fixtures + pytest -m cip68
```

All scripts are idempotent and safe to re-run. They print clear progress lines and exit non-zero on failure.
