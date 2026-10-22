---
title: Getting started
description: Query a token's metadata with the V2 API, one token or many at a time.
---

All you need is the **subject** of a token: its policy id followed by its asset name, in hex. The examples use
`$REGISTRY` for the address of a registry instance.

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

## Options

| Parameter | Effect |
|---|---|
| `property` | return only these properties, e.g. `?property=name&property=ticker` |
| `query_priority` | which standard wins when both have a property; `CIP_68,CIP_26` by default |
| `show_cips_details` | add each standard's own metadata under `standards.cip26` and `standards.cip68` |
| `validation` | `ALL` (default), `MANDATORY_ONLY` or `STRICT`: how strict to be, see [validation errors](../concepts/validation-errors/) |

## Programmable tokens

A token registered as a [CIP-113](../guides/cip113/) programmable token comes back with `"type": "PROGRAMMABLE"` and
its rules under `extensions.cip113`. Everything else is the same.

## The V1 API

`GET /metadata/{subject}` and `POST /metadata/query` serve **CIP-26 only**, in the format of the original CIP-26
metadata server. Use V2 for new integrations.
