---
title: API
description: The endpoints of the registry, V2 and V1, and the validate endpoint.
---

Every instance publishes its OpenAPI description at **`/apidocs`**.

## V2: CIP-26, CIP-68 and CIP-113 (current)

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v2/subjects/{subject}` | one token |
| POST | `/api/v2/subjects/query` | several tokens: `{"subjects": [...]}` |

Both take `query_priority`, `show_cips_details` and `validation`, see [Getting started](../../getting-started/#options).
To return only some properties, pass `property` (single subject, repeatable) or `"properties": [...]` (batch body), see
[only the properties you need](../../getting-started/#only-the-properties-you-need).

## Validate

Check a token before it exists. Nothing is stored. The [validator](../../validator/) page is a form over these
endpoints.

| Method | Path | Body |
|---|---|---|
| POST | `/api/v2/validate/cip26` | `{"mapping": <registry file>, "file_name": "<subject>.json"}` |
| POST | `/api/v2/validate/cip68` | `{"fields": {...}, "labels": [333]}` or `{"datum": "<CBOR hex>", "labels": [333, 444]}` |
| POST | `/api/v2/validate/cip113` | `{"datum": "<CBOR hex>"}` |

```bash
curl -X POST "$REGISTRY/api/v2/validate/cip68" -H 'Content-Type: application/json' -d '{
  "fields": { "name": "My Token", "description": "What it is for", "ticker": "MTK", "decimals": 6,
              "logo": "ipfs://bafy…", "version": 1 },
  "labels": [333]
}'
```

```json
{
  "standard": "CIP-68",
  "stored": true,
  "validation_errors": [],
  "served": [
    {
      "label": 333,
      "metadata": { "name": { "value": "My Token", "source": "CIP_68" }, "valid": true, "…": "…" },
      "validation_errors": [],
      "scopes": ["ALL", "MANDATORY_ONLY", "STRICT"]
    }
  ],
  "datum": "d8799fa5…ff",
  "notes": ["V2 serves a 333 or 444 subject only once its user token is minted …"]
}
```

| Field | Meaning |
|---|---|
| `stored` | whether the registry would index it |
| `dropped` | when not, the reason: `code` and `message` |
| `validation_errors` | everything that departs from the standard |
| `served` | how V2 would serve it: once for CIP-26, per label for CIP-68, with the `?validation=` scopes that return it |
| `datum` | CIP-68: the datum as given, or built from `fields` |
| `cip113` | CIP-113: the extension V2 would add |
| `notes` | what the answer assumes |

A request over 64 KiB is refused. A registry operator can switch the endpoint off.

## V1: CIP-26 only (deprecated)

The format of the original CIP-26 metadata server. **Deprecated:** kept for existing integrations, it serves CIP-26
only, without CIP-68, CIP-113 or validation errors. Use V2.

| Method | Path | Returns |
|---|---|---|
| GET | `/metadata/{subject}` | all properties of a token |
| GET | `/metadata/{subject}/properties/{property}` | one property |
| POST | `/metadata/query` | several tokens |

## Health

`/actuator/health/readiness` is `UP` once the instance has caught up with the chain and the CIP-26 registry.
