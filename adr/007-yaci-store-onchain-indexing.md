# ADR-007: Yaci Store for On-Chain Data Indexing

## Status

**Superseded by [ADR-017](017-thin-wrapper-over-assets-ext.md)** — yaci-store is still the on-chain indexer, but the integration model changed: `CustomUtxoStorage` was deleted, `Cip68FungibleTokenService` and `Cip68EventListener` were replaced by assets-ext's equivalents (`Cip68Processor` + `Cip68StorageReader`), and `OnchainSyncStatusService` was replaced by assets-ext's health indicators. The service is now a consumer of `yaci-store-assets-ext-spring-boot-starter`, not a direct integrator of raw yaci-store components.

## Date

2026-03-24 (original); superseded 2026-04-23

## Context

CIP-68 token metadata is stored on the Cardano blockchain as reference NFTs. To serve this data via the REST API, the registry needs to index relevant on-chain data. This requires connecting to a Cardano node, following the chain, parsing transactions, and extracting reference NFT datums.

Building a custom chain indexer from scratch would be a significant undertaking, involving chain synchronization, rollback handling, CBOR decoding, and storage management.

## Decision

We integrate Yaci Store 2.0.0 as an embedded blockchain indexer within the API module. Yaci Store is a Cardano mini-indexer framework that provides:

1. **Chain synchronization**: Connects to a Cardano node (default: `backbone.mainnet.cardanofoundation.org:3001`) and follows the chain from a configurable start point (slot 65836843, the CIP-68 creation slot).

2. **Custom UTXO storage**: We extend `UtxoStorageImpl` with `CustomUtxoStorage` to filter and store only CIP-68 reference NFTs (identified by the `000643b0` asset name prefix with quantity = 1), reducing storage requirements.

3. **CIP-68 datum parsing**: `Cip68FungibleTokenService` extracts structured metadata (name, description, ticker, url, decimals, logo, version) from reference NFT datums.

4. **Auto-recovery**: Yaci Store's admin module provides automatic recovery from sync failures.

5. **Health monitoring**: `OnchainSyncStatusService` tracks sync progress as a percentage by comparing current slot against the network tip, with smart refresh intervals (15 minutes during initial sync, 1 minute when near-synced).

6. **Epoch calculation**: Configured with a 14,400-second interval (4 hours) for epoch boundary calculations.

## Consequences

### Positive

- **No separate indexer**: The blockchain indexer runs within the API process, eliminating the need for a separate indexer service and its operational overhead.
- **Selective indexing**: Custom UTXO storage ensures only relevant data (CIP-68 reference NFTs) is persisted, keeping the database lean.
- **Real-time updates**: On-chain metadata changes are reflected in the API as soon as the chain sync catches up.
- **Framework support**: Yaci Store handles chain following, rollback management, and CBOR decoding, letting us focus on CIP-68 business logic.

### Negative

- **Startup time**: Initial chain sync from the CIP-68 creation slot takes significant time, during which CIP-68 data is unavailable. The startup health probe and sync progress indicator mitigate this from an operational perspective.
- **Process coupling**: The indexer runs in the same process as the API. A crash in chain sync affects API availability, and vice versa.
- **Yaci Store dependency**: Tightly coupled to Yaci Store's API and data model. Major Yaci Store upgrades may require significant adaptation.
- **Resource consumption**: Chain synchronization consumes CPU and network bandwidth, which may compete with API request handling.

## Alternatives Considered

- **Blockfrost API**: Query on-chain data via Blockfrost's hosted API. Simpler to integrate but introduces a third-party dependency, API rate limits, and latency. Not suitable for real-time metadata serving.
- **DB Sync**: Use Cardano's full-chain PostgreSQL indexer. Provides comprehensive data but requires running a full Cardano node and DB Sync instance, which is operationally heavy for the limited data subset we need.
- **Separate indexer service**: Run Yaci Store as a standalone process writing to a shared database. Better isolation but adds deployment and monitoring complexity.
- **Ogmios / Kupo**: Lightweight chain followers that could provide similar data. Less integrated with the Spring Boot ecosystem than Yaci Store.
