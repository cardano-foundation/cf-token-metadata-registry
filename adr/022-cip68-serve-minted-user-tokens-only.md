# ADR-022: Serve a CIP-68 333 or 444 Subject Only When Its User Token Was Minted

## Status

Accepted. Ported from yaci-store (bloxbean/yaci-store#1294, its ADR 0008). Fixes #112.

## Date

2026-10-22

## Context

A CIP-68 token's metadata is the datum of its reference NFT (`000643b0` + name). The V2 API maps a fungible (333,
`0014df10` + name) or rich fungible (444, `001bc280` + name) subject to that reference NFT and serves its datum.

The reference NFT says nothing about which user tokens exist. Every reference NFT therefore answered under **both**
labels, whether or not that token was ever minted, including the reference NFTs of NFTs (222):
- **Preprod (2026-10-22):** 1,078 333 subjects and 2,556 444 subjects that do not exist on chain answered 200, against
  2,417 333/444 user tokens that do.
- **Mainnet:** 334 of the 999 subjects of the CIP-68 regression baseline were never minted as 333. 324 are NFTs, 5 are
  rich fungible tokens, 5 have no user token at all.

A client could not tell a real token from a label that was never minted. Since ADR-019 such a subject was also reported
with the errors of the wrong label, for example a 444 token asked for as 333 "misses a description".

The indexer already saw part of the answer: `deriveLabels` finds the user tokens minted in the same transaction as the
reference NFT. It kept only the choice of validation rules, and missed a user token minted in another transaction, which
CIP-68 allows.

## Decision

**Record the 333 and 444 user tokens that appear on chain, and serve such a subject from CIP-68 only when its token was
seen.**

1. **A small table, `cip68_user_token`** (migration V8): `unit` (policy id + asset name, lower case, primary key),
   `slot`, `tx_hash`. One row per 333 or 444 token, written the first time it appears in an output. Only a unit whose
   asset name starts with `0014df10` or `001bc280` and has a name after it is kept, so a row is small, and the table
   holds a few thousand rows on mainnet.
2. **Recorded by the CIP-68 listener** (`Cip68UserTokenRecorder`), per block, in Yaci Store's block transaction:
   - **idempotent:** a token recorded at the same or an earlier slot is not written again;
   - **safe with parallel block processing:** the write is a database upsert (`Cip68UserTokenWriter`, `ON CONFLICT` on
     PostgreSQL, `MERGE` on H2), in a fixed order. It keeps the **smallest** slot, so a rollback between two blocks
     that committed out of order does not delete a token that was already seen. Upstream needed this: a read followed
     by an insert stopped their first preprod sync on a duplicate key;
   - **best effort:** a failure of its own is a WARN, never an exception.
3. **Rollback:** the rows first seen after the rollback slot are deleted, with the reference NFT rows.
4. **Serving:** a single subject looks up its unit, one primary-key query. A batch looks up all its subjects in one
   `IN` query (`BatchPrefetchData.cip68UserTokens`). A subject whose token was never seen has nothing from CIP-68: a
   CIP-26 entry for it is still served, and with neither it is a 404 (or left out of a batch).
5. **The label of the row is not used as the gate.** A reference NFT can serve several user tokens (a 222 and a 333
   together), and a label can be inferred from the datum when the user token is minted later. Checking that the token
   exists cannot hide one that exists.

## Consequences

### Positive

- V2 answers only for tokens that exist. Clients can rely on a 200 meaning "this asset exists and has metadata".
- A token is no longer served under, or reported with the errors of, a label it does not have.
- The design and code match yaci-store's `assets-ext`, so moving to it later changes nothing for clients.

### Negative

- **Needs the chain history.** The table is empty after the migration and is filled by a sync from scratch. On a
  database that is not resynced, every 333/444 subject is a 404 until its token appears again. ADR-019 already
  requires the full resync, and this decision ships with it.
- **Behaviour change:** subjects that answered 200 are now 404, including the 333 and 444 subjects of NFTs. The mainnet
  regression baseline is corrected accordingly: the 334 never-minted subjects moved to
  `cip68_never_minted_subjects.json`, which asserts the 404.
- **One more write per block** with 333/444 tokens, and one more query per V2 request for a CIP-68 subject.
- **A burned token stays "seen":** the table records that a token existed, not its supply. A token burned to zero is
  still served, with the metadata it had.

## Alternatives Considered

- **Infer the label from the datum:** a datum that follows CIP-68 reads the same as 333 or 444. That ambiguity is the
  bug.
- **Use the label stored with the row:** it hides user tokens minted in a later transaction, and it can hold only one
  label where a reference NFT serves several.
- **Ask Koios or the node at request time:** an external dependency, and a round trip, on every query.
- **Enable Yaci Store's `assets` module:** it stores every mint on the chain, millions of rows on mainnet, for a lookup
  that needs a few thousand.
- **Track supply and stop serving a token burned to zero:** more writes and rollback cases for a rare case.
  Not done now; possible later.
