# ADR-023: A Stateless Endpoint to Validate a Token Before It Exists

## Status

Accepted

## Date

2026-10-22

## Context

The registry tells you what is wrong with a token once it is indexed: `validation_errors`, `metadata.valid` and the
`?validation=` scopes (ADR-019), and `invalid_token` for what cannot be stored at all (ADR-020). By then the token is
minted, or its CIP-26 entry is merged, and fixing a mistake costs a new transaction or another pull request.

Token issuers ask the question earlier: *if I mint this, or submit this file, what will the registry make of it?* The
rules are spread over several CIPs, a label decides which fields are mandatory (333, 444), a logo must be a URI with an
allowed scheme, a version must be one CIP-68 defines, and a CIP-113 token is served only with companion metadata.
Answering it by reading the CIPs is error-prone, and a separate validator would drift from what the registry does.

## Decision

Add a **stateless validate endpoint** to the V2 API, which runs **the code the indexer runs** on input that exists only
in the request:

| Endpoint | Input | Runs |
|---|---|---|
| `POST /api/v2/validate/cip26` | a registry file (`mapping`) and, optionally, its file name | `TokenMetadataValidator` (CIP-26 rules, logo), the file name check |
| `POST /api/v2/validate/cip68` | a reference NFT datum as CBOR hex, **or** the token fields to build it from; the labels (333, 444) | `Cip68FTDatumParser`, the per-label rules of the CIP-68 listener (`Cip68EventListener.errorsForLabels`), and the mapping V2 serves (`Cip68FungibleTokenService`) |
| `POST /api/v2/validate/cip113` | a registry node datum as CBOR hex | `Cip113RegistryNodeParser` |

The answer has the shape of what the registry would record and serve:
- `stored`, and `dropped` with the same reason codes as `invalid_token` when it would not be stored;
- `validation_errors`, as V2 returns them;
- `served`: per label (CIP-68) or once (CIP-26), the V2 `metadata` block, the subject's errors and the `?validation=`
  scopes that return it;
- `datum`: for CIP-68, the datum as given, or as built from the fields (`Cip68DatumBuilder`), so an issuer can put
  exactly that datum on the reference NFT;
- `cip113`: the extension V2 would add;
- `notes`: what the answer assumes. A 333/444 subject is served only once its user token is minted (ADR-022); CIP-26
  signatures are not verified; a programmable token is served only with its CIP-26 or CIP-68 metadata (ADR-020).

**Nothing is stored or read from the index.** The endpoint is a function of its input.

**It is public, so it is bounded:**
- **Size:** a request larger than `registry.validate.max-request-bytes` (64 KiB by default), or of unknown length, is
  refused (413 / 411) before it is read. The parser's own bounds (nesting depth, value sizes) apply as for chain data.
- **Switch:** `registry.validate.enabled` (default `true`). It is read at runtime, not as a bean condition, so the native
  image honours it too (see ADR-021).
- **CORS:** only for `/api/v2/validate/**`, only for the origins in `registry.validate.cors.allowed-origins` (for example
  the project website on GitHub Pages). Without origins, a browser on another site cannot call it.
- **Rate limiting** is left to the ingress in front of the API, like for the rest of it.

**Scope of CIP-113:** this is a simplified check: whether the registry can read a node, and what V2 would show. Building
and submitting registrations belongs to the CIP-113 tooling (cardano-foundation/cip113-programmable-tokens), which the
website links to.

## Consequences

### Positive

- Issuers can check a token before it costs a transaction or a pull request, and get the datum to mint.
- The answer cannot drift from what the registry does: it is the same code.
- The project website can offer an interactive validator without a backend of its own.

### Negative

- A public, unauthenticated POST endpoint: it is bounded and stateless, but operators should rate-limit it at the
  ingress, or switch it off.
- CIP-26 signatures and policy scripts are not verified; the answer covers the content of the entry. Verifying them is
  possible later.
- A CIP-68 answer assumes the user token is minted: the endpoint cannot know which tokens will be.

## Alternatives Considered

- **Validate in the browser:** the rules would be reimplemented in TypeScript and drift from the indexer.
- **A separate validator service:** a second deployment and a second copy of the rules.
- **Only document the rules:** issuers would still find mistakes after minting.
