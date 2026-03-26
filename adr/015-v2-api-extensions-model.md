# ADR-015: V2 API Extensions Model

## Status

Accepted

## Date

2026-03-26

## Context

The V2 API was originally designed around two metadata standards: CIP-26 (offchain) and CIP-68 (on-chain). Both provide the same category of data — display metadata (name, description, ticker, decimals, logo, url) — and the query priority model (ADR-010) merges them into a single `metadata` block per subject.

As the Cardano ecosystem evolves, new CIPs introduce data that is **orthogonal to display metadata**. For example, CIP-113 describes transfer validation rules for programmable tokens — Plutus script hashes and policy IDs that have no overlap with name/description/ticker. Future CIPs may add royalty information, token categories, compliance attestations, or other supplementary data.

This data does not fit the existing metadata merge model:

1. **Not an alternative source**: It is supplementary information, not a competing answer to "what is this token's name?"
2. **Not subject to priority**: There is no preference choice — the data is either present or absent.
3. **Different schema per CIP**: Each CIP defines its own fields with no overlap with the display metadata model.

Adding these fields directly into the `metadata` block would conflate display metadata with behavioral/supplementary metadata. Adding them as flat top-level fields on the `Subject` record would pollute the response schema with every new CIP.

## Decision

We introduce an **extensions model** in the V2 API response:

### 1. Extension marker interface

A marker interface `Extension` serves as the base type. Each CIP that enriches the Subject response implements this interface and is keyed by its CIP identifier (e.g., `cip113`) in the response map:

```json
{
  "subject": {
    "subject": "policyId + assetName",
    "metadata": { "..." },
    "extensions": {
      "cip113": { "..." }
    }
  }
}
```

### 2. Map-based response field

The `extensions` field on the `Subject` record is a `Map<String, Extension>`, annotated with `@JsonInclude(NON_EMPTY)`. When a token has no extensions, the field is omitted entirely from the JSON response — existing consumers see no change.

### 3. Separate from query priority

Extensions are appended to the response *after* the priority-based metadata merge. They are not part of the `QueryPriority` enum and do not participate in the merge/fill logic. This keeps the two concerns cleanly separated.

### 4. OpenAPI documentation

The `Extension` interface uses `@Schema(oneOf = {...})` to enumerate all known extension types in the OpenAPI spec, making the possible extension shapes discoverable via `/apidocs`.

## Consequences

### Positive

- **Clean separation**: Display metadata and supplementary CIP data live in distinct response sections with clear semantics.
- **Extensible by design**: New CIPs implement `Extension`, register under their own key, and require no changes to the metadata or query priority model.
- **Backward compatible**: The field is omitted when empty, preserving existing response shapes for tokens without extensions.
- **Single response**: Consumers get all token data — metadata and extensions — in one API call, avoiding multi-request orchestration.

### Negative

- **Boilerplate per extension**: Each new CIP requires its own model class, service, and wiring into the controller's extension-building logic.
- **No extension discovery at runtime**: The API does not expose a list of active extensions. Consumers must consult the OpenAPI docs to know which extensions may appear.
- **Untyped map key**: The `String` key relies on convention (e.g., `cip113`) rather than compile-time enforcement.

## Alternatives Considered

- **Flat top-level fields**: Add `programmable_token`, `royalty_info`, etc. directly on the `Subject` record. Does not scale — each CIP pollutes the response schema and requires a model change to `Subject`.
- **Embed in metadata block**: Add extension fields alongside `name`, `description`, `ticker`. Rejected because it conflates orthogonal concerns and breaks the merge semantics.
- **Embed in standards block**: Add a `cip113` entry alongside `cip26`/`cip68` in the `standards` map. Rejected because `standards` represents *display metadata sources* and CIP-113 is not one — mixing them would mislead consumers about the data's purpose.
- **Separate endpoint per CIP**: Expose `/api/v2/extensions/cip113/{policyId}`. Forces consumers to make multiple requests and know which extensions might apply. The in-response map keeps things consolidated.
