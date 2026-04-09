# ADR-001: Multi-Module Maven Project Structure

## Status

Accepted

## Date

2026-03-24

## Context

The CF Token Metadata Registry serves multiple distinct functions: exposing a REST API for metadata queries, running background synchronization jobs against GitHub, providing CLI tooling for metadata operations, and sharing domain models across these concerns. A single-module project would conflate these responsibilities, making it difficult to deploy, test, and evolve them independently.

## Decision

We structure the project as a multi-module Maven build with the following modules:

- **api**: Spring Boot web application serving the REST API, integrating Yaci Store for on-chain data, and exposing health/metrics endpoints.
- **job**: Standalone Spring Boot `CommandLineRunner` for batch metadata synchronization from GitHub. Can be deployed and scheduled independently.
- **common**: Shared domain models (JPA entities), repositories, services (e.g., `TokenMetadataSyncService`, `GitService`), and utilities. Has no Spring Boot starter of its own.
- **cli**: Command-line tool using Apache Commons CLI for metadata operations (init, validate). Runs as a non-web Spring Boot application.
- **integration-test**: End-to-end tests that require a running API and database.

The root `pom.xml` defines shared dependency versions, plugin configurations, and the module ordering. Each module declares only the dependencies it needs.

## Consequences

### Positive

- **Independent deployment**: The API, job, and CLI can be built and deployed as separate artifacts with different lifecycles.
- **Clear dependency direction**: `common` has no upward dependencies; `api` and `job` depend on `common` but not on each other.
- **Smaller artifacts**: Each deployable JAR contains only its required dependencies, reducing image size and startup time.
- **Focused testing**: Unit tests run per-module; integration tests are isolated in their own module and only executed when infrastructure is available.

### Negative

- **Build complexity**: Multi-module Maven builds require careful dependency management and can slow down full rebuilds.
- **Shared code governance**: Changes to `common` affect all downstream modules, requiring cross-module testing.
- **Version coordination**: All modules share the same version (`${revision}` property), making independent versioning impossible without restructuring.

## Alternatives Considered

- **Monolithic single-module**: Simpler build but would force deploying unused code (e.g., CLI dependencies in the API container) and complicate separation of concerns.
- **Gradle build**: Offers better incremental builds and build caching, but Maven is the established choice in the Cardano Foundation Java ecosystem and aligns with team expertise.
- **Separate repositories per module**: Maximum independence but introduces coordination overhead for shared domain changes and complicates atomic cross-module updates.
