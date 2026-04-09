# ADR-009: Incremental GitHub Sync with Commit Tracking

## Status

Accepted

## Date

2026-03-24

## Context

The `cardano-foundation/cardano-token-registry` GitHub repository contains thousands of token metadata files. Re-processing all files on every sync cycle is wasteful, as only a small number of files typically change between syncs. The sync process needs to be efficient while remaining resilient to failures.

## Decision

We implement incremental synchronization using Git commit hash tracking:

1. **Sync state persistence**: The `off_chain_sync_state` table stores the `last_commit_hash` (varchar 40) of the most recently completed successful sync.

2. **Incremental sync flow**:
   - On each sync cycle, the service pulls the latest changes and compares the current HEAD against the stored commit hash.
   - JGit's `DiffCommand` computes the list of changed files between the two commits.
   - Only changed files are processed (parsed, validated, and upserted).

3. **Full sync fallback**: If no previous commit hash exists (first run) or hash determination fails, a full sync processes all files in the `mappings` directory.

4. **Partial failure handling**: If any individual token fails to process (parse error, validation failure), the sync continues with remaining tokens but does not advance the commit hash. This ensures failed tokens are retried on the next cycle.

5. **Scheduling**: The sync runs as a cron job with an initial delay of 1 minute and a fixed interval of 60 minutes, conditional on `token.metadata.job.enabled=true`.

6. **Sync status tracking**: The service exposes sync state via an enum (`SYNC_NOT_STARTED`, `SYNC_IN_PROGRESS`, `SYNC_DONE`, `SYNC_ERROR`, `SYNC_IN_EXTRA_JOB`) consumed by health indicators and metrics.

## Consequences

### Positive

- **Efficiency**: Only changed files are processed, reducing sync time from minutes (full scan) to seconds for typical runs.
- **Resilience**: Failed tokens don't block the entire sync; they are retried on the next cycle via the non-advancing commit hash.
- **Observability**: Sync status is exposed via health checks and Prometheus metrics, giving operators visibility into sync progress.
- **Idempotent restarts**: After a crash, the application resumes from the last successfully committed hash rather than starting over.

### Negative

- **Stuck on failures**: If a token permanently fails validation, the commit hash never advances past that change, causing repeated retries of all changes in that range. Manual intervention may be needed.
- **Single-row state**: The sync state is a single row, making it difficult to track per-token sync status or support parallel sync workers.
- **Git history dependency**: Incremental sync relies on Git history being available and consistent. Force-pushes or history rewrites on the source repository could break the diff computation.

## Alternatives Considered

- **File modification timestamps**: Use filesystem timestamps to detect changes. Unreliable after clone/checkout operations, which reset timestamps.
- **Content hashing**: Hash each file and compare against stored hashes. More robust than timestamps but requires scanning all files on every cycle, negating the efficiency benefit.
- **GitHub webhooks**: Receive push notifications for real-time sync. Lower latency but requires exposing a public endpoint and handling webhook reliability (retries, deduplication).
- **GitHub API polling**: Use the GitHub Commits API to find changed files. Avoids cloning but introduces API rate limits and external dependency.
