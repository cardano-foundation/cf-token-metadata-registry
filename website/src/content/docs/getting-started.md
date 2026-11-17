---
title: Getting started
description: Query a token's metadata with the V2 API, one token or many at a time.
---

All you need is the **subject** of a token: its policy id followed by its asset name, in hex. The examples use `$REGISTRY` for the address of a registry instance: `https://v2.tokens.cardano.org` for mainnet, `https://preprod.tokens.cardano.org` for preprod.

The API combines CIP-26 and CIP-68 metadata and reports validation errors. CIP-113 programmable tokens also carry script information in a separate extension. You can try each operation from the [API reference](../api/) using the **Test request** button.

## One token

```bash
REGISTRY="https://v2.tokens.cardano.org"
SUBJECT="<your-token-subject>" # replace with your token's subject

curl "$REGISTRY/api/v2/subjects/$SUBJECT"
```

```json
{
  "subject": {
    "subject": "…",
    "type": "NATIVE",
    "metadata": {
      "name":        { "value": "My Token", "source": "CIP_68" },
      "description": { "value": "What the token is for", "source": "CIP_68" },
      "ticker":      { "value": "MTK", "source": "CIP_68" },
      "decimals":    { "value": 6, "source": "CIP_68" },
      "logo":        { "value": "ipfs://…", "source": "CIP_68" },
      "valid": true
    },
    "validation_errors": []
  },
  "queryPriority": ["CIP_68", "CIP_26"]
}
```

Each property says where it came from (`source`). `valid` is `true` when the token has no validation errors. A token the registry knows nothing about is a `404`.

## Many tokens

```bash
curl -X POST "$REGISTRY/api/v2/subjects/query" \
  -H 'Content-Type: application/json' \
  -d '{"subjects": ["<subject 1>", "<subject 2>"]}'
```

The response holds the tokens the registry knows, in `subjects`. Unknown subjects are left out.

## Only the properties you need

By default every property is returned. To get only some of them, name them, which keeps responses small when you show, say, only a name and a ticker:

```bash
# one token: repeat the parameter
curl "$REGISTRY/api/v2/subjects/$SUBJECT?property=name&property=ticker&property=decimals"

# many tokens: list them in the body
curl -X POST "$REGISTRY/api/v2/subjects/query" \
  -H 'Content-Type: application/json' \
  -d '{"subjects": ["<subject 1>", "<subject 2>"], "properties": ["name", "ticker", "decimals"]}'
```

The properties are `name`, `description`, `ticker`, `decimals`, `logo`, `url` and `version`. Any combination works; you do not need to ask for `name` or `description`.

Requesting specific properties also narrows the validation errors returned. For example, asking for `ticker` does not report a missing `description`. The `validation` parameter is applied after this filtering.

## Options

| Parameter | Effect |
|---|---|
| `property` | return only these properties, e.g. `?property=name&property=ticker`; see [above](#only-the-properties-you-need) |
| `query_priority` | which standard wins when both have a property; `CIP_68,CIP_26` by default |
| `show_cips_details` | add each standard's own metadata under `standards.cip26` and `standards.cip68` |
| `validation` | `ALL` (default), `MANDATORY_ONLY` or `STRICT`: how strict to be, see [validation errors](../concepts/validation-errors/) |

## Programmable tokens

A token registered as a [CIP-113](../guides/cip113/) programmable token comes back with `"type": "PROGRAMMABLE"` and script information under `extensions.cip113`. Its name, ticker and logo come from CIP-26 or CIP-68 metadata; CIP-113 does not participate in `query_priority`.

Without companion metadata, the default `validation=ALL` still returns `200`, with a `MANDATORY` `missing_value` error on `companion_metadata`. `MANDATORY_ONLY` and `STRICT` return `404` for that subject or omit it from a batch.
