# ADR-017: Refactor to Thin Wrapper over yaci-store assets-ext

## Status

Accepted

## Date

2026-04-23

## Context

The CF Token Metadata Registry was built before yaci-store's `assets-ext` extension existed. As a result, it independently implemented:

- CIP-26 offchain sync from the cardano-foundation/cardano-token-registry GitHub repo (JGit-based clone, incremental diff, mapping parser, validation)
- CIP-68 reference NFT indexing (filtered UTxO storage, event listener, datum parser, fungible-token-subject projection)
- CIP-113 programmable token registry tracking (event listener, datum parser, registry service)
- The V2 query-priority merge that combines CIP-26 + CIP-68 + CIP-113 into a single response
- The data model (`metadata`, `metadata_reference_nft`, `cip113_registry_node`, `off_chain_sync_state`) and its Flyway migrations
- Health indicators for offchain sync, on-chain connection, on-chain readiness

In parallel, Bloxbean Labs (the yaci-store maintainers) built the `assets-ext` extension, which provides exactly the same capabilities in a reusable Spring Boot starter: `Cip26StorageReader`, `Cip68StorageReader`, `Cip113StorageReader`, a `TokenQueryService` that performs the priority-based merge, health indicators, Flyway migrations, and the entire GitHub sync pipeline. Several pieces in assets-ext were direct ports from this registry.

Maintaining both implementations meant every feature or bugfix landed twice. The two codebases drifted on column lengths, column names, and parsing edge cases. Any upstream improvement in assets-ext required manual backporting. This ADR records the decision to eliminate the duplication.

## Decision

Rebuild this service as a thin wrapper over `yaci-store-assets-ext-spring-boot-starter`. Concretely:

1. **Drop all duplicated code.**
   - Delete the `common` module entirely (entities, repositories, domain models, `GitService`, `TokenMetadataSyncService`, `TokenMappingService`, `TokenMetadataService`, validators).
   - Delete from `api/`: `V1ApiMetadataIndexer` + postgres impl, `Cip68EventListener`, `Cip68FTDatumParser`, `Cip68FungibleTokenService`, `Cip113EventListener`, `Cip113RegistryNodeParser`, `Cip113RegistryService`, `CustomUtxoStorage`, `AssetType`, `OnchainSyncStatusService`, our own health indicators, all v2 model DTOs.
   - Delete all Flyway migrations under `api/src/main/resources/db/migration/postgresql/`.

