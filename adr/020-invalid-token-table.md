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

**Why this check exists.** A CIP-113 registry node says only that a policy is *programmable*: which scripts govern its
minting and transfers. It carries no name, ticker, decimals or logo. Those come from the token's ordinary metadata, in
CIP-26 or CIP-68, which we call its **companion**. The V2 API serves a programmable token only with its companion
(ADR-016): it returns the metadata with `type: PROGRAMMABLE` and the node under `extensions.cip113`. A token with only
a registry node is a 404, because there is nothing a wallet could show.

So a policy can be registered in the CIP-113 registry and still be invisible in the API, with no error anywhere: the
indexer did its job, and V2 correctly has nothing to serve. The check makes that state visible to operators. One
`invalid_token` row per such policy, and the `token_metadata_invalid_tokens_total` counter, answer "which programmable
tokens can we not serve, and why", for example to tell an issuer that their token needs CIP-26 or CIP-68 metadata.
**It changes no API response;** it is a diagnostic.

**Why a reconciler.** Whether a policy has a companion is a *state*, made of three inputs that change independently:
the registry nodes (on chain), the CIP-26 entries (the GitHub registry, synced off chain) and the CIP-68 metadata (on
chain). No single moment knows all three. So the rows are not written once by one listener; they are recomputed from
the three inputs by `Cip113CompanionReconciler` whenever one of them changes.

The registry indexes registry nodes, not the mints or the programmable-token ("jail") address. So the gap is known per
**policy**, not per token, and a policy registered with nothing minted yet is reported the same way.

- **What it computes:** the registered policies (node keys of a policy id's length) minus those with a companion. A
  companion is a CIP-26 entry or a CIP-68 reference NFT, both of which V2 serves. Metadata with validation errors
  counts, since it is served.
- **What it writes:** one row per policy without a companion. Rows of policies that now have one, or are no longer
  registered, are removed.
- **When it runs: on events.** Each input publishes a Spring application event when it changes, and the reconciler
  listens to them:

  | Event | Published by | When |
  |---|---|---|
  | `Cip113RegistryChangedEvent` | CIP-113 listener | registry nodes were indexed, or removed by a rollback |
  | `Cip68MetadataIndexedEvent(policyIds)` | CIP-68 listener | a block stored a reference NFT; the reconciler runs only if one of the policies is registered |
  | `Cip26SyncCompletedEvent` | CIP-26 sync | a sync run in the same process stored or removed entries |
  | `ApplicationReadyEvent` | Spring | the application started |

  The listeners do not call the reconciler; they publish facts, and anything that depends on them can listen.
- **After the commit, in its own transaction.** The CIP-68 and CIP-113 events are published inside Yaci Store's block
  transaction. The reconciler reacts with `@TransactionalEventListener(phase = AFTER_COMMIT)` and
  `@Transactional(REQUIRES_NEW)`: it runs once the block is committed, in a transaction of its own. A database error in
  the reconciler, which on PostgreSQL would abort the transaction it runs in, can therefore never stop the sync. If the
  process stops between the commit and the reconciliation, nothing is lost for long: the next event, or the next
  start, recomputes everything. An event published outside a transaction, as by the CIP-26 sync, is delivered at once.
- **When the CIP-26 sync runs in a separate `job` process** (`TOKEN_METADATA_SYNC_JOB=false`, as on mainnet), its
  events never reach the API, whose only view of the changes is the database. The reconciler then also runs on a
  schedule (`cip113.companion.reconcile.interval-ms`, 10 minutes by default). When the API runs the sync itself, the
  schedule does nothing.
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
