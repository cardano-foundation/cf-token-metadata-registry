---
title: Run your own registry
description: Run an instance with Docker, for mainnet or a test network.
---

The registry is a single service with a PostgreSQL database. It indexes the chain itself, from a Cardano node, and
syncs the CIP-26 registry from GitHub.

## With Docker Compose

```bash
git clone https://github.com/cardano-foundation/cf-token-metadata-registry
cd cf-token-metadata-registry
docker compose up -d                                  # mainnet
docker compose --env-file .env.preprod up -d          # preprod
```

The API listens on the port in `API_LOCAL_BIND_PORT`. The first sync indexes the chain from the start point in the
`.env` file, which takes hours on mainnet; `/actuator/health/readiness` turns `UP` when it is done.

## Images

| Image | Use |
|---|---|
| `cardanofoundation/cf-token-metadata-registry-api:<version>` | JVM |
| `cardanofoundation/cf-token-metadata-registry-api:<version>-native` | GraalVM native image: faster start, less memory |

## Read-only instances

To scale reads, run extra instances that serve the same database without indexing: the `ro` Compose profile
(`COMPOSE_PROFILES=ro docker compose up -d`) starts one with `STORE_READONLYMODE=true` and the `readonly` Spring profile.

## Main settings

| Variable | Meaning |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | the PostgreSQL database |
| `STORE_CARDANO_HOST`, `STORE_CARDANO_PORT`, `STORE_CARDANO_PROTOCOL_MAGIC` | the Cardano node to index from |
| `STORE_CARDANO_SYNC_START_SLOT`, `STORE_CARDANO_SYNC_START_BLOCKHASH` | where indexing starts |
| `TOKEN_METADATA_SYNC_JOB` | sync the CIP-26 registry in this instance (`true`), or leave it to a separate job |
| `GITHUB_ORGANIZATION`, `GITHUB_PROJECT_NAME` | the CIP-26 registry repository |
| `CIP113_REGISTRY_NFT_POLICY_IDS` | the CIP-113 registry to index; empty switches CIP-113 off |
| `REGISTRY_VALIDATE_ENABLED` | the validate endpoint, on by default |
| `REGISTRY_VALIDATE_CORS_ALLOWED_ORIGINS` | sites allowed to call the validate endpoint from a browser, e.g. `https://cardano-foundation.github.io` |

The full list, and the design decisions behind the registry, are in the
[repository](https://github.com/cardano-foundation/cf-token-metadata-registry).