2. **Controllers become delegations.**
   - `V2ApiController` injects `TokenQueryService` and serves `SubjectResponse` / `SubjectBatchResponse` / `Subject` / `Metadata` directly — they are the same shape as the records this service previously owned. V1 of "use yaci-store types directly" (the API's public contract IS the JSON shape, not the Java package name).
   - `MetadataApiController` (V1) injects `Cip26StorageReader` and uses a small local mapper (`V1TokenMetadataMapper`) to project assets-ext's `TokenMetadata` entity onto the V1 CIP-26 annotated-property shape (`{value, signatures, sequenceNumber}`). This mapper is the one piece that can't be avoided — V1's response wrapping is different from V2's `{value, source}` wrapping.

3. **Job module becomes a thin `CommandLineRunner`.**
   - Instead of owning the sync logic, it injects assets-ext's `TokenMetadataSyncService` and calls `synchronizeDatabase()` once. The externally managed K8s CronJob deployment shape is preserved — only the image guts shrink.

4. **Configuration surface changes.**
   - `store.assets.ext.enabled=true` enables the extension.
   - `store.assets.ext.cip26.enabled=false` in the API's prod profile disables the in-process scheduled sync so the external K8s CronJob is the sole sync source (avoids double-writes).
   - `store.assets.ext.cip26.*` replaces our previous `git.*` + `token.metadata.job.*` properties.
   - `store.assets.ext.cip113.registry-nft-policy-ids` replaces our `cip113.registry.nft.policy-ids`.
   - `store.assets.ext.query.priority` replaces `cip.query.priority` (same default: `CIP_68,CIP_26`).

5. **Health probes use assets-ext's indicator beans.** The bean IDs are `assetStoreOffchainSync`, `assetStoreOnchainConnection`, `assetStoreOnchainReadiness` (the `assetStore` prefix avoids collisions between yaci-store extensions). Probe groups reference those directly.

6. **Metrics scope reduced.** The previous `RegistryMetricsService` shipped gauges that counted rows in the custom tables and a sync-status gauge — both redundant with yaci-store's `yaci.store.*` metrics and with `http_server_requests_seconds_*` from Spring Boot / Micrometer. The replacement is a ~70-line service with only the `cftr.api.cip.hits{cip=26|68|113}` counters (which are genuinely not available upstream).

## Consequences

### Positive

- **~9.7k lines removed**, one Maven module (`common`) deleted, 30+ classes gone. The api module shrinks from a stack to a bundle of controllers + one mapper.
- **Single source of truth.** CIP-26 sync, CIP-68 parsing, CIP-113 tracking, health, schema — all live in one place. Drift risk between this service and assets-ext is gone. Bug fixes and feature work in yaci-store flow to this service automatically on version bumps.
- **Consistent schema** with any other service built on assets-ext — simplifies cross-service tooling and operational debugging.
- **Thin-wrapper shape** means controller changes touch fewer files and have less surface area to regress.

### Negative

- **Tight version coupling to yaci-store assets-ext API.** A breaking change in `TokenQueryService` or the storage readers means a breaking change here. Mitigation: pin to released versions; coordinate with the yaci-store team before upgrading.
- **Snapshot dependency on this branch.** Version pinned to `2.1.0-pre4-d358232-SNAPSHOT` pending a proper yaci-store release. Must bump before merging to `main`.
- **`utxo_storage` / `tx_input` bloat.** `CustomUtxoStorage` used to filter UTxO persistence to only CIP-68 reference NFTs and CIP-113 registry nodes. That filter is gone, so these tables now accumulate every on-chain UTxO. Operationally non-critical (we never query them) but consumes disk. The underlying cause is that yaci-store bundles event emission (`UtxoProcessor`) and DB writes (`UtxoStorageImpl`) behind the same `store.utxo.enabled` flag; disabling the store silences `AddressUtxoEvent`, which assets-ext depends on. A future fix would be an upstream change to yaci-store to decouple event emission from persistence — tracked as a follow-up.
- **GraalVM native build needs rebasing.** `NativeImageConfig` (which registered reflection hints for our entities and REST DTOs) was removed because the entities it referenced no longer exist. A new hints config is needed, scoped to the controllers + `V1TokenMetadataMapper` + assets-ext types. Not done on this branch; deferred to a follow-up.
- **Unit-test coverage narrowed.** `MetadataApiV1ControllerTest` and `MetadataApiV2ControllerTest` were deleted because they mocked classes that no longer exist. Regression coverage shifts to `tests/end2end/mainnet/` (Python suite over live API) and the `integration-test/` module. `MetadataApiControllerTest` and `HealthApiControllerTest` were rewritten against the new dependencies.

## Alternatives Considered

- **Keep the custom stack and accept the duplication.** Rejected: the drift risk already bit us on column sizing (e.g. `metadata.policy` widening, CIP-113 column rename) and every such incident requires two fixes.
- **Contribute missing features upstream one at a time, keep our stack as the primary.** Rejected: most of assets-ext's core was already ported *from* this service. The net benefit of replacing-with-upstream is larger than the net benefit of keeping our fork around.
- **Wrap assets-ext but keep our own V2 DTO records** (map assets-ext → our types on every response). Rejected: the DTO shapes are byte-identical at the JSON layer, and the API contract is the JSON, not the Java package. Using assets-ext's types directly saves a trivial mapping layer at no observable cost to callers.
- **Keep `CustomUtxoStorage` on this branch.** Rejected by a separate conscious decision: the service works correctly without it; the cost is disk growth, not correctness. Reintroducing it is a ~50-line follow-up if disk becomes an operational concern, or (preferred) an upstream fix decoupling event emission from persistence in yaci-store.

## Impact on previous ADRs

| ADR | Effect |
|---|---|
| [ADR-001](001-multi-module-maven-structure.md) | `common` module removed |
| [ADR-003](003-graalvm-native-image-support.md) | Deprecated: `NativeImageConfig` deleted; native build needs rebasing |
| [ADR-004](004-cip26-cip68-dual-standard.md) | Implementation moved to assets-ext; conceptual design unchanged |
| [ADR-005](005-postgresql-jsonb-metadata.md) | Schema now owned by assets-ext (`ft_offchain_metadata`) |
| [ADR-006](006-flyway-database-migrations.md) | App no longer ships own migrations; assets-ext migrations only |
| [ADR-007](007-yaci-store-onchain-indexing.md) | Superseded: `CustomUtxoStorage` removed, assets-ext extension used instead of raw integration |
| [ADR-008](008-jgit-git-operations.md) | Superseded: JGit usage moved to assets-ext |
| [ADR-009](009-incremental-github-sync.md) | Superseded: sync logic moved to assets-ext |
| [ADR-010](010-v2-api-query-priority.md) | Implementation moved to assets-ext's `TokenQueryService`; contract unchanged |
| [ADR-011](011-health-check-groups-kubernetes.md) | Updated: indicator bean IDs prefixed `assetStore*` |
| [ADR-012](012-prometheus-business-metrics.md) | Scope reduced: only CIP-hit counters remain |
| [ADR-015](015-v2-api-extensions-model.md) | V2 DTOs provided by assets-ext; wire model unchanged |
| [ADR-016](016-cip113-programmable-tokens.md) | Implementation moved to assets-ext |
