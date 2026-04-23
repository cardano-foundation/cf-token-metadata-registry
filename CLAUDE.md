# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

Multi-standard Cardano token metadata registry built with Spring Boot 3 and Java 25. Merges offchain (CIP-26), on-chain (CIP-68), and programmable token (CIP-113) metadata into a unified API with configurable query priority.

### Modules

- **api**: REST API server — serves V1 (CIP-26 only) and V2 (CIP-26 + CIP-68 + CIP-113) endpoints. Embeds Yaci Store for real-time blockchain indexing of CIP-68 reference NFTs and CIP-113 registry nodes
- **job**: Background job that syncs CIP-26 offchain metadata from the GitHub cardano-token-registry
- **common**: Shared JPA entities (`Metadata`, `MetadataReferenceNft`, `Cip113RegistryNode`), repositories, and domain models
- **cli**: Command-line tool for CIP-26 metadata operations (`init`, `entry`, `validate`)
- **integration-test**: End-to-end integration tests run against a live API instance with `RestTemplate`

### Metadata Standards

- **CIP-26** (offchain): JSON metadata files synced from GitHub, stored in `metadata` table
- **CIP-68** (on-chain): Reference NFT datum parsed from blockchain UTxOs (prefix `000643b0`), stored in `metadata_reference_nft` table. Fungible tokens (prefix `0014df10`) are mapped to their reference NFT counterpart
- **CIP-113** (programmable tokens): Registry node NFTs with transfer logic scripts, stored in `cip113_registry_node` table. Enabled when `CIP113_REGISTRY_NFT_POLICY_IDS` is non-empty

### V2 Query Priority

The V2 API merges metadata from CIP-26 and CIP-68 using a priority mechanism:
- Default priority: `CIP_68,CIP_26` (on-chain preferred, configurable via `cip.query.priority`)
- Per-request override via `query_priority` parameter
- First source with valid data (name + description) wins; gaps filled by lower-priority sources
- CIP-113 extensions are appended when the token's policy ID is in the programmable token registry

### On-Chain Indexing (Yaci Store)

Yaci Store 2.0.0 is embedded in the API module for real-time Cardano blockchain sync:
- `CustomUtxoStorage` filters UTxOs — only persists CIP-68 reference NFTs and CIP-113 registry nodes
- Event listeners (`Cip68EventListener`, `Cip113EventListener`) parse datums on new blocks
- Admin UI available at `/admin-ui/` for sync control

## Key Commands

### Build and Run
```bash
# Build entire project
mvn clean package

# Run tests
mvn clean verify

# Run with Docker Compose (includes Postgres DB)
docker compose up

# Rebuild Docker images after code changes
docker compose build
```

### Development
```bash
# Run single test
mvn test -Dtest=TestClassName

# Skip tests during build
mvn clean package -DskipTests

# Check API health
curl http://localhost:8081/actuator/health
```

## Database Management

Database migrations are managed by Flyway and located in:
- `api/src/main/resources/db/migration/postgresql/`
- `api/src/main/resources/db/store/{vendor}/`

Migrations run automatically on application startup when `spring.flyway.enabled=true`.

## Configuration

Main configuration is in `api/src/main/resources/application.properties`. Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/cf_token_metadata_registry` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `cardano` / (empty) |
| `TOKEN_METADATA_SYNC_JOB` | Enable CIP-26 GitHub sync | `false` |
| `GITHUB_ORGANIZATION` | Source GitHub org | `cardano-foundation` |
| `GITHUB_PROJECT_NAME` | Source repo name | `cardano-token-registry` |
| `CIP113_REGISTRY_NFT_POLICY_IDS` | Comma-separated monitored policy IDs (CIP-113 enabled when non-empty) | (empty) |
| `STORE_CARDANO_HOST` | Cardano node hostname | `backbone.mainnet.cardanofoundation.org` |
| `STORE_CARDANO_PORT` | Cardano node port | `3001` |

## API Endpoints

### V1 API (CIP-26 only)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/metadata/{subject}` | All properties for a subject |
| GET | `/metadata/{subject}/properties/{property}` | Single property for a subject |
| POST | `/metadata/query` | Batch query multiple subjects |

### V2 API (CIP-26 + CIP-68 + CIP-113)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v2/subjects/{subject}` | Subject with priority, optional `property`, `query_priority`, `show_cips_details` params |
| POST | `/api/v2/subjects/query` | Batch query with same options |

### Operational Endpoints

| Path | Description |
|------|-------------|
| `/actuator/health` | Aggregated health status |
| `/actuator/health/startup` | Startup probe — includes: `db`, `onchainConnection` |
| `/actuator/health/liveness` | Liveness probe — includes: `livenessState`, `offchainSync`, `onchainConnection` |
| `/actuator/health/readiness` | Readiness probe — includes: `readinessState`, `offchainSync`, `onchainReadiness`, `db` |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/metrics` | Spring Boot metrics |
| `/apidocs` | OpenAPI 3.0 specification (JSON) |
| `/admin-ui/` | Yaci Store admin UI for sync control |
| `/health` | **DEPRECATED** — use `/actuator/health/readiness` instead. Will be removed in a future release |

## Testing Strategy

- Unit tests use JUnit 5, Mockito, and **AssertJ** (`assertThat()`) for fluent assertions
- Use **`@Nested`** inner classes to group tests by concern (e.g. `@Nested class FindByPolicyId`, `@Nested class ContainsRegistryNode`)
- Controller tests use `@WebMvcTest` with `MockMvc` and `@MockBean` for dependencies
- End-to-end integration tests live in `integration-test/` module — they use `RestTemplate` against a running API (no Spring test context)
- Test coverage tracked via JaCoCo, reports in `api/target/site/jacoco/`
- Do not test POJOs/records — focus on real logic (parsing, filtering, query behavior, edge cases)

## Coding Conventions

- **Do not use `var`** — always use explicit types for local variables
- **Annotate nullable returns with `@Nullable`** — use `org.jspecify.annotations.Nullable` on any method that can return null
- **Prefer Java records over tuples/pairs** — use named records for readability instead of generic `Pair<A,B>` or tuple types
