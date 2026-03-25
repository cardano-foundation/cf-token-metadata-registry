# ADR-011: Health Check Groups for Kubernetes Probes

## Status

Accepted

## Date

2026-03-24

## Context

The registry runs in Kubernetes, which uses three types of health probes to manage pod lifecycle:

- **Startup probe**: Determines when the application has finished initializing. Until this passes, liveness and readiness probes are not checked.
- **Liveness probe**: Determines if the application is alive. If it fails, Kubernetes restarts the pod.
- **Readiness probe**: Determines if the application can accept traffic. If it fails, the pod is removed from service endpoints.

The registry has a complex startup sequence: Flyway migrations must complete, Yaci Store must connect to a Cardano node and begin receiving blocks, and the GitHub sync must run. These phases have different health semantics and must map to the correct Kubernetes probe type.

## Decision

We configure three Spring Boot Actuator health groups mapped to Kubernetes probe endpoints:

1. **Startup group** (`/actuator/health/startup`):
   - `OnchainConnectionHealthIndicator` (reused): Checks that Yaci Store's block fetcher is initialized, the connection to the Cardano node is alive, and blocks are being received.
   - Database health check
   - Purpose: Prevents liveness checks from killing the pod during the potentially long initial chain sync.

2. **Liveness group** (`/actuator/health/liveness`):
   - Spring Boot's built-in liveness state
   - `OffchainSyncHealthIndicator`: Reports off-chain sync status
   - `OnchainConnectionHealthIndicator`: Checks node connection and block reception only (no sync percentage)
   - Purpose: Detects application deadlocks or unrecoverable failures. Does NOT check sync progress — liveness runs in parallel with readiness, and the pod must not be killed during initial sync. Reuses the same indicator as the startup group since the checks are identical.

3. **Readiness group** (`/actuator/health/readiness`):
   - Spring Boot's built-in readiness state
   - `OffchainSyncHealthIndicator`: Reports off-chain sync status
   - `OnchainReadinessHealthIndicator`: Requires 100% sync to chain tip
   - Database health check
   - Purpose: Only routes traffic to pods that are fully synced to the chain tip and have a healthy database connection.

**Custom health indicators**:

- `OnchainConnectionHealthIndicator`: Reports UP (connected and receiving blocks), OUT_OF_SERVICE (not receiving blocks), or DOWN (error/no connection). Does not check sync progress.
- `OnchainReadinessHealthIndicator`: Reports UP (100% synced), OUT_OF_SERVICE (syncing with percentage), or DOWN (error/no connection). Uses `OnchainSyncStatusService` which requires full sync (100%) to report as synced.
- `OffchainSyncHealthIndicator`: Maps sync states to health: UP (done/extra job), OUT_OF_SERVICE (in progress/not started), DOWN (error).

## Consequences

### Positive

- **Correct pod lifecycle**: Kubernetes won't restart pods during the long initial chain sync (startup probe), won't route traffic before data is ready (readiness), and will restart genuinely stuck pods (liveness).
- **Granular health visibility**: Each probe endpoint returns detailed status with sync percentages and component health, useful for debugging.
- **Operational safety**: Separating startup from liveness prevents a common Kubernetes anti-pattern where slow-starting applications are killed in a restart loop.

### Negative

- **Configuration complexity**: Three health groups with different indicators require careful configuration and documentation.
- **Health indicator maintenance**: Each new component or sync mechanism needs a corresponding health indicator and group assignment.
- **Sync percentage accuracy**: The on-chain sync percentage is approximate (based on slot comparison) and may not accurately reflect actual progress due to varying block density.

## Alternatives Considered

- **Single health endpoint**: One `/health` endpoint for all probes. Simpler but doesn't allow Kubernetes to distinguish between "still starting" and "broken."
- **Custom HTTP endpoints**: Implement health checks outside Spring Actuator. Loses the framework's health aggregation and auto-configuration.
- **Sidecar health checker**: A separate container that checks application health. Adds deployment complexity without clear benefit over Actuator endpoints.
