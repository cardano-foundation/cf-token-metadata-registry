[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/main/LICENSE)
![GitHub top language](https://img.shields.io/github/languages/top/cardano-foundation/cf-token-metadata-registry)
[![Build](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml/badge.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml)
[![Issues](https://img.shields.io/github/issues/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry/issues)

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
| GET | `/health` | Sync status (`SYNC_NOT_STARTED`, `SYNC_IN_PROGRESS`, `SYNC_COMPLETED`) |
| GET | `/actuator/health` | Spring Boot liveness check |
| GET | `/actuator/prometheus` | Prometheus metrics |

For the full API reference (including V1 endpoints and query parameters), see the [API Reference](https://cardano-foundation.github.io/cf-token-metadata-registry/).

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

Two Docker image variants are available:

| Variant | Base image | Startup | Memory | Image size | Use case |
|---------|-----------|---------|--------|------------|----------|
| **JVM** | Eclipse Temurin 25 LTS | ~15s | ~2 GB | ~637 MB | Development, debugging |
| **Native** | GraalVM 25 LTS (AOT-compiled) | ~3s | ~150 MB | ~200 MB | Production, Kubernetes |

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
# Mainnet (JVM, default)
docker compose up -d

# Mainnet (native image)
API_DOCKERFILE=api/Dockerfile.native docker compose up -d --build

# Preprod
docker compose --env-file .env.preprod up -d
```

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

## Contributing

File an issue or a PR or reach out directly to us if you want to contribute.

When contributing to this project and interacting with others, please follow our [Contributing Guidelines](./CONTRIBUTING.md) and [Code of Conduct](./CODE-OF-CONDUCT.md).

---

Thanks for visiting and enjoy :heart:!
