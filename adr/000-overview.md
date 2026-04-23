# ADR-000: Architecture Decision Records Overview

## Status

Accepted

## Date

2026-03-24

## Context

The CF Token Metadata Registry is a CIP-26 compliant offchain metadata registry for Cardano tokens, extended with CIP-68 on-chain metadata and CIP-113 programmable token support. As the project has evolved through multiple releases, several significant architectural decisions have been made that shape its design, deployment, and operation.

This document serves as an index and navigation guide to all Architecture Decision Records (ADRs) in this project, providing a high-level map of the key decisions and their relationships.

## Architecture at a Glance

The system is a multi-module Spring Boot 3 application composed of:

- **api** - Thin REST API wrapper over yaci-store's `assets-ext` extension; controllers delegate to `TokenQueryService` and the `Cip26StorageReader` / `Cip68StorageReader` / `Cip113StorageReader` beans
- **job** - Standalone `CommandLineRunner` that triggers assets-ext's CIP-26 sync once and exits; intended to be driven by a Kubernetes CronJob
- **cli** - Command-line tools for metadata operations
- **integration-test** - End-to-end tests against a running API

Data flows into the system from two sources, both owned by assets-ext:
1. **Off-chain**: GitHub repository (`cardano-token-registry`) synced via JGit inside assets-ext
2. **On-chain**: Cardano blockchain indexed via Yaci Store; assets-ext's `Cip68Processor` and `Cip113Processor` consume `AddressUtxoEvent`s and populate their own tables

The API exposes both a legacy V1 (CIP-26 only) and an enhanced V2 (CIP-26 + CIP-68 with configurable priority, plus CIP-113 extensions) interface. See [ADR-017](017-thin-wrapper-over-assets-ext.md) for the current architectural baseline.

## ADR Index

### Foundational

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-017](017-thin-wrapper-over-assets-ext.md) | Refactor to Thin Wrapper over yaci-store assets-ext | **Accepted (current baseline)** |

### Project Structure & Technology

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](001-multi-module-maven-structure.md) | Multi-Module Maven Project Structure | Accepted (updated by ADR-017: `common` module removed) |
| [ADR-002](002-spring-boot-virtual-threads-jdk25.md) | Spring Boot 3 with Virtual Threads on JDK 25 | Accepted |
| [ADR-003](003-graalvm-native-image-support.md) | GraalVM Native Image Support | **Deprecated** (see ADR-017) |

### Data & Standards

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-004](004-cip26-cip68-dual-standard.md) | CIP-26 and CIP-68 Dual Standard Support | Accepted (implementation moved to assets-ext per ADR-017) |
| [ADR-005](005-postgresql-jsonb-metadata.md) | PostgreSQL with JSONB for Extensible Metadata | Accepted (schema owned by assets-ext per ADR-017) |
| [ADR-006](006-flyway-database-migrations.md) | Flyway for Database Schema Management | Accepted (migrations owned by assets-ext per ADR-017) |
| [ADR-015](015-v2-api-extensions-model.md) | V2 API Extensions Model | Accepted (DTOs provided by assets-ext per ADR-017) |
| [ADR-016](016-cip113-programmable-tokens.md) | CIP-113 Programmable Token Support | Accepted (implementation moved to assets-ext per ADR-017) |

### Data Ingestion

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-007](007-yaci-store-onchain-indexing.md) | Yaci Store for On-Chain Data Indexing | **Superseded by ADR-017** |
| [ADR-008](008-jgit-git-operations.md) | JGit for Git Operations | **Superseded by ADR-017** (JGit moved into assets-ext) |
| [ADR-009](009-incremental-github-sync.md) | Incremental GitHub Sync with Commit Tracking | **Superseded by ADR-017** (logic moved to assets-ext) |

### API Design

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-010](010-v2-api-query-priority.md) | V2 API with Query Priority Model | Accepted (implementation in assets-ext's `TokenQueryService` per ADR-017) |

### Operations & Observability

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-011](011-health-check-groups-kubernetes.md) | Health Check Groups for Kubernetes Probes | Accepted (indicator bean IDs updated per ADR-017) |
| [ADR-012](012-prometheus-business-metrics.md) | Prometheus Metrics with Custom Business Counters | Accepted (scope reduced per ADR-017) |
| [ADR-013](013-structured-logging-ecs.md) | Structured Logging with ECS Format | Accepted |

### CI/CD & Release

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-014](014-release-please-versioning.md) | Release-Please for Automated Versioning | Accepted |

## How to Use ADRs

Each ADR follows a consistent format:

- **Status**: Current state (Proposed, Accepted, Deprecated, Superseded)
- **Date**: When the decision was made or last updated
- **Context**: The problem or situation that motivated the decision
- **Decision**: What was decided and why
- **Consequences**: The resulting impact, both positive and negative
- **Alternatives Considered**: Other options that were evaluated

To propose a new ADR, create a new file following the naming convention `NNN-short-title.md` and update this index.
