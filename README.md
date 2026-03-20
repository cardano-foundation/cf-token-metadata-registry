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

## How to build

For building from source you need:
- [Apache Maven](https://maven.apache.org/)
- [Java SDK 21+](https://adoptium.net/installation/)
- [Git](https://git-scm.com/)

```console
git clone git@github.com:cardano-foundation/cf-token-metadata-registry.git
cd cf-token-metadata-registry
mvn package
```

> [!NOTE]
> If you change the code, rebuild the local image with `docker compose build` before running `docker compose up`.

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
