# ADR-020: A Table for What Is Dropped (`invalid_token`), CIP-113 Included

## Status

Accepted. Follows yaci-store's ADR 0007 (bloxbean/yaci-store#1224), and adds the CIP-113 items it reserved.

## Date

2026-10-21

## Context

ADR-019 stores every token that can be read, with its validation errors. What is still dropped (ADR-019 decision 4)
has no row to carry errors, and left only a WARN in the log of one node. An operator could not ask "what did we drop,
and why" without reading logs, and the owner of a dropped token could not be told anything.

CIP-113 adds a failure of its own. A programmable token is served only with its CIP-26 or CIP-68 metadata (ADR-016).
A policy registered in the CIP-113 registry whose tokens have none cannot be served, and the API cannot tell its owner
why. yaci-store reserved codes for CIP-113 without writing them, since it does not index CIP-113 yet; the registry
does.

## Decision

### 1. One generic table, one row per dropped item

`invalid_token` (migration V7) holds the item, not the token, so one schema serves every standard:

| Column | Meaning |
|---|---|
| `id` | SHA-256 of `standard, reason_code, policy_id, asset_name, tx_hash, output_index, origin` |
| `standard` | the standard whose rule found the problem: `CIP-26`, `CIP-68`, `CIP-113` |
| `combined_with` | the other standard of a combination, when there is one |
| `reason_code`, `reason` | a stable code for programs, a message for people (at most 500 characters) |
| `policy_id`, `asset_name`, `subject` | the token, when known |
| `slot`, `tx_hash`, `output_index` | the on-chain origin |
| `origin` | the off-chain origin (the registry file name) |
| `details` | bounded JSON (at most 2 KB): `inner_subject`, `datum_prefix`, `exception`, `registry_key`, ... |
| `first_seen_at` | when it was first recorded |

### 2. Only what has no row

A token that is stored keeps its problems in its own `validation_errors` and is **not** in this table. The codes
written:

| Standard | Code | When |
|---|---|---|
| CIP-26 | `filename_subject_mismatch` | a registry file named after another subject than the one inside it (indexed under neither) |
| CIP-26 | `unusable_subject` | a subject that cannot be a key (not even-length hex, not 56 to 120 characters) |
| CIP-26 | `unparseable_mapping` | a file that cannot be read as a mapping |
| CIP-26 | `missing_git_history` | a file with no commit in the registry's history |
| CIP-68 | `datum_undecodable`, `datum_too_deep_to_decode` | a datum that cannot be decoded |
| CIP-68 | `processing_failure` | a reference NFT whose indexing throws something unexpected (one row per reference NFT) |
| CIP-113 | `registry_node_unparseable` | a registry node whose datum cannot be parsed, or has no inline datum |
| CIP-113 | `no_companion_metadata` | a registered policy none of whose tokens has CIP-26 or CIP-68 metadata |

A datum that is not CIP-68 at all is ignored, not recorded, since a reference NFT output can carry any datum.

### 3. CIP-113: a registered policy without a companion

The registry indexes registry nodes, not the mints or the programmable-token ("jail") address. So the gap is known per
**policy**, not per token, and a policy registered with nothing minted yet is reported the same way.

The row describes a state, not an event, so it is recomputed by `Cip113CompanionReconciler`:
- **What it computes:** the registered policies (node keys of a policy id's length) minus those with a companion. A
  companion is a CIP-26 entry, or **served** CIP-68 metadata: a reference NFT whose 333 or 444 user token was minted
  (ADR-022). A reference NFT alone serves nothing, so it does not count. Metadata with validation errors counts, since
  it is served.
- **What it writes:** one row per policy without a companion. Rows of policies that now have one, or are no longer
  registered, are removed.
- **When it runs:** when one of its inputs changes:
  - registry nodes are indexed, or removed by a rollback;
  - the CIP-68 listener stores a reference NFT, or sees a user token for the first time, of a registered policy;
  - a CIP-26 sync run in the same process stores or removes entries (`Cip26SyncCompletedEvent`);
  - the application starts.
- **When the CIP-26 sync runs in a separate `job` process** (`TOKEN_METADATA_SYNC_JOB=false`, as on mainnet), its
  changes reach the API only through the database. The reconciler then also runs on a schedule
  (`cip113.companion.reconcile.interval-ms`, 10 minutes by default). When the API runs the sync itself, the schedule
  does nothing.
- **Read-only instances** never reconcile: they do not write.
- **Rollbacks:** these rows have no slot, so a rollback does not remove them; the reconciliation does.

The tokens of such a policy are a 404 in V1 and V2, as before.

### 4. Written safely

The CIP-68 and CIP-113 listeners run in yaci-store's block transaction, where an exception stops the whole sync. The
recorder is therefore:
- **bounded:** every text is cut, and `details` has at most 2 KB.
- **idempotent:** the id is derived from the item, and an existing id is left alone. A replayed block or a re-run sync
  adds nothing and keeps `first_seen_at`.
- **contained:** its own exceptions are a WARN, never thrown.

### 5. CIP-26 rows say what is wrong now

At the start of each registry file, the rows of that file are forgotten and the file is judged again. When a file is
deleted upstream, its rows go too.

### 6. Rollback and access

A chain rollback deletes the rows with a `slot` after the rollback slot. There is no API: the table is read with SQL.
The counter `token.metadata.invalid.tokens{standard,reason_code}` counts the rows written.

## Consequences

### Positive

- An operator can list what was dropped and why, including the CIP-113 policies that cannot be served, and tell
  their owners.
- Adding a standard needs new codes and a writer, not a migration.

### Negative

- The table is complete only for a database synced after it existed, so a full resync is required (ADR-019).
- `no_companion_metadata` is per policy. With the CIP-26 sync in a separate process, it lags a new CIP-26 entry by up to
  one schedule interval; otherwise it follows the data as it changes.

## Alternatives Considered

- **Log only:** not queryable, per node, lost with the logs.
- **Record every token, errors included:** duplicates `validation_errors`, with tens of thousands of rows.
- **A table per standard:** a migration for every standard, and awkward combinations such as CIP-68 with CIP-113.
- **Index the CIP-113 jail and record each token without a companion (`jail_without_token`):** exact per token, but it
  needs indexing mints and the programmable-token address, and a full resync. Kept for later, if per-token detail is
  needed.
