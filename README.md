[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/main/LICENSE)
![GitHub top language](https://img.shields.io/github/languages/top/cardano-foundation/cf-token-metadata-registry)
[![Build](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml/badge.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml)
[![Issues](https://img.shields.io/github/issues/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry/issues)

---

# Cardano offchain metadata registry

A reference implementation of a Cardano [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry with [CIP-68](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0068) and [CIP-113](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0113) support.

## Overview

The registry is a Spring Boot application backed by PostgreSQL. It serves token metadata from three sources:

- **CIP-26 (offchain)**: metadata is synced from a [GitHub repository](https://github.com/cardano-foundation/cardano-token-registry) on a scheduled basis (every 60 minutes by default).
- **CIP-68 (on-chain)**: metadata is read directly from the Cardano blockchain via [Yaci Store](https://github.com/bloxbean/yaci-store), which connects to a Cardano node and indexes CIP-68 reference NFT datums.
- **CIP-113 (programmable tokens)**: registry node NFTs for programmable tokens are indexed from the chain and surfaced as extensions on the V2 API. Supported on preview, preprod, and mainnet — enabled per-network by setting `CIP113_REGISTRY_NFT_POLICY_IDS`.

The V2 API queries CIP-26 and CIP-68 by priority (`CIP_68,CIP_26` by default). CIP-68 on-chain data takes precedence when available. CIP-113 extensions are appended when the token's policy ID is in the configured programmable token registry.

> [!NOTE]
> By default, Yaci Store connects to a public Cardano node (`STORE_CARDANO_HOST` in `.env`). You can point it to your own node if preferred.

See the [API Reference](https://cardano-foundation.github.io/cf-token-metadata-registry/) for the full API definition.

## Quick Start

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- 2+ CPU cores, 4GB RAM, 10GB storage

### Mainnet

Syncs CIP-26 offchain metadata from GitHub and CIP-68 on-chain metadata from a public Cardano mainnet node. CIP-113 indexing is available but no registry NFT policy IDs are configured by default in [`.env`](./.env) — set `CIP113_REGISTRY_NFT_POLICY_IDS` to enable.

```console
docker compose up
```

### Preprod

Syncs CIP-26 metadata from the [testnet registry](https://github.com/input-output-hk/metadata-registry-testnet), CIP-68 on-chain metadata, and CIP-113 programmable token registry nodes from a public preprod node.

```console
docker compose --env-file .env.preprod up
```

### Preview

Syncs CIP-68 on-chain metadata and CIP-113 programmable token registry nodes from the preview testnet. CIP-26 offchain sync is disabled since no offchain registry exists for preview.

```console
docker compose --env-file .env.preview up
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

All settings are controlled via environment variables. See [`.env`](./.env) (mainnet), [`.env.preprod`](./.env.preprod) (preprod), and [`.env.preview`](./.env.preview) (preview) for the full list.

| Variable | Description | Default |
|----------|-------------|---------|
| `TOKEN_METADATA_SYNC_JOB` | Enable scheduled GitHub sync | `true` |
| `CIP_QUERY_PRIORITY` | CIP priority order for V2 queries | `CIP_68,CIP_26` |
| `STORE_CARDANO_HOST` | Cardano node host for CIP-68 sync | `backbone.mainnet.cardanofoundation.org` |
| `STORE_CARDANO_PROTOCOL_MAGIC` | Network protocol magic | `764824073` (mainnet) |
| `CIP113_REGISTRY_NFT_POLICY_IDS` | Comma-separated CIP-113 registry NFT policy IDs (enables CIP-113 when non-empty) | _(empty)_ |
| `API_DOCKERFILE` | Dockerfile variant for `docker compose build` | `api/Dockerfile.jvm` |

## Docker Images

Two Docker image variants are available:

| Variant | Base image | Startup | Memory | Image size | Use case |
|---------|-----------|---------|--------|------------|----------|
| **JVM** | Eclipse Temurin 25 LTS | ~15s | ~2 GB | ~637 MB | Production, development |
| **Native** | GraalVM 25 LTS (AOT-compiled) | ~3s | ~150 MB | ~200 MB | Experimental |

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

# Preview (CIP-113 programmable tokens)
docker compose --env-file .env.preview up -d

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
- [x] CIP-113 programmable token registry support (preview, preprod, mainnet — enabled via `CIP113_REGISTRY_NFT_POLICY_IDS`)
- [x] Prometheus metrics (`/actuator/prometheus`)
- [x] Kubernetes / Helm deployment support (`deploy/`)

## End-to-End Tests

Python-based regression tests validate all V1 and V2 business endpoints against database snapshots.

```console
cd tests
python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt
python end2end/mainnet/fixtures/generate_fixtures.py
cd end2end/mainnet && python -m pytest -v
```

See [`tests/README.md`](./tests/README.md) for full details on fixture generation, test markers, Allure reports, and configuration options.

## Contributing

File an issue or a PR or reach out directly to us if you want to contribute.

When contributing to this project and interacting with others, please follow our [Contributing Guidelines](./CONTRIBUTING.md) and [Code of Conduct](./CODE-OF-CONDUCT.md).

---

Thanks for visiting and enjoy :heart:!
