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
| CIP-26 | `logo_too_large` | a logo far beyond any real size (over about 1 MiB): the entry is stored without it, so the item without a row is the logo |
| CIP-26 | `processing_failure` | a file whose check throws something unexpected; the subject may be fine, so it is not `unusable_subject` |
| CIP-68 | `datum_undecodable`, `datum_too_deep_to_decode` | a datum that cannot be decoded |
| CIP-68 | `processing_failure` | a reference NFT whose indexing throws something unexpected (one row per reference NFT) |
| CIP-113 | `registry_node_unparseable` | a registry node whose datum cannot be parsed, or has no inline datum |
| CIP-113 | `no_companion_metadata` | **retired** (decision 3): a token without CIP-26 or CIP-68 metadata is served with a validation error |

A datum that is not CIP-68 at all is ignored, not recorded, since a reference NFT output can carry any datum.

### 3. CIP-113: a registered policy without a companion is served, not recorded

A CIP-113 registry node says only that a policy is *programmable*: which scripts govern its minting and transfers. It
carries no name, ticker, decimals or logo. Those come from the token's ordinary metadata, in CIP-26 or CIP-68, which we
call its **companion**.

**Decision (2026-10-27, after the review of bloxbean/yaci-store#1220).** A token of a registered policy is **served**
whether it has a companion or not, and a token without one is **not** an `invalid_token` row. V2 returns it with
`type: PROGRAMMABLE`, the registry node under `extensions.cip113`, no metadata, and one validation error:

```json
{"cip": "CIP-113", "field_name": "companion_metadata", "field_category": "OPTIONAL", "code": "missing_value",
 "error": "the policy is registered in the CIP-113 registry, but the token has no CIP-26 or CIP-68 metadata (name, ticker, decimals, logo)"}
```

*Why:* this table holds what cannot be stored, and a registry node without a companion is stored and served like any
other: it is a programmable token that breaks a rule, the case ADR-019 decided for CIP-26 and CIP-68, where a token
that departs from its CIP is served with its errors so that its owner sees what to fix and a wallet decides. CIP-113 had
not been relaxed with them. Some programmable tokens may be valid without CIP-26 or CIP-68 metadata.

- **The category is open.** It is `OPTIONAL` for now (`Cip113Validation.MISSING_COMPANION_CATEGORY`), so the token is
  returned by `?validation=ALL` and `MANDATORY_ONLY` and left out by `STRICT`. `MANDATORY` would also leave it out of
  `MANDATORY_ONLY`. The team decides; the change is one constant.
- **Per subject, not per policy.** The check is made when a subject is served, on the rows the request reads: a subject
  of a registered policy with no CIP-26 or CIP-68 metadata for **that** subject has the error, whatever the other tokens
  of the policy have. The registry indexes nodes, not mints, so any asset name under a registered policy answers `200`,
  including names never minted; this is the trade-off accepted for 333 and 444 subjects (ADR-022, rejected).
- **A property filter keeps the error:** it concerns every property.
- **Not a 404 any more, and no reconciler.** Before, such a token was a 404, and `Cip113CompanionReconciler` kept a
  `no_companion_metadata` row per policy in step with three event streams and a schedule. The reconciler, its events
  (`Cip113RegistryChangedEvent`, `Cip68MetadataIndexedEvent`, `Cip26SyncCompletedEvent`) and the
  `cip113.companion.reconcile.*` settings are removed. `no_companion_metadata` is retired: not written, and not reused.
  A database that has such rows keeps them until it is resynced from an empty one, which this release requires anyway.
- **V1 is unchanged:** it is CIP-26 only.

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

## Alternatives Considered

- **Log only:** not queryable, per node, lost with the logs.
- **Record every token, errors included:** duplicates `validation_errors`, with tens of thousands of rows.
- **A table per standard:** a migration for every standard, and awkward combinations such as CIP-68 with CIP-113.
- **Index the CIP-113 jail and record each token without a companion (`jail_without_token`):** exact per token, but it
  needs indexing mints and the programmable-token address, and a full resync. Moot since decision 3: such a token is
  served with an error.
- **Record a registered policy without a companion in this table (`no_companion_metadata`):** what was done until
  2026-10-27. Replaced by decision 3: it hid tokens the registry indexed, and treated as unstorable what is a token
  breaking a rule.
