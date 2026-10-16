# ADR-017: Serve the Last Valid CIP-68 Datum

## Status

Accepted

## Date

2026-10-16

## Context

A CIP-68 token's metadata is the inline datum on the output that holds its reference NFT (prefix `000643b0`). The
issuer can move the reference NFT at any time, and the new output may carry:

- a new, valid datum (a metadata update),
- a datum the registry rejects: no `name`, no `description` for a fungible token (label 333), `decimals` encoded as
  bytes, `name` encoded as a Plutus list of byte chunks instead of a byte string, or a version CIP-68 does not define
  (only 1 to 4 are accepted), or
- no datum at all.

The indexer stores one row in `metadata_reference_nft` per valid datum, keyed by `(policy_id, asset_name, slot)`. A
rejected datum is logged with a WARN and not stored, and an output without a datum is not stored. The read path
(`findFirstByPolicyIdAndAssetNameOrderBySlotDesc`) returns the row with the highest slot.

So when a reference NFT moves to an output with a rejected datum or no datum, the API keeps serving the datum of an
earlier output. A chain explorer such as Koios shows the current datum, or none, so the two disagree for these
tokens.

On preprod this is common. A sanity check of 75 fungible tokens on 2026-10-16 found:
- 7 whose reference NFT now sits on an output with no datum.
- 1 (`4ce978bb…` "Kai token name") whose newer datum encodes `name` as a list of byte chunks.

## Decision

The registry serves **the last valid datum** of a CIP-68 token. It does not serve the datum of the reference NFT's
current output.

- A rejected datum or a missing datum does not clear or replace the stored metadata.
- A newer **valid** datum replaces it, because it is stored with a higher slot.
- A rollback removes the rows above the rollback slot (`deleteBySlotGreaterThan`). The datum that was valid at that
  point is then served again.

## Consequences

### Positive

- An issuer's mistake doesn't make a token's name, ticker and decimals disappear for wallets and explorers. A
  malformed update or moving the reference NFT without its datum are examples of such mistakes.
- No tombstone rows or "metadata removed" state are needed. The storage model and read path stay as they are.
- The strict validation rules can be applied without the risk that one bad update removes a token's metadata.

### Negative

- The API can serve metadata that is no longer on chain. An issuer who removes the datum on purpose can't withdraw
  the metadata that way. They have to publish a new valid datum.
- The API and a chain explorer show different metadata for these tokens. Regression tests and manual checks against
  Koios must treat this as expected.

## Alternatives Considered

- **Serve only the current output's datum**: This follows the strictest reading of CIP-68, where the metadata is
  whatever is at the reference NFT now. It would need a tombstone row or a "current output" table, so that a datum
  that is missing or rejected clears the metadata. A single invalid update would then remove a token from the V2 API.
  We rejected it because the registry's purpose is to serve usable metadata, and these cases are almost always
  mistakes.
- **Store rejected datums and serve the newest one regardless**: This would undo the validation rules. Rejected datums
  are missing required fields, so they have nothing usable to serve.
