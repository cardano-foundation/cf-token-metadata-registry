# ADR-016: CIP-113 Programmable Token Support

## Status

Accepted

## Date

2026-03-26

## Context

[CIP-113](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0113) defines **programmable tokens** for Cardano — standard fungible tokens (CIP-26/CIP-68) that are additionally constrained by on-chain transfer validation logic. The mechanism works via the withdraw-zero pattern: a Plutus script must successfully execute whenever tokens move between addresses, enabling use cases like regulatory compliance, freeze/seize, and custom transfer rules.

CIP-113 introduces an on-chain **registry** of programmable tokens. Each registry entry is an NFT whose datum contains:

- **key** — the policy ID of the token being registered as programmable
- **next** — linked-list pointer to the next registry entry
- **transfer_logic_script** — hash of the Plutus script that validates every transfer
- **third_party_transfer_logic_script** — hash of the script for issuer/admin operations (freeze, seize, burn)
- **global_state_policy_id** — optional policy ID of a global state NFT (e.g., a denylist for freeze-and-seize)

The token metadata registry should surface this information so that wallets, dApps, and explorers can identify programmable tokens and display their transfer constraints alongside standard display metadata.

This is the first implementation of the V2 API extensions model (ADR-015).

## Decision

### 1. Data model

A new `cip113_registry_node` table stores indexed registry entries:

```sql
CREATE TABLE cip113_registry_node (
    policy_id TEXT NOT NULL,
    slot BIGINT NOT NULL,
    tx_hash TEXT NOT NULL,
    transfer_logic_script TEXT,
    third_party_transfer_logic_script TEXT,
    global_state_policy_id TEXT,
    next_key TEXT,
    datum TEXT NOT NULL,
    PRIMARY KEY (policy_id, slot, tx_hash)
);
```

The composite primary key `(policy_id, slot, tx_hash)` allows tracking historical updates. Queries use `ORDER BY slot DESC LIMIT 1` to get the latest entry for a given policy ID.

### 2. On-chain indexing

CIP-113 registry nodes are indexed using the existing Yaci Store infrastructure:

- **`CustomUtxoStorage`** is extended to persist UTxOs that match configured CIP-113 registry NFT policy IDs (quantity = 1)
- **`Cip113EventListener`** processes `AddressUtxoEvent`s, filtering for UTxOs with inline datums that match monitored policy IDs
- **`Cip113RegistryNodeParser`** deserializes the CBOR datum (ConstrPlutusData with 5 fields) into a structured record

### 3. API response — two access patterns

CIP-113 data is served in two complementary ways:

**a) As an extension on V2 subject endpoints (ADR-015)**

When querying a specific token by subject, CIP-113 data appears under the `extensions.cip113` key:

```json
{
  "subject": {
    "subject": "577f0b...0014df10464c4454",
    "metadata": { "name": {...}, "description": {...} },
    "extensions": {
      "cip113": {
        "transfer_logic_script": "aaa513b0...",
        "third_party_transfer_logic_script": "def513b0...",
        "global_state_policy_id": "12345678..."
      }
    }
  }
}
```

**b) Via the V2 policy endpoints (ADR-017)**

When querying by policy ID, CIP-113 data appears as the `programmable` field alongside all tokens under that policy:

```json
{
  "policy_id": "ae563991...",
  "tokens": [
    { "subject": "ae563991...0014df10555344432", "name": "USDC", "ticker": "USDC", "decimals": 6, "source": "CIP_68" }
  ],
  "programmable": {
    "transfer_logic_script": "77014ec6...",
    "third_party_transfer_logic_script": "4b348779...",
    "global_state_policy_id": null
  }
}
```

The `ProgrammableTokenCip113` record implements the `Extension` interface. The `global_state_policy_id` field is nullable — not all programmable token substandards require global state.

### 4. Implicit enablement

CIP-113 support is derived from configuration: it is active when `CIP113_REGISTRY_NFT_POLICY_IDS` contains at least one policy ID. No separate boolean flag is needed. An empty list means there are no registries to monitor, so all CIP-113 code paths short-circuit.

### 5. Batch optimization

For the batch query endpoint (`POST /api/v2/subjects/query`), CIP-113 data is pre-fetched for all unique policy IDs in a single query via `Cip113RegistryService.findByPolicyIds()`, avoiding N+1 database calls.

## Consequences

### Positive

- **Complete token picture**: Wallets and explorers can show both display metadata and programmable token constraints in a single API call.
- **Validates the extensions model**: CIP-113 serves as the first real-world test of ADR-015, proving the pattern works for orthogonal CIP data.
- **Reuses existing infrastructure**: Yaci Store, Flyway migrations, and the V2 controller extension-building pattern are shared with CIP-68, minimizing new code.

### Negative

- **Operator configuration**: Operators must know the policy ID(s) of the CIP-113 registry NFT minting script(s) deployed on their target network. If new registries are deployed, the configuration must be updated.
- **CBOR parsing brittleness**: The datum parser assumes a specific ConstrPlutusData layout (constructor 0, 5 fields). If the CIP-113 datum format evolves, the parser must be updated.
- **No rollback handling**: Registry node updates are tracked by slot, but if a blockchain rollback occurs, stale entries may persist until the next valid entry is indexed.

## Alternatives Considered

- **Query the registry on-chain at request time**: Instead of indexing, query a Cardano node for the registry UTxO on each API request. Rejected due to latency and availability concerns — the registry is on-chain state that requires UTxO lookups, which are too slow for API response times.
- **Store in the existing metadata tables**: Reuse `metadata` or `metadata_reference_nft` with extra columns. Rejected because CIP-113 data is keyed by policy ID (not subject), has a fundamentally different schema, and is not display metadata.
- **Separate microservice**: Deploy CIP-113 indexing and serving as a standalone service. Rejected because it adds operational complexity for a feature that shares infrastructure (Yaci Store, PostgreSQL) with the existing API.
