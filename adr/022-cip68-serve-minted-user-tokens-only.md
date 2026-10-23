# ADR-022: Serve a CIP-68 333 or 444 Subject Only When Its User Token Was Minted

## Status

Rejected. The design was built (a port of bloxbean/yaci-store#1294) and removed before release. Issue #112 is not
fixed: the behaviour it describes is kept, and documented.

## Date

2026-10-23

## Context

A CIP-68 token's metadata is the datum of its reference NFT (`000643b0` + name). The V2 API maps a fungible (333,
`0014df10` + name) or rich fungible (444, `001bc280` + name) subject to that reference NFT and serves its datum.

The reference NFT says nothing about which user tokens exist, so a reference NFT answers under both its 333 and its 444
subject, whether or not that token was ever minted. Of the 999 subjects of the mainnet CIP-68 regression baseline, 334
were never minted as 333 (324 are NFTs, 5 are rich fungible tokens, 5 have no user token at all).

## Proposal

Record the 333 and 444 user tokens seen on chain in a table (`cip68_user_token`, written by the CIP-68 listener with an
upsert that keeps the smallest slot, deleted on rollback), and serve such a subject from CIP-68 only when its token was
seen. A subject never minted would have nothing from CIP-68: its CIP-26 entry, if any, still served, and otherwise a 404.

It worked: on preprod every seen token answered 200 and every made-up subject 404.

## Decision

**Keep the lookup as it is.** A 333 or 444 subject is served from its reference NFT whether or not its user token was
minted. A `200` says that metadata is attached to the subject, not that the token exists; checking that a token exists
is for chain data, not for a metadata API.

Why:
- **The metadata of a token that no longer exists is still wanted.** Explorers and wallets show historical
  transactions, where a burned token's name, logo and decimals still matter. A rule that serves only existing tokens
  would take that away, and an index of outputs cannot get it exactly right: a token minted and burned in the same
  transaction never appears in an output.
- **Wallets are not affected.** A wallet asks for the tokens it holds, and a token never minted is in nobody's wallet.
- **Impersonation is not possible.** The reference NFT is minted under the token's own policy, which only its owner can
  mint under, so nobody can publish metadata for another issuer's token, minted or not.
- **The cost is out of proportion.** A table, a migration, a write per block, rollback handling, a lookup per request
  and a resync from scratch, to change a `200` into a `404` for subjects nobody holds.

## Consequences

- A made-up 333 or 444 subject of an existing reference NFT answers `200`, including the 333 and 444 subjects of an
  NFT (222). This is documented, not fixed.
- Such a subject is checked against the rules of the label it is asked for (ADR-019). A 444 token asked for as 333 may
  therefore report a missing description, which a client asking for the real token does not see.
- A CIP-113 policy with a CIP-68 reference NFT has a companion (ADR-020), whatever its user tokens.
- No table, migration or resync is needed.

## Alternatives Considered

- **The proposal above:** rejected for the reasons in the decision.
- **Infer the label from the datum:** a datum that follows CIP-68 reads the same as 333 or 444.
- **Ask Koios or the node at request time:** an external dependency, and a round trip, on every query.
- **Enable Yaci Store's `assets` module:** it stores every mint on the chain, millions of rows on mainnet.
