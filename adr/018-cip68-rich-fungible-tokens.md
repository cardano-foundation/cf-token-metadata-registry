# ADR-018: Serve CIP-68 Rich Fungible Tokens (444)

## Status

Accepted. Amended by ADR-019 (2026-10-21): what a 444 datum lacks is now a validation error, not a reason to skip it,
and its image is stored and served as written.

## Date

2026-10-19

## Context

CIP-68 defines three user-token labels that point to a reference NFT (prefix `000643b0`) holding the metadata datum:

| Label | Prefix | Token | Required fields |
|---|---|---|---|
| 222 | `000de140` | NFT | `name`, `image` |
| 333 | `0014df10` | Fungible token (FT) | `name`, `description` |
| 444 | `001bc280` | Rich fungible token (RFT) | `name`, `image` |

From CIP-68 support (#21, v1.1.0) until this change, the V2 API mapped only 333 subjects to their reference NFT. The
indexer stored only datums with a description, because the `description` column was `NOT NULL`, so most 444 datums
were never stored.

On mainnet (2026-10-19), there are 1,692 label-444 assets under 93 policies. Only 484 have a description; 1,679 have an
image. They are editions and collectibles with a supply above 1, such as music releases and art prints. We decided to
serve them.

## Decision

The V2 API serves 444 subjects next to 333 subjects. NFTs (222) are still not served.

1. **Mapping**: a `001bc280` subject maps to its reference NFT, like a `0014df10` subject
   (`Cip68FungibleTokenService.getReferenceNftSubject`). It does so whether or not that user token was minted, so a
   reference NFT answers under both its 333 and its 444 subject (ADR-022 records why this is kept).
2. **Image as logo**: CIP-68 gives an RFT no `logo`. Its picture is the required `image`, which is served, for a
   `001bc280` subject whatever label its datum was stored or guessed as (ADR-019), as
   `metadata.logo` (source `CIP_68`), so wallets show it with no change on their side. For CIP-68 FTs, `logo` is
   already a URI, so the format matches. Since ADR-019 the image is stored (`image`, with `media_type`, migration V7)
   and served as written, even when it is not a URI with a scheme CIP-68 allows; a validation error says so. A row
   stored before V7 has no image column, so it is read from the stored datum.
3. **Description optional**: CIP-68 requires only `name` and `image` for a 444 datum.
   - V6 makes `metadata_reference_nft.description` nullable.
   - The listener stores a valid datum without a description when a 444 user token is paired with it in the
     transaction. It does the same when the datum updates a reference NFT that already has a stored version: an
     update without the user token has its label inferred from the datum, and an RFT without `decimals` reads as a
     222 NFT. Any other valid datum without a description is a 222 NFT and is still skipped.
   - Since ADR-019, validity comes from the validation errors: a 444 datum without a description has no error (the
     description is optional for it), so it is `valid`. A 333 datum without one has a `MANDATORY` error.
4. **V1 is unchanged**: V1 serves CIP-26 only. An RFT appears there only if it is registered in CIP-26, and none is on
   mainnet today.

## Consequences

### Positive

- RFTs get a name, an image and, when present, a description and decimals from the registry, the same way FTs do.
- No API schema change: an RFT fills the same `metadata` fields as an FT.

### Negative

- RFTs without a description that were skipped before this change are served only after their datum is indexed
  again. On mainnet that means re-indexing from the first RFT (2023-07-01); 471 RFTs were already stored and are
  served right away.
- `logo` now means "logo" for an FT and "image" for an RFT. A client that needs the difference can tell them apart by
  the subject's prefix.
- An RFT request parses the stored datum to get the image. The datum is small, so the cost is negligible.

## Alternatives Considered

- **A new `metadata.image` property**: more explicit, but wallets would show no picture until they read the new
  field.
- **Serve only RFTs with a description**: no schema change and no re-index, but CIP-68-valid RFTs would be left out.
- **Store the image in its own column**: the stored rows would have no image until re-indexed. Reading it from the
  stored datum serves them right away.
