# ADR-004: CIP-26 and CIP-68 Dual Standard Support

## Status

Accepted

## Date

2026-03-24

## Context

The Cardano ecosystem has two complementary standards for token metadata:

- **CIP-26** (Cardano Offchain Metadata): Token metadata is submitted to a centralized GitHub repository (`cardano-foundation/cardano-token-registry`) and served via a REST API. This is the original standard, widely adopted but dependent on a manual submission process.

- **CIP-68** (Datum Metadata Standard): Token metadata is stored on-chain as reference NFTs. Each fungible token can have an associated reference NFT (prefix `000643b0`) whose datum contains the metadata. This is a newer, fully decentralized approach.

Many tokens have metadata registered under one or both standards, and consumers need a unified interface to query metadata regardless of its source. The registry must support both standards and allow consumers to express a preference for which source takes priority.

## Decision

We support both CIP-26 and CIP-68 as first-class metadata sources with the following design:

1. **Separate data models**: CIP-26 metadata is stored in the `metadata` table (with JSONB for extensible properties). CIP-68 metadata is stored in the `metadata_reference_nft` table with structured columns (name, description, ticker, url, decimals, logo, version, datum).

2. **Separate ingestion paths**: CIP-26 data is synced from GitHub via `TokenMetadataSyncService`. CIP-68 data is indexed from the Cardano blockchain via Yaci Store and `Cip68FungibleTokenService`.

3. **Query priority model**: A `QueryPriority` enum (`CIP_26`, `CIP_68`) controls which source is queried first. The default priority is configurable via `cip.query.priority=CIP_68,CIP_26` (on-chain data preferred). The V2 API allows per-request priority override.

4. **CIP-68 reference NFT identification**: Reference NFTs are identified by the `000643b0` prefix in their asset name and must have a quantity of exactly 1. The `CustomUtxoStorage` filters UTXOs to store only matching reference NFTs.

5. **Validation**: CIP-26 tokens are validated against the spec using the `cf-tokens-cip26` library. CIP-68 tokens are validated structurally during datum parsing.

## Consequences

### Positive

- **Comprehensive coverage**: Consumers get metadata regardless of which standard the token issuer chose.
- **Flexible priority**: Operators and API consumers can decide which source they trust more.
- **Future-proof**: Adding support for additional CIP standards (e.g., CIP-25 for NFTs) follows the same pattern.
- **Data integrity**: Separate storage prevents data model conflicts between standards with different schemas.

### Negative

- **Dual maintenance**: Two ingestion pipelines, two data models, and two health indicators to maintain.
- **Consistency challenges**: The same token may have different metadata under each standard, and the API must handle conflicts gracefully.
- **Query complexity**: The V2 API must merge results from multiple sources, adding logic to controllers and complicating testing.

## Alternatives Considered

- **CIP-26 only**: Simpler but ignores the growing adoption of on-chain metadata and would make the registry increasingly incomplete.
- **CIP-68 only**: Fully decentralized but would drop support for the large existing CIP-26 registry, breaking backward compatibility for existing consumers.
- **Unified data model**: A single table for both standards would simplify queries but would require compromising on schema design, as CIP-26 uses JSONB properties while CIP-68 has fixed datum fields.
