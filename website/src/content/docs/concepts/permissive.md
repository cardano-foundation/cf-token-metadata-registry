---
title: A permissive registry
description: Why the registry serves tokens that break their standard, which fields are mandatory, how clients choose how strict to be, and why a token may not appear at all.
---

The registry is **permissive by design**. It indexes token metadata **as it is written**, also when it breaks its
standard, and says exactly what is wrong. It does not repair anything, guess anything, or hide anything on your behalf.
Deciding what to do with a token that does not comply is left to **you**, the client of the API: a wallet, an explorer, a
DEX, an indexer.

## Why

- **A dropped token is invisible.** Its owner cannot tell why it does not show, and a client cannot decide for itself. A
  token whose metadata misses a description, or whose logo is a bare IPFS hash, is still a real token that people hold.
- **Clients have different needs.** An explorer wants to show everything that exists. A wallet may want only tokens with
  the fields it needs to display. A curated list may want only tokens that follow their standard to the letter. One
  rule in the registry cannot serve all of them.
- **What is written is what you get.** The registry never states something that is not on chain or in the CIP-26
  registry. A value that breaks a rule is kept as written and reported, not "fixed".

## You choose how strict to be

Every response carries `validation_errors`, and `metadata.valid` is `true` exactly when there are none. To have the
registry filter for you, use the **`validation`** parameter:

| `?validation=` | Returns | Suits |
|---|---|---|
| `ALL` (default) | every token, with its errors | explorers, indexers, anything that wants to see everything |
| `MANDATORY_ONLY` | only tokens with no error in a field their standard **requires** | wallets and apps that need a name and the required fields to show a token |
| `STRICT` | only tokens with **no** error at all | curated lists, listings that want full compliance |

```bash
curl "$REGISTRY/api/v2/subjects/$SUBJECT?validation=MANDATORY_ONLY"
```

A token the chosen level does not accept is a `404`, or is left out of a batch. The errors of a token that is returned
are never filtered: you always see everything that is wrong with it.

Or take everything (`ALL`) and decide per field yourself: each error names the `field_name`, whether the field is
`MANDATORY` or `OPTIONAL`, and a stable `code`. See [validation errors](../validation-errors/).

## Mandatory and optional fields

What each standard **requires** (an error there is `MANDATORY`) and what it **allows** (an error there is `OPTIONAL`):

| Field | CIP-26 | CIP-68, label 333 (fungible) | CIP-68, label 444 (rich fungible) |
|---|---|---|---|
| `name` | **mandatory** | **mandatory** | **mandatory** |
| `description` | **mandatory** | **mandatory** | optional |
| `image` | – | – | **mandatory**, a URI (`ipfs://`, `https://`, `ar://`, `data:`) |
| `version` | – | **mandatory**, 1 to 4 | **mandatory**, 1 to 4 |
| `ticker` | optional | optional | optional |
| `url` | optional | optional | optional |
| `decimals` | optional | optional | optional |
| `logo` | optional, base64 PNG up to 64 KiB | optional, a URI (`ipfs://`, `https://`, `ar://`, `data:`) | – (the `image` is served as the logo) |
| other properties | – | optional | optional |

A missing or broken **mandatory** field is a `MANDATORY` error: the token is still served under `ALL`, and hidden by
`MANDATORY_ONLY` and `STRICT`. A broken **optional** field is an `OPTIONAL` error: the token is hidden only by `STRICT`.

A CIP-68 reference NFT paired with several user tokens (a 333 and a 444) is checked against each label, and each
subject is served with the errors of its own label.

## When the newest metadata is broken

The registry serves the **newest** metadata. If a CIP-68 token is updated with a datum that breaks the standard, the
new datum is served, with its errors; the registry does not fall back to an older, valid one. Moving the reference NFT
to an output **without** a datum is not an update: the last datum stays.

## Why a token may not appear

A `404` for a token that exists on chain has one of these reasons, from the most to the least common:

1. **Not indexed yet.** The registry indexes the chain as it goes, and syncs the CIP-26 registry every hour. A token
   minted a moment ago, or a CIP-26 entry merged a moment ago, may not be there yet.
2. **No metadata.** The token has neither a CIP-26 entry nor a CIP-68 datum. There is nothing to serve.
3. **The CIP-68 user token was never minted.** The registry serves a 333 or 444 subject only once that token exists; a
   reference NFT alone is not a token anyone holds. NFTs (label 222) are not served.
4. **A CIP-113 token without metadata.** A programmable token is served only with its CIP-26 or CIP-68 metadata: a
   registry node has no name to show.
5. **Your `validation` level hides it.** With `MANDATORY_ONLY` or `STRICT`, a token with errors is a `404`. Ask with
   `ALL` to see it and its errors.
6. **It is in a very bad state, or looks like an attack on the registry.** A few things are not indexed at all,
   because they cannot be read, or because their only plausible purpose is to harm the indexer:

| What | Why it is not indexed |
|---|---|
| a CIP-68 datum that cannot be decoded | there is nothing to read |
| a CIP-68 datum nested so deeply it cannot be decoded | a datum built to exhaust the decoder |
| a datum that is not CIP-68 at all | it is not token metadata; it is ignored |
| a CIP-26 file named after another subject than the one inside it | it would make two tokens claim one name |
| a CIP-26 subject that is not hex, or not 56 to 120 characters | it cannot identify a token |
| a CIP-26 file that cannot be parsed, or that has no history in the registry | there is nothing reliable to read |

Some things are dropped **in part**, so the rest of the token is still served:

| What | What happens |
|---|---|
| a CIP-68 property nested more than 100 levels | that property is left out, with a `too_deep` error; the rest of the datum is kept |
| a CIP-26 logo far beyond any real size (over about 1 MiB) | the logo is not stored; the entry is |

The registry records each of the items in the first table, with the reason, except a datum that is not CIP-68 at all,
which is ignored. Registry operators can therefore tell a token owner why their token does not appear.

Before you mint or submit, the [validator](../../validator/) tells you which of these would apply, with the same rules.

## The decisions behind this

The design is recorded in the project's architecture decision records, for those who want the full reasoning:

- [ADR-019](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/develop/adr/019-validation-errors-reported-not-enforced.md): validation errors are reported, not enforced
- [ADR-020](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/develop/adr/020-invalid-token-table.md): what cannot be stored is recorded, CIP-113 included
- [ADR-022](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/develop/adr/022-cip68-serve-minted-user-tokens-only.md): a CIP-68 token is served only once its user token is minted
- [ADR-023](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/develop/adr/023-validate-endpoint.md): the validate endpoint
