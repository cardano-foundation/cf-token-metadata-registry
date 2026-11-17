---
title: Validation errors
description: How the registry reports what departs from a token's standard, and how to filter by it.
---

The registry serves tokens **as they are written** and reports errors found by its checks in `validation_errors`. Why, which fields are mandatory, and when a token does not appear at all: [A permissive registry](../permissive/).

## An error

```json
{
  "cip": "CIP-68",
  "sub_standard": "333",
  "field_name": "description",
  "field_category": "MANDATORY",
  "code": "missing_value",
  "error": "description field is missing a mandatory value"
}
```

| Field | Meaning |
|---|---|
| `cip` | the standard whose rule is broken: `CIP-26`, `CIP-68` or `CIP-113` |
| `sub_standard` | for CIP-68, the label the rule was checked for (`333`, `444`); absent when the rule does not depend on a label |
| `field_name` | the field, e.g. `description`, `logo`, `version`, `properties.owner`, `companion_metadata` |
| `field_category` | `MANDATORY` if the standard requires the field, `OPTIONAL` otherwise |
| `code` | a stable code for programs, see below |
| `error` | a message for people |

`metadata.valid` is `true` exactly when there are no errors.

## Choosing how strict to be

| `?validation=` | Returns |
|---|---|
| `ALL` (default) | every token, with its errors |
| `MANDATORY_ONLY` | tokens with no `MANDATORY` error: problems only in optional fields are tolerated |
| `STRICT` | tokens with no reported validation errors, whether `MANDATORY` or `OPTIONAL` |

A token that the chosen level does not accept is a `404`, or is left out of a batch. The errors of a token that is returned are never filtered by the `validation` level.

With `?property=`, validation errors are scoped to the requested properties before the registry calculates `metadata.valid` and applies `MANDATORY_ONLY` or `STRICT`. For example, `?property=ticker&validation=STRICT` can return a token whose description is missing, provided no reported error affects the ticker.

Errors that concern all metadata, such as `no_nested_entry` and missing CIP-113 `companion_metadata`, remain regardless of `?property=`. An `image` error also applies when `logo` is requested, since rich fungible tokens expose their image as the logo.

## Error codes

| Code | Meaning |
|---|---|
| `missing_value` | a field the standard requires is missing or empty: for CIP-68 the `version` and, as the label requires, `name`, `description` (333) and `image` (444); for CIP-26 `name` and `description`; for CIP-113 the token's CIP-26 or CIP-68 metadata (`companion_metadata`) |
| `wrong_type` | the value has a type the standard does not allow, e.g. a number where text is expected; a value that cannot be kept is left out |
| `invalid_uri_scheme` | an `image` or `logo` is not a URI with a scheme CIP-68 allows (`https`, `ipfs`, `ar`, `data`); the value is kept as written |
| `invalid_length` | a CIP-26 text is shorter or longer than CIP-26 allows, e.g. a `name` over 50 characters |
| `invalid_value` | a CIP-26 value breaks another rule of the CIP-26 validator |
| `out_of_range` | a number is out of range: `decimals` |
| `invalid_version` | the CIP-68 `version` is there but unusable: not an integer, not a 64-bit integer, or outside 1 to 4 |
| `plutus_constructor` | a property holds a Plutus constructor, which CIP-68 metadata does not allow; it is kept as `{constructor, fields}` |
| `not_utf8` | a text is not valid UTF-8; it is kept as hex |
| `invalid_files_entry` | an entry of `files` breaks the CIP-68 definition, e.g. no `mediaType` or `src` |
| `no_nested_entry` | a nested datum (`"721"`) has no entry for the token |
| `too_deep` | a property is nested more than 100 levels and is left out; the rest is kept |
| `unsupported_key` | a map key has no text form (a list or a map) and is left out |
| `key_collision` | two keys read the same, e.g. the text `1` and the integer `1`; one is kept |
| `invalid_base64` | a CIP-26 `logo` is not valid base64 |
| `too_large` | a CIP-26 `logo` is over 64 KiB once decoded; over 1,400,000 base64 characters it is not stored at all |

New codes may be added.

## What is not indexed at all

Some inputs cannot be indexed. The registry records these reasons in its internal `invalid_token` table for operators; they are not subject responses or a public error-query API:

| Code | When |
|---|---|
| `filename_subject_mismatch` | a CIP-26 file named after another subject than the one inside it |
| `unusable_subject` | a CIP-26 subject that is not hex, or not 56 to 120 characters |
| `datum_undecodable`, `datum_too_deep_to_decode` | a CIP-68 datum that cannot be decoded |
| `registry_node_unparseable` | a CIP-113 registry node that cannot be read |

A datum that is not CIP-68 at all is simply ignored, rather than recorded in that table.
