[![License](https://img.shields.io/github/license/cardano-foundation/cf-token-metadata-registry?label=license)](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/main/LICENSE)
[![CI](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/ci.yaml)
[![Nightly](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/nightly.yaml/badge.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/nightly.yaml)
[![CodeQL](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/codeql.yml)
[![SonarCloud Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=cardano-foundation_cf-token-metadata-registry&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=cardano-foundation_cf-token-metadata-registry)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=cardano-foundation_cf-token-metadata-registry&metric=coverage)](https://sonarcloud.io/summary/new_code?id=cardano-foundation_cf-token-metadata-registry)
[![GitHub top language](https://img.shields.io/github/languages/top/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry)
[![Issues](https://img.shields.io/github/issues/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry/issues)
[![Discord](https://img.shields.io/discord/1022471509173882950?label=chat&logo=discord)](https://discord.gg/cardano)

---

# Cardano offchain metadata registry

A reference implementation of a Cardano [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry with [CIP-68](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0068) support.

## Overview

The registry is a Spring Boot application backed by PostgreSQL. It serves token metadata from two sources:

- **CIP-26 (offchain)**: metadata is synced from a [GitHub repository](https://github.com/cardano-foundation/cardano-token-registry) on a scheduled basis (every 60 minutes by default).
- **CIP-68 (on-chain)**: metadata is read directly from the Cardano blockchain via [Yaci Store](https://github.com/bloxbean/yaci-store), which connects to a Cardano node and indexes CIP-68 reference NFT datums.

The V2 API queries both standards by priority (`CIP_68,CIP_26` by default). CIP-68 on-chain data takes precedence when available.

> [!NOTE]
> By default, Yaci Store connects to a public Cardano node (`STORE_CARDANO_HOST` in `.env`). You can point it to your own node if preferred.

See the [API Reference](https://cardano-foundation.github.io/cf-token-metadata-registry/) for the full API definition.

## Quick Start

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- 2+ CPU cores, 4GB RAM, 10GB storage

### Mainnet

```console
docker compose up
```

### Preprod

```console
docker compose --env-file .env.preprod up
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v2/subjects/{subject}` | Token metadata with CIP priority selection |
| POST | `/api/v2/subjects/query` | Batch query with priority and detail options |

For the full API reference (including V1 endpoints and query parameters), see the [API Reference](https://cardano-foundation.github.io/cf-token-metadata-registry/).

## Operational Endpoints

| Method | Path | Description | Kubernetes Probe |
|--------|------|-------------|------------------|
| GET | `/actuator/health` | Aggregated health status with details for all indicators | — |
| GET | `/actuator/health/startup` | Checks database connectivity and Cardano node connection | Startup |
| GET | `/actuator/health/liveness` | Checks offchain sync status and Cardano node connection | Liveness |
| GET | `/actuator/health/readiness` | Checks offchain sync, on-chain sync progress (100%), and database | Readiness |
| GET | `/actuator/info` | Application info | — |
| GET | `/actuator/prometheus` | Prometheus metrics (Micrometer) | — |
| GET | `/actuator/metrics` | Micrometer metrics listing and details | — |
| GET | `/health` | **Deprecated** — legacy sync status endpoint, use `/actuator/health/readiness` instead | — |

## Configuration

All settings are controlled via environment variables. See [`.env`](./.env) (mainnet) and [`.env.preprod`](./.env.preprod) (preprod) for the full list.

| Variable | Description | Default |
|----------|-------------|---------|
| `TOKEN_METADATA_SYNC_JOB` | Enable scheduled GitHub sync | `true` |
| `CIP_QUERY_PRIORITY` | CIP priority order for V2 queries | `CIP_68,CIP_26` |
| `STORE_CARDANO_HOST` | Cardano node host for CIP-68 sync | `backbone.mainnet.cardanofoundation.org` |
| `STORE_CARDANO_PROTOCOL_MAGIC` | Network protocol magic | `764824073` (mainnet) |
| `API_DOCKERFILE` | Dockerfile variant for `docker compose build` | `api/Dockerfile.jvm` |

## Docker Images

Two Docker image variants are available. **Both are fully supported for production from v1.5.1 onwards.**

| Variant | Base image | Startup | Memory | Image size | Use case |
|---------|-----------|---------|--------|------------|----------|
| **JVM** | Eclipse Temurin 25 LTS | ~11 s | ~2.4 GiB | ~400 MB | Production, development |
| **Native** | GraalVM 25 LTS (AOT-compiled) | ~1.8 s | ~740 MiB | ~584 MB | Production (low-memory / fast-start) |

Observed figures above are from long-running mainnet deploys on the `CIP-113-token` branch — startup measured from container start to Spring Boot's `Started … in` log; memory measured as Docker RSS ~1 h into mainnet sync; image size measured on a fresh `docker inspect`. Your numbers will differ slightly depending on JVM tuning, GraalVM profile-guided optimisation, and base image. Native image sync throughput sits within ~5 % of the JVM build on the same hardware.

> [!NOTE]
> Native images were experimental in `1.5.0`; they became a first-class, supported production target in `1.5.1`. The main bug that had previously blocked offchain CIP-26 sync on native (JGit enum reflection — see [PR #79](https://github.com/cardano-foundation/cf-token-metadata-registry/pull/79)) was fixed for `1.5.1`.

### Building the JVM image

The default `docker compose build` builds a JVM image:

```console
docker compose build
```

Or build it directly:

```console
docker build -t cardanofoundation/cf-token-metadata-registry-api:latest -f api/Dockerfile.jvm .
```

### Building the Native image (GraalVM)

The native image compiles the application ahead-of-time into a standalone binary. No JVM is needed at runtime.

```console
docker build -t cardanofoundation/cf-token-metadata-registry-api:latest -f api/Dockerfile.native .
```

> [!NOTE]
> The native image build takes 10-15 minutes and requires 8+ GB of RAM during compilation.

> [!NOTE]
> The native image is **built** with [Oracle GraalVM](https://www.oracle.com/java/graalvm/) under the [GraalVM Free Terms and Conditions (GFTC)](https://www.oracle.com/downloads/licenses/graal-free-license.html). Oracle GraalVM is free for development and production use, but redistribution of the GraalVM SDK itself is restricted. This does not affect the final Docker image or binary — the multi-stage build ensures only the compiled native binary (not the GraalVM toolchain) is included in the runtime image. Users who build from source need to accept the GFTC to pull the Oracle GraalVM build image.

### Running locally with Docker Compose

By default, `docker compose` uses the JVM Dockerfile. To use the native image instead, set `API_DOCKERFILE`:

```console
# Mainnet (JVM, default) — full sync mode
docker compose up -d

# Mainnet (native image)
API_DOCKERFILE=api/Dockerfile.native docker compose up -d --build

# Preprod
docker compose --env-file .env.preprod up -d

# Read-only mode (no sync, no node connection)
COMPOSE_PROFILES=ro docker compose up -d
```

> [!NOTE]
> The `.env` file sets `COMPOSE_PROFILES=rw` by default, which starts the full read-write API. Two profiles are available:
> - **`rw`** (default) — full API with CIP-26 GitHub sync and CIP-68 on-chain indexing
> - **`ro`** — read-only API on port `8081` (configurable via `API_RO_LOCAL_BIND_PORT`) that serves queries from the existing database without connecting to a Cardano node or syncing metadata

To start fresh (wipe database and resync from scratch):

```console
docker compose down -v
docker compose up -d
```

### Verifying the deployment

Once started, the API syncs both offchain (GitHub) and on-chain (Cardano node) metadata. Check the health endpoints:

```console
# Startup probe — is the app initialized?
curl http://localhost:8080/actuator/health/startup

# Readiness probe — is the app ready to serve traffic?
curl http://localhost:8080/actuator/health/readiness

# Liveness probe — is the app still healthy?
curl http://localhost:8080/actuator/health/liveness

# Prometheus metrics (sync progress, token counts)
curl http://localhost:8080/actuator/prometheus | grep cftr_
```

Key metrics to watch during sync:
- `cftr_sync_status` — offchain sync: `0`=not started, `1`=in progress, `2`=done
- `cftr_tokens_cip26_count` — number of CIP-26 tokens loaded from GitHub
- `cftr_tokens_cip68_count` — number of CIP-68 tokens indexed from chain
- `yaci_store_current_block` — current block being processed

> [!TIP]
> A full mainnet sync from genesis takes approximately 15 hours. The readiness probe will report `OUT_OF_SERVICE` until the on-chain sync reaches 98%.

## How to build from source

For building from source you need:
- [Apache Maven](https://maven.apache.org/)
- [Java SDK 25 LTS](https://adoptium.net/installation/) (e.g. Amazon Corretto or Eclipse Temurin)
- [Git](https://git-scm.com/)

```console
git clone git@github.com:cardano-foundation/cf-token-metadata-registry.git
cd cf-token-metadata-registry
mvn clean package -DskipTests
```

### Building a native binary locally

To build a native binary without Docker, you need [GraalVM 25 LTS](https://www.graalvm.org/downloads/):

```console
# Using SDKMAN
sdk install java 25.0.2-graal
sdk use java 25.0.2-graal

# Build the native binary
mvn clean package -pl api,common -am -DskipTests -Pnative

# The binary is at api/target/api
```

## Features

- [x] CIP-26 compliant REST API
- [x] CIP-68 fungible token support (V2 API with priority-based querying)
- [x] Prometheus metrics (`/actuator/prometheus`)
- [x] Kubernetes / Helm deployment support (`deploy/`)
- [x] GraalVM native-image builds — production-supported from `1.5.1`

## End-to-End Tests

Python-based regression tests validate all V1 and V2 business endpoints against database snapshots.

Managed with [uv](https://docs.astral.sh/uv/) (`brew install uv`).

```console
cd regression-tests
uv venv && uv pip install -r requirements.txt
uv run python mainnet/fixtures/generate_fixtures.py
cd mainnet && uv run pytest -v
```

See [`regression-tests/README.md`](./regression-tests/README.md) for full details on fixture generation, test markers, Allure reports, and configuration options.

## Contributing

File an issue or a PR or reach out directly to us if you want to contribute.

When contributing to this project and interacting with others, please follow our [Contributing Guidelines](./CONTRIBUTING.md) and [Code of Conduct](./CODE-OF-CONDUCT.md).

---

Thanks for visiting and enjoy :heart:!
