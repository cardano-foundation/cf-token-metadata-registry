---
title: Getting started
description: Query a token's metadata with the V2 API, one token or many at a time.
---

All you need is the **subject** of a token: its policy id followed by its asset name, in hex. The examples use
`$REGISTRY` for the address of a registry instance: `https://tokens.cardano.org` for mainnet,
`https://preprod.tokens.cardano.org` for preprod.

:::note[Which API]
**V2** (`/api/v2/...`) is the current API: it serves CIP-26, CIP-68 and CIP-113, with validation errors. **V1**
(`/metadata/...`) is **deprecated**: it serves CIP-26 only, in the format of the original CIP-26 metadata server, and is
kept for existing integrations. Use V2 for anything new.
:::

## One token

```bash
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
  }
}
```

Each property says where it came from (`source`). `valid` is `true` when the token has no validation errors. A token
the registry knows nothing about is a `404`.

## Many tokens

```bash
curl -X POST "$REGISTRY/api/v2/subjects/query" \
  -H 'Content-Type: application/json' \
  -d '{"subjects": ["<subject 1>", "<subject 2>"]}'
```

The response holds the tokens the registry knows, in `subjects`. Unknown subjects are left out.

## Only the properties you need

By default every property is returned. To get only some of them, name them, which keeps responses small when you show,
say, only a name and a ticker:

```bash
# one token: repeat the parameter
curl "$REGISTRY/api/v2/subjects/$SUBJECT?property=name&property=ticker&property=decimals"

# many tokens: list them in the body
curl -X POST "$REGISTRY/api/v2/subjects/query" \
  -H 'Content-Type: application/json' \
  -d '{"subjects": ["<subject 1>", "<subject 2>"], "properties": ["name", "ticker", "decimals"]}'
```

The properties are `name`, `description`, `ticker`, `decimals`, `logo`, `url` and `version`. Any combination works; you
do not need to ask for `name` or `description`.

The `validation_errors` then cover only the properties you asked for: a client that asks only for the `ticker` is not
told that the description is missing, and `?validation=` judges the token on those properties only.

## Options

| Parameter | Effect |
|---|---|
| `property` | return only these properties, e.g. `?property=name&property=ticker`; see [above](#only-the-properties-you-need) |
| `query_priority` | which standard wins when both have a property; `CIP_68,CIP_26` by default |
| `show_cips_details` | add each standard's own metadata under `standards.cip26` and `standards.cip68` |
| `validation` | `ALL` (default), `MANDATORY_ONLY` or `STRICT`: how strict to be, see [validation errors](../concepts/validation-errors/) |

## Programmable tokens

A token registered as a [CIP-113](../guides/cip113/) programmable token comes back with `"type": "PROGRAMMABLE"` and
its rules under `extensions.cip113`. Everything else is the same. Its name, ticker and logo come from CIP-26 or CIP-68 metadata; without any, it is still a `200`, with a `MANDATORY` error on `companion_metadata`.

## The V1 API (deprecated)

`GET /metadata/{subject}`, `GET /metadata/{subject}/properties/{property}` and `POST /metadata/query` serve **CIP-26
only**, in the format of the original CIP-26 metadata server. V1 is **deprecated** and kept for existing integrations:
it does not serve CIP-68 or CIP-113 tokens and has no validation errors. Use V2.
