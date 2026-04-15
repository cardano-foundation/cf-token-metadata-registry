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
- **transfer_logic_script** — optional hash of the Plutus script that validates every transfer
- **third_party_transfer_logic_script** — optional hash of the script for issuer/admin operations (freeze, seize, burn)
- **global_state_policy_id** — optional policy ID of a global state NFT (e.g., a denylist for freeze-and-seize)

The token metadata registry should surface this information so that wallets, dApps, and explorers can identify programmable tokens and display their transfer constraints alongside standard display metadata.

This is the first implementation of the V2 API extensions model (ADR-015).

## Decision

### 1. Data model

A new `cip113_registry_node` table stores indexed registry entries:

```sql
CREATE TABLE cip113_registry_node (
    key VARCHAR(64) NOT NULL,
    slot BIGINT NOT NULL,
    tx_hash VARCHAR(64) NOT NULL,
    transfer_logic_script VARCHAR(56),
    third_party_transfer_logic_script VARCHAR(56),
    global_state_policy_id VARCHAR(56),
    next VARCHAR(64) NOT NULL,
    datum TEXT NOT NULL,
    PRIMARY KEY (key, slot, tx_hash)
);
```

Column names mirror the on-chain datum field names (`key`, `next`, `transfer_logic_script`, `third_party_transfer_logic_script`, `global_state_cs` → stored as `global_state_policy_id`) so that the schema, the parsed record (`ParsedRegistryNode`), and the CIP-113 spec all agree. In particular, the `key` column is deliberately not named `policy_id`: only real registered policies populate it with a 56-hex policy ID — the head sentinel stores an empty string and the tail sentinel stores a 58–64-hex marker (conventionally 32 bytes of `0xFF` in the aiken-linked-list library), neither of which is a policy ID.

Column lengths are bounded to their protocol maxima: 28-byte policy IDs / credential hashes are `VARCHAR(56)` (56 hex chars), 32-byte transaction hashes are `VARCHAR(64)`. The `key` and `next` columns are `VARCHAR(64)` rather than `VARCHAR(56)` to accommodate the head/tail sentinels described above. The `next` column is `NOT NULL` because every registry node — including the head and tail sentinels — must point to a next entry.

The composite primary key `(key, slot, tx_hash)` allows tracking historical updates. Queries use `ORDER BY slot DESC LIMIT 1` to get the latest entry for a given key (i.e. policy ID of a registered programmable token).

**Field nullability:**

| Field | Required | Nullable | Rationale |
|-------|----------|----------|-----------|
| `transfer_logic_script` | no | yes | The transfer validation script hash. May be absent in registry nodes that do not yet specify transfer logic. |
| `third_party_transfer_logic_script` | no | yes | Not all substandards require issuer/admin operations. |
| `global_state_policy_id` | no | yes | Only substandards with shared state (e.g. freeze-and-seize denylists) use this. |

The indexer is defensive: anyone can put invalid data on-chain, so the parser validates the datum and skips entries with missing `key` or `next` fields (logging a warning). All three script/policy fields (`transfer_logic_script`, `third_party_transfer_logic_script`, `global_state_policy_id`) are nullable.

### 2. On-chain indexing

CIP-113 registry nodes are indexed using the existing Yaci Store infrastructure:

- **`CustomUtxoStorage`** is extended to persist UTxOs that match configured CIP-113 registry NFT policy IDs (quantity = 1)
- **`Cip113EventListener`** processes `AddressUtxoEvent`s, filtering for UTxOs with inline datums that match monitored policy IDs
- **`Cip113RegistryNodeParser`** deserializes the CBOR datum (ConstrPlutusData with 5 fields) into a structured record

### 3. API response

CIP-113 data is served as an extension on V2 subject endpoints (ADR-015). When querying a specific token by subject, CIP-113 data appears under the `extensions.cip113` key and the `type` field indicates whether the token is `NATIVE` or `PROGRAMMABLE`:

