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
   - `StartupHealthIndicator`: Checks that Yaci Store's block fetcher is initialized and the connection to the Cardano node is alive.
   - Purpose: Prevents liveness checks from killing the pod during the potentially long initial chain sync.

2. **Liveness group** (`/actuator/health/liveness`):
   - Spring Boot's built-in liveness state
   - `OffchainSyncHealthIndicator`: Reports off-chain sync status
   - `OnchainSyncHealthIndicator`: Reports on-chain sync status with sync percentage
   - Purpose: Detects application deadlocks or unrecoverable failures.

3. **Readiness group** (`/actuator/health/readiness`):
   - Spring Boot's built-in readiness state
   - Both sync health indicators
   - Database health check
   - Purpose: Only routes traffic to pods that have completed initial sync and have a healthy database connection.

**Custom health indicators**:

- `OnchainSyncHealthIndicator`: Reports UP (synced), OUT_OF_SERVICE (syncing with percentage), or DOWN (error/no connection). Uses `OnchainSyncStatusService` which caches the network tip with adaptive refresh intervals.
- `OffchainSyncHealthIndicator`: Maps sync states to health: UP (done/extra job), OUT_OF_SERVICE (in progress/not started), DOWN (error).
- `StartupHealthIndicator`: Checks Yaci Store's `BlockFetcherInitialized` and `ConnectionAlive` flags.

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
