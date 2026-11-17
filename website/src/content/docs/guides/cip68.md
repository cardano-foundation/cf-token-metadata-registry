---
title: Mint a CIP-68 token
description: On-chain metadata for fungible (333) and rich fungible (444) tokens, and the rules the registry applies.
---

A CIP-68 token keeps its metadata **on chain**, in the inline datum of a separate **reference NFT** with the same policy ID and the same asset-name bytes after the label prefix. Updating the metadata means spending that output and creating a new one with a new datum.

## The tokens

| Token | Asset name | Quantity |
|---|---|---|
| Reference NFT (label 100), holds the metadata | `000643b0` + name | exactly 1 |
| Fungible token (label 333), what people hold | `0014df10` + name | your supply |
| Rich fungible token (label 444), what people hold | `001bc280` + name | your supply |

The registry serves the **333 or 444 token**: its subject is the policy id followed by `0014df10…` or `001bc280…`. The metadata comes from the reference NFT with the same policy ID and the same asset-name bytes after the label prefix, whether or not the user token was minted, so a `200` does not mean the token exists: ask the chain. NFTs (label 222) are not served by this registry.

## The datum

```
Constr 0 [ metadata : Map<Bytes, Data>, version : Int, extra : Data ]
```

The `extra` field is required. When you have no additional data, use Unit (`Constr 0 []`).

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

CIP-68 defines text fields as UTF-8 byte strings. The registry also accepts a non-empty list of byte strings: it joins the bytes before decoding the text, without reporting an error for the list form. Invalid UTF-8 is kept as hex with a `not_utf8` error. Other value types are left out with a `wrong_type` error.

## What happens when a rule is broken

With the default `validation=ALL`, indexed metadata is returned with `validation_errors` that name the field and the rule: a missing required field is a `MANDATORY` error, a `logo` that is not an allowed URI is an `OPTIONAL` one and is kept as written, and a `version` that is not an integer, does not fit a 64-bit integer or is outside 1 to 4 is a `MANDATORY` `invalid_version`. A datum that cannot be decoded, that is not CIP-68, or that belongs only to an NFT (label 222) is not indexed at all.

If your reference NFT is paired with several user tokens (a 333 and a 444), the datum is checked against each, and each subject is served with the errors of its own label.

## Before you mint

Build the reference NFT datum with your minting tool according to the [CIP-68 specification](https://cips.cardano.org/cip/CIP-0068). Check the required fields for your user-token label against the table above. This registry reads the result; it does not mint the tokens.

## After you mint

The registry indexes the chain as it goes. Your token appears once the block with the reference NFT is indexed; an update appears once the block with the new datum is.