```json
{
  "subject": {
    "subject": "577f0b...0014df10464c4454",
    "type": "PROGRAMMABLE",
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

The `ProgrammableTokenCip113` record implements the `Extension` interface. All three fields (`transfer_logic_script`, `third_party_transfer_logic_script`, `global_state_policy_id`) are nullable — registry nodes may omit any of them depending on the substandard.

### 4. Token type classification

The V2 `Subject` response includes a `type` field (`TokenType` enum):

- **`NATIVE`** — standard token with no on-chain transfer logic
- **`PROGRAMMABLE`** — token with CIP-113 (or future) extensions defining transfer validation rules

The type is derived from the presence of extensions: if any extension is present, the token is `PROGRAMMABLE`; otherwise `NATIVE`.

### 5. Not a metadata source

CIP-113 is a layer on top of existing metadata standards — it defines how to constrain CIP-26/CIP-68 tokens inside smart contracts with transfer validation logic. It does not provide display metadata (name, description, ticker). Display metadata for programmable tokens comes from CIP-26 or CIP-68, same as for any other Cardano token. Therefore, CIP-113 is not part of the `QueryPriority` enum and does not participate in the metadata merge.

### 6. Implicit enablement

CIP-113 support is derived from configuration: it is active when `CIP113_REGISTRY_NFT_POLICY_IDS` contains at least one policy ID. No separate boolean flag is needed. An empty list means there are no registries to monitor, so all CIP-113 code paths short-circuit.

### 7. Batch optimization

For the batch query endpoint (`POST /api/v2/subjects/query`), CIP-113 data is pre-fetched for all unique policy IDs in a single query via `Cip113RegistryService.findByPolicyIds()`, avoiding N+1 database calls.

### 8. Rollback handling

Both CIP-113 and CIP-68 event listeners handle Yaci Store's `RollbackEvent`. When the Cardano node reports a chain rollback, all indexed entries with `slot > rollbackSlot` are deleted. This follows the same pattern used by all Yaci Store modules (UTxO, assets, transactions, etc.).

```java
@EventListener
@Transactional
public void handleRollback(RollbackEvent rollbackEvent) {
    long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
    int count = repository.deleteBySlotGreaterThan(rollbackSlot);
}
```

The `ORDER BY slot DESC` query pattern then naturally picks up the correct pre-rollback entry (if one exists at a lower slot).

**Edge cases:**

- **Rolled-back mint that reappears**: The re-indexed block produces a new `AddressUtxoEvent`, so the entry is re-created at the correct slot. No data loss.
- **Rolled-back mint that never reappears** (e.g., transaction dropped from mempool): The entry is deleted and the token reverts to `NATIVE` (no CIP-113 extension). This is correct — the registration never confirmed on-chain.
- **Rollback deeper than the initial registration**: If the rollback slot is earlier than when the token was first registered, all entries for that token are deleted. The token appears as unregistered until re-indexed. This is correct — the registration didn't happen yet at that chain point.
- **Multiple updates at different slots**: Only entries after the rollback point are deleted. Earlier entries (at lower slots) survive and become the latest via `ORDER BY slot DESC`.

## Consequences

### Positive

- **Complete token picture**: Wallets and explorers can show both display metadata and programmable token constraints in a single API call.
- **Validates the extensions model**: CIP-113 serves as the first real-world test of ADR-015, proving the pattern works for orthogonal CIP data.
- **Reuses existing infrastructure**: Yaci Store, Flyway migrations, and the V2 controller extension-building pattern are shared with CIP-68, minimizing new code.
- **Chain-consistent**: Rollback handling ensures the DB always reflects confirmed on-chain state. No phantom entries for rolled-back registrations.

### Negative

- **Operator configuration**: Operators must know the policy ID(s) of the CIP-113 registry NFT minting script(s) deployed on their target network. If new registries are deployed, the configuration must be updated.
- **CBOR parsing brittleness**: The datum parser assumes a specific ConstrPlutusData layout (constructor 0, 5 fields). If the CIP-113 datum format evolves, the parser must be updated.

## Alternatives Considered

- **Query the registry on-chain at request time**: Instead of indexing, query a Cardano node for the registry UTxO on each API request. Rejected due to latency and availability concerns — the registry is on-chain state that requires UTxO lookups, which are too slow for API response times.
- **Store in the existing metadata tables**: Reuse `metadata` or `metadata_reference_nft` with extra columns. Rejected because CIP-113 data is keyed by policy ID (not subject), has a fundamentally different schema, and is not display metadata.
- **Separate microservice**: Deploy CIP-113 indexing and serving as a standalone service. Rejected because it adds operational complexity for a feature that shares infrastructure (Yaci Store, PostgreSQL) with the existing API.
