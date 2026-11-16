---
title: Mint a CIP-68 token
description: On-chain metadata for fungible (333) and rich fungible (444) tokens, and the rules the registry applies.
---

A CIP-68 token keeps its metadata **on chain**, in the inline datum of a separate **reference NFT** with the same policy
and name. Updating the metadata means spending that output and creating a new one with a new datum.

## The tokens

| Token | Asset name | Quantity |
|---|---|---|
| Reference NFT (label 100), holds the metadata | `000643b0` + name | exactly 1 |
| Fungible token (label 333), what people hold | `0014df10` + name | your supply |
| Rich fungible token (label 444), what people hold | `001bc280` + name | your supply |

The registry serves the **333 or 444 token**: its subject is the policy id followed by `0014df10…` or `001bc280…`. The
metadata comes from the reference NFT of the same policy and name, whether or not the user token was minted, so a `200`
does not mean the token exists: ask the chain. NFTs (label 222) are not served by this registry.

## The datum

```
Constr 0 [ metadata : Map<Bytes, Data>, version : Int, extra : Data (optional) ]
```

| Field | 333 fungible | 444 rich fungible | Format |
|---|---|---|---|
| `name` | **required** | **required** | UTF-8 text |
| `description` | **required** | optional | UTF-8 text |
| `image` | – | **required** | URI: `ipfs://`, `https://`, `ar://` or `data:` |
| `mediaType` | – | optional | e.g. `image/png` |
| `logo` | optional | – | URI: `ipfs://`, `https://`, `ar://` or `data:` |
| `ticker` | optional | optional | UTF-8 text |
| `url` | optional | optional | UTF-8 text |
| `decimals` | optional | optional | integer |
| `version` | **required** | **required** | 1 to 4 |

A rich fungible token's `image` is served as its `logo`, so wallets show it without special handling.

The ledger limits a byte string in a datum to 64 bytes. A longer value is written in CBOR chunks, which the registry reads as one value. A text field written as a list of byte strings is joined and served, with a `chunked_text` error; a value that is not a byte string at all is a `wrong_type` error, and is left out.

## What happens when a rule is broken

The datum is **still indexed and served**, with `validation_errors` that name the field and the rule:
a missing required field is a `MANDATORY` error, a `logo` that is not an allowed URI is an `OPTIONAL` one and is kept
as written, and a `version` that is not an integer, does not fit a 64-bit integer or is outside 1 to 4 is a `MANDATORY`
`invalid_version`. A datum that cannot be decoded, that is not CIP-68, or that belongs only to an NFT (label 222) is not indexed at all; everything else is served, with its errors.

If your reference NFT is paired with several user tokens (a 333 and a 444), the datum is checked against each, and each
subject is served with the errors of its own label.

## Before you mint

Use the [validator](../../validator/), CIP-68 tab: enter your token's fields, choose the labels, and you get the errors
the registry would report and **the datum to put on your reference NFT**, ready to use with your minting tool.

## After you mint

The registry indexes the chain as it goes. Your token appears once the block with the reference NFT is indexed; an update appears once the block with the new datum is.
