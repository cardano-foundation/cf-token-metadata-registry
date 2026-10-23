---
title: Validation errors
description: How the registry reports what departs from a token's standard, and how to filter by it.
---

The registry serves tokens **as they are written**. When a token breaks its standard, it is still served, with a list of
`validation_errors` saying what is wrong. Nothing is fixed or guessed for you, and nothing is hidden unless you ask.
Why, which fields are mandatory, and when a token does not appear at all: [A permissive registry](../permissive/).

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
| `cip` | the standard whose rule is broken: `CIP-26` or `CIP-68` |
| `sub_standard` | for CIP-68, the label the rule was checked for (`333`, `444`); absent when the rule does not depend on a label |
| `field_name` | the field, e.g. `description`, `logo`, `version`, `properties.owner` |
| `field_category` | `MANDATORY` if the standard requires the field, `OPTIONAL` otherwise |
| `code` | a stable code for programs, see below |
| `error` | a message for people |

`metadata.valid` is `true` exactly when there are no errors.

## Choosing how strict to be

| `?validation=` | Returns |
|---|---|
| `ALL` (default) | every token, with its errors |
| `MANDATORY_ONLY` | tokens with no `MANDATORY` error: problems only in optional fields are tolerated |
| `STRICT` | tokens with no error at all |

A token that the chosen level does not accept is a `404`, or is left out of a batch. The errors of a token that is
returned are never filtered.

With `?property=`, the errors are about the requested properties only: a client that asks only for the `ticker` is not
told that the description is missing.

## Error codes

| Code | Meaning |
|---|---|
| `missing_value` | a field the standard requires is missing or empty: for CIP-68 the `version` and, as the label requires, `name`, `description` (333) and `image` (444); for CIP-26 `name` and `description` |
| `wrong_type` | the value has a type the standard does not allow, e.g. a number where text is expected; a value that cannot be kept is left out |
| `invalid_uri_scheme` | an `image` or `logo` is not a URI with a scheme CIP-68 allows (`https`, `ipfs`, `ar`, `data`); the value is kept as written |
| `invalid_length` | a CIP-26 text is shorter or longer than CIP-26 allows, e.g. a `name` over 50 characters |
| `invalid_value` | a CIP-26 value breaks another rule of the CIP-26 validator |
| `out_of_range` | a number is out of range: `decimals`, or a `version` that does not fit a 64-bit integer |
| `undefined_version` | the CIP-68 `version` is not one CIP-68 defines (1 to 4); the datum is still read |
| `plutus_constructor` | a property holds a Plutus constructor, which CIP-68 metadata does not allow; it is kept as `{constructor, fields}` |
| `not_utf8` | a text is not valid UTF-8; it is kept as hex |
| `invalid_files_entry` | an entry of `files` breaks the CIP-68 definition, e.g. no `mediaType` or `src` |
| `no_nested_entry` | a nested (version 4, `"721"`) datum has no entry for the token |
| `too_deep` | a property is nested more than 100 levels and is left out; the rest is kept |
| `unsupported_key` | a map key has no text form (a list or a map) and is left out |
| `key_collision` | two keys read the same, e.g. the text `1` and the integer `1`; one is kept |
| `invalid_base64` | a CIP-26 `logo` is not valid base64 |
| `too_large` | a CIP-26 `logo` is over 64 KiB once decoded |

New codes may be added; an existing code is never changed or removed.

## What is not indexed at all

A few things cannot be stored, so they have no errors to show. The [validator](../../validator/) reports them with
these codes:

| Code | When |
|---|---|
| `filename_subject_mismatch` | a CIP-26 file named after another subject than the one inside it |
| `unusable_subject` | a CIP-26 subject that is not hex, or not 56 to 120 characters |
| `datum_undecodable`, `datum_too_deep_to_decode` | a CIP-68 datum that cannot be decoded |
| `not_cip68` | a datum that is not a CIP-68 datum at all (validator only; the registry simply ignores it) |
| `registry_node_unparseable` | a CIP-113 registry node that cannot be read |
