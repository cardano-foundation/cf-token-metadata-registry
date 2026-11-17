---
title: Overview
description: What the Cardano Token Metadata Registry is, which standards it serves, and how to use it.
---

The **Cardano Token Metadata Registry** answers one question about Cardano native tokens: *what is it called, and how should it be shown?* It combines CIP-26 and CIP-68 metadata into one response. For programmable tokens, it also returns information from the CIP-113 on-chain registry.

| Standard | Where the metadata lives | Served as |
|---|---|---|
| [CIP-26](https://cips.cardano.org/cip/CIP-0026) | a JSON file in the [Cardano token registry](https://github.com/cardano-foundation/cardano-token-registry) on GitHub | `source: "CIP_26"` |
| [CIP-68](https://cips.cardano.org/cip/CIP-0068) | the inline datum of a *reference NFT* on chain; fungible tokens (label 333) and rich fungible tokens (label 444) | `source: "CIP_68"` |

[CIP-113 programmable tokens](../guides/cip113/) have a separate extension: `type: "PROGRAMMABLE"` and `extensions.cip113` identify the policy's programmable-token scripts. Their names, tickers and logos still come from the two metadata sources above.

- **Show tokens** in a wallet, an explorer or a DEX: one request per token, or a batch. [Getting started](../getting-started/)
- **Find out why a token looks wrong**: every response lists its `validation_errors`, naming the field and the rule. [Validation errors](../concepts/validation-errors/)
- **[Run your own registry](../guides/self-hosting/)** to size the service for your workload and upgrade on your schedule.

## The API

Query `/api/v2/subjects/...` for one token or a batch, with all properties or only those you name. [Getting started](../getting-started/) walks through the requests; the [API reference](../api/) documents each operation.

## How a token is identified

A token is identified by its **subject**: the policy id (56 hex characters) followed by the asset name in hex.

## How strict the registry is

The registry serves tokens **as they are written**, even when they break their standard, and reports validation errors. A wallet decides how strict to be:

- `?validation=ALL` (the default): every indexed token, with its errors.
- `?validation=MANDATORY_ONLY`: only tokens with no error in a field their standard requires.
- `?validation=STRICT`: only tokens with no reported validation errors.

Which fields are mandatory, how to choose a level, and why a token may not appear at all: [A permissive registry](../concepts/permissive/).
