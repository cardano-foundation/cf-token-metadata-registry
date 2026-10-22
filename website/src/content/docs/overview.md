---
title: Overview
description: What the Cardano Token Metadata Registry is, which standards it serves, and how to use it.
---

The **Cardano Token Metadata Registry** answers one question for any Cardano native token: *what is it called, and how
should it be shown?* It collects metadata from the three ways a token can carry it, and serves them as one answer.

| Standard | Where the metadata lives | Served as |
|---|---|---|
| [CIP-26](https://cips.cardano.org/cip/CIP-0026) | a JSON file in the [Cardano token registry](https://github.com/cardano-foundation/cardano-token-registry) on GitHub | `source: "CIP_26"` |
| [CIP-68](https://cips.cardano.org/cip/CIP-0068) | the inline datum of a *reference NFT* on chain; fungible tokens (label 333) and rich fungible tokens (label 444) | `source: "CIP_68"` |
| [CIP-113](https://github.com/cardano-foundation/CIPs/pull/444) | an on-chain registry of *programmable tokens* and the scripts that govern them | `type: "PROGRAMMABLE"` and `extensions.cip113` |

## What you can do with it

- **Show tokens** in a wallet, an explorer or a DEX: one request per token, or a batch.
  [Getting started](../getting-started/)
- **Check a token before you mint it**: the [validator](../validator/) runs the registry's own rules on metadata that does
  not exist yet, and builds a CIP-68 datum for you.
- **Find out why a token looks wrong**: every response lists its `validation_errors`, naming the field and the rule.
  [Validation errors](../concepts/validation-errors/)
- **Run your own instance**, for example to index a test network. [Run your own registry](../reference/self-hosting/)

## How a token is identified

A token is identified by its **subject**: the policy id (56 hex characters) followed by the asset name in hex. For a
CIP-68 token, that is the subject of the token people hold, starting with its label prefix after the policy id:
`0014df10` for a fungible token, `001bc280` for a rich fungible token.

## How strict the registry is

The registry serves tokens **as they are written**, even when they break their standard, and says what is wrong. A
wallet decides how strict to be:

- `?validation=ALL` (the default): every token, with its errors.
- `?validation=MANDATORY_ONLY`: only tokens with no error in a field their standard requires.
- `?validation=STRICT`: only tokens with no error at all.

Which fields are mandatory, how to choose a level, and why a token may not appear at all:
[A permissive registry](../concepts/permissive/).
