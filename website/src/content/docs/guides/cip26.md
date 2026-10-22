---
title: Register a CIP-26 token
description: Off-chain metadata in the Cardano token registry, and what this registry checks in it.
---

CIP-26 metadata is a JSON file in the [Cardano token registry](https://github.com/cardano-foundation/cardano-token-registry)
on GitHub, submitted by pull request. That repository explains how to create, sign and submit the file. This registry
**syncs it every hour** and serves it with `source: "CIP_26"`.

## The file

```json
{
  "subject": "<policy id><asset name hex>",
  "policy": "<policy script, CBOR hex>",
  "name":        { "value": "My Token", "sequenceNumber": 0, "signatures": [ … ] },
  "description": { "value": "What the token is for", "sequenceNumber": 0, "signatures": [ … ] },
  "ticker":      { "value": "MTK", "sequenceNumber": 0, "signatures": [ … ] },
  "decimals":    { "value": 6, "sequenceNumber": 0, "signatures": [ … ] },
  "url":         { "value": "https://example.com", "sequenceNumber": 0, "signatures": [ … ] },
  "logo":        { "value": "<base64 PNG>", "sequenceNumber": 0, "signatures": [ … ] }
}
```

## What this registry checks

| Rule | If broken |
|---|---|
| `name` and `description` are present | a `MANDATORY` `missing_value` error |
| texts have the lengths CIP-26 allows, e.g. a `name` of at most 50 characters | an `invalid_length` error, `MANDATORY` for `name` and `description` |
| `decimals` is a whole number in range | an `OPTIONAL` `out_of_range` or `wrong_type` error |
| `logo` is valid base64, at most 64 KiB once decoded | an `OPTIONAL` `invalid_base64` or `too_large` error |
| the file is named `<subject>.json`, after the subject inside it | **not indexed** (`filename_subject_mismatch`) |
| the subject is hex, 56 to 120 characters | **not indexed** (`unusable_subject`) |

An entry with errors is still served, with its `validation_errors`. Only the last two rules stop it from being indexed.

Check your file before you open the pull request: paste it into the [validator](../../validator/), CIP-26 tab, with the
file name you will use.

:::note
Signatures and the policy script are not verified by this registry. The token registry's own checks cover them.
:::
