# ADR-019: Validation Errors Are Reported, Not Enforced

## Status

Accepted. Supersedes ADR-017. Follows yaci-store's ADR 0006 (bloxbean/yaci-store#1224, implemented in #1220). Amended by ADR-023
(2026-11-04): blocks are indexed in chain order, not in parallel.

## Date

2026-10-21

## Context

Until this change, the registry enforced CIP-26 and CIP-68 by **dropping**.
- A CIP-26 entry that failed the cf-tokens-cip26 validator was not stored.
- A CIP-68 datum that missed a field its label requires, or had a version CIP-68 does not define, was not indexed.
- A CIP-68 logo that was not an allowed URI was removed from the datum.
- V2 answered 404 for a token whose merged metadata had no `name` and `description`.

A dropped token is invisible. Its owner cannot tell why it doesn't show, and a wallet cannot decide for itself. On
mainnet this hid, among others:
- Greenland Reserve Coin (version 0)
- the SundaeSwap USDrf/sUSDrf LP token, whose reference NFT is paired with a pool NFT
- 217 fungible tokens whose logo was a bare IPFS CID

The team decided on a different policy: index what is written, and say what is wrong with it. The design (the
`validation_errors` contract and the `invalid_token` table) was first worked out for yaci-store's `assets-ext`, and the
registry's CIP-68 parser and validators are ported from it.

## Decision

### 1. Index everything usable, serve it as indexed, and say what is wrong

A CIP-26 entry that can be read, and a CIP-68 datum that can be decoded, are **stored as written and served**. What
departs from the CIP is neither repaired nor dropped: it is described in a list of **validation errors**, stored with
the row (`validation_errors` JSON column, migration V7) and returned by the V2 API. Values are kept as written: a logo
with a bad scheme, a version 0, a `decimals` of 300, a Plutus constructor in a property (kept as
`{"constructor": n, "fields": [...]}`). A CIP-68 text given as a list of byte strings is kept as
the same bytes, joined, the way CIP-68 itself joins a chunked `uri`: that is a representation, not a repair. It is
**not reported**:
- **What CIP-68 says:** it defines these texts as one `bounded_bytes`, which can be any length. In a datum, the 64-byte
  limit applies to each CBOR chunk of a byte string, not to its value; the 64-byte limit on each text is a rule of
  transaction metadata (CIP-25), not of Plutus Data. The ledger accepts a long text as one byte string: on mainnet
  (2026-11-02, latest row per reference NFT) 299 descriptions over 64 bytes are stored that way, the longest 1,406
  bytes. So a list is never needed in a datum; it is a habit carried over from CIP-25.
- **What minters do:** most minting tools split a long text into a list anyway. On mainnet, 592 fungible-token
  descriptions and 13,480 NFT descriptions are lists, against 107 and 171 long descriptions stored as one byte string.
- **So the list is accepted as the text,** as CIP-68 version 3 accepted it for `image` and `src`. The token is valid,
  and `STRICT` returns it.
- **Rows indexed before:** a row indexed by an earlier build may hold a `chunked_text` error; it is left out when the
  token is served, so no resync is needed.

### 2. The contract: `validation_errors`

Each error has the same fields and codes as yaci-store's:

| Field | Meaning |
|---|---|
| `cip` | `CIP-26`, `CIP-68`, or `CIP-113` for a programmable token without CIP-26 or CIP-68 metadata (ADR-020 decision 3) |
| `sub_standard` | for CIP-68, the label the rule was checked for: the label of the subject asked for (`333`, `444`); absent for CIP-26 and for a rule that does not depend on a label (a bad `logo`, a version) |
| `field_name` | the field, e.g. `description`, `image`, `version`, `files`, `properties.owner` |
| `field_category` | `MANDATORY` if the CIP requires the field, `OPTIONAL` otherwise |
| `code` | a stable reason: `missing_value`, `wrong_type`, `invalid_uri_scheme`, `invalid_length`, `invalid_value`, `out_of_range`, `invalid_version`, `plutus_constructor`, `not_utf8`, `invalid_files_entry`, `no_nested_entry`, `too_deep`, `unsupported_key`, `key_collision`, `invalid_base64`, `too_large`, `invalid_constructor` |
| `error` | a message for people |

V2 returns `validation_errors` in two places:
- **On the subject:** the errors of every standard that has the token, in query priority order.
- **On `standards.cip26` / `standards.cip68`:** each standard's own errors.

`metadata.valid`, which V2 always had, stays where it is and is `true` exactly when there are no errors. A row stored
before validation existed has `NULL` errors, meaning "never checked", which is different from `[]`. For such a row,
`validation_errors` is left out and `valid` falls back to its old rule: name and description present.

### 3. The API serves what is indexed

- **The `name`/`description` gate is gone:** a token with errors is a `200`. A subject is a `404` only when no
  standard has anything indexed for it.
- **Property filters:** a filter (`?property=`) no longer has to include `name` and `description`. Under a filter,
  the errors, `valid` and the gate are about the requested properties only. An `image` error counts for `logo`, and
  `no_nested_entry` always counts.
- **`?validation=`** chooses which tokens are returned, on `GET /api/v2/subjects/{subject}` and
  `POST /api/v2/subjects/query`:
  - `ALL`, the default: every indexed token.
  - `MANDATORY_ONLY`: no `MANDATORY` error.
  - `STRICT`: no error at all.

  A token that is not accepted is a `404`, or is left out of a batch. The errors of a returned token are never
  filtered.

  **The default is `ALL`:**
  - The registry describes what is indexed. A client that does not ask for a scope gets every token, with what is
    wrong with it, and decides for itself: a wallet that only wants tokens meeting their CIP asks for
    `MANDATORY_ONLY` or `STRICT`.
  - A stricter default would bring back, for every client that doesn't set the parameter, the invisible tokens this
    ADR is about.

### 3a. `MANDATORY_ONLY` is close to the old view, not the same

A client that wants what V2 returned before can ask for `MANDATORY_ONLY`. It returns roughly the tokens the old rules
kept, but differs in both directions:
- **It returns more:**
  - A CIP-26 entry whose only errors are on optional fields (`ticker`, `url`, `decimals`, `logo`). Before, any
    cf-tokens-cip26 error dropped the whole entry; now only `name` and `description` errors are `MANDATORY`.
  - A CIP-68 token with a `logo` that is not an allowed URI. It used to be served with the logo removed; it is now
    served with the logo as written and an `OPTIONAL` error. The logo value a client receives therefore changes.
  - A CIP-68 token whose `name` or `description` is given as a list of byte strings. Before, such a text was not read;
    now it is joined and served as the text, with no error (decision 1). 591 of the 2,024 fungible tokens on mainnet
    have their description as a list.
- **It returns less:** a token is hidden by any `MANDATORY` error of a standard that has it, not only by a missing
  merged `name` or `description`. For example, a token with a CIP-26 entry that has an error and a valid CIP-68 datum
  used to be served from the datum; now it is hidden, because the CIP-26 error counts.

`STRICT` hides every token with any error, so it is stricter than the old rules everywhere.

### 4. What is still dropped: only what has no row, or looks like an attack

These are dropped, with a WARN, and recorded in `invalid_token` (ADR-020):
- **CIP-68 datums:**
  - a datum that cannot be decoded, or is nested too deeply to decode
  - an output whose indexing throws something unexpected; only that reference NFT is skipped
- **CIP-26 files:** a file whose name differs from its subject, whose subject cannot be a key, that cannot be parsed,
  that has no Git history, or whose check throws something unforeseen (`processing_failure`, not `unusable_subject`:
  the subject may be fine)

A property nested more than 100 levels is dropped from its datum (with a `too_deep` error), and the rest of the datum
is stored. A CIP-26 logo over about 1 MiB is not stored in the logo table, and is an `invalid_token` row
(`logo_too_large`); the entry is stored.

### 4a. CIP-26: what the registry's validator does not check, and the subject

Found on the mainnet validation run of 2026-10-26, and done the same way in yaci-store (bloxbean/yaci-store#1220):
- **A `url` needs `https` and a host.** cf-tokens-cip26 checks the scheme only, so `https:/rosen.tech` (one slash)
  passed. A `url` that is not a valid URI, not `https`, or has no host is an `OPTIONAL` `invalid_value` error that
  quotes it, unless the library already reported the `url`.
- **CIP-26 `decimals` is checked against [0, 19]** (2.0.1), the range CIP-26 allows. Up to 2.0.0 the check used
  `TokenDecimals`, [0, 255], so 20 to 255 passed as valid; that stays the storage bound shared with CIP-68, which sets
  no range of its own. None on mainnet: the highest is 18. Found in review of bloxbean/yaci-store#1220.
- **A subject is stored in lower case.** It is base16 and its case carries no meaning, but 39 mainnet registry files
  are named and written in upper case and were stored as written, so a lower-case query, as every on-chain subject is
  written, was a 404. The subject is lower-cased when the entry and its logo are stored, when an entry is deleted, and
  when a full sync compares the stored subjects with the file names. There is no migration: deployments of this
  release index from an empty database.
- **An entry is served with its subject as the registry file writes it** (V1, and `standards.cip26` in V2), not in
  lower case: each CIP-26 signature is made over the CBOR of the subject as written, so the signatures of those 39
  entries (106 on mainnet) only verify against the subject in upper case. 2.0.0 served the stored subject, in lower
  case, and none of them verified; corrected in 2.0.1. The entry is still stored in lower case.
- **A subject asked for is looked up in lower case** (V1 and V2, alone and in a batch), so a client finds an entry in
  either case. 2.0.0 looked up the subject as asked: one asked for as the registry file writes it, in upper case, was
  a `204` on V1 and a `404` on V2, where 1.7.0 (the release before) found it; corrected in 2.0.1.

### 5. Where the registry differs from yaci-store

- **222-only datums are not stored.**
  - The registry serves fungible and rich fungible tokens only (ADR-018), so a datum whose transaction mints its
    reference NFT with a 222 NFT and no 333 or 444 token is skipped at DEBUG.
  - A datum without a user token in its transaction is stored, whatever it looks like (decided 2026-10-28). Its 333
    or 444 token can be minted in another transaction, before or after the reference NFT: on mainnet the GOAT RFTs
    (`e987e459…`) minted their reference NFTs first, BushWifPlanesTier1 (`cc816014…`) its 444 token first. Their
    datums have an image and no `decimals`, so a label guessed from the datum read them as 222 NFTs, and they were
    never served. Only the transaction can be relied on: a 333 or 444 token minted in a later block is not known when
    the datum is indexed. (*Amended by ADR-023:* this said that yaci-store processes the blocks of a batch in parallel
    during sync. The registry does not: it indexes blocks one at a time, in chain order.) An NFT whose reference NFT was minted
    without its 222 token is stored too, and answers under its 333 and 444 subjects, as any reference NFT does
    (ADR-022, rejected).
  - A datum that updates a reference NFT already stored is kept whatever its transaction holds.
  - A datum without a user token in its transaction and with none of the CIP-68 token fields (`name`, `description`,
    `ticker`, `decimals`, `url`, `logo`, `image`, `mediaType`, `files`, a nested `"721"`), usable or not, is skipped at
    DEBUG unless its reference NFT is already stored. Such a datum would only read as a 333 by default; on mainnet
    these were dApp state (Atrium profiles, an `insert_fee`) and empty maps.
  - yaci-store stores 222 datums too.
- **A datum that is not constructor 0, and a `uri` list with an element that is not bytes, are reported (2.0.1).**
  CIP-68 defines the datum as `#6.121([metadata, version, extra])`, constructor 0, and a `uri` as
  `bounded_bytes / [* bounded_bytes]`. Up to 2.0.0 the parser read both without an error, so they passed `STRICT`.
  - A datum of another constructor is read as written, with a **`MANDATORY`** `invalid_constructor` error (field
    `constructor`), like an invalid `version`. On mainnet 12 datums are not constructor 0 (2026-11-04), 3 of them of 333
    tokens: two of BuyBTC (constructor 1) and one of Greenland Reserve Coin (the general form, tag 102).
  - An element of a `uri` list that is not a byte string is left out, the byte strings are joined, and a `wrong_type`
    error says so. None on mainnet (943 lists, all byte strings).
  - The full URI syntax is not checked, only the scheme: no mainnet value with an allowed scheme breaks it, and a
    strict parser risks rejecting real `data:` URIs.
  - The errors are stored when a datum is indexed: a 2.0.0 database has them only after a resync.
  Found in review of bloxbean/yaci-store#1220, where `assets-ext` has the same fix.
- **Errors of the label asked for, checked when the token is served.**
  - The row stores what is wrong with the datum itself: the parser's errors and a bad `logo`. They have no
    `sub_standard`.
  - What the label requires (`name`; `description` for 333; `image` for 444) is checked when a subject is served, for
    the label of that subject, and reported with that label as `sub_standard`. The label of a datum is often guessed:
    an owner updates the reference NFT without moving the user tokens, and the transaction then holds no user token.
    One reference NFT can also serve a 333 and a 444 token. A label stored with the row would be wrong in both cases:
    a 333 updated with a name and an image but no description read as a 222, and was served without the error its
    missing description is.
  - What is wrong with the form of a `description` (`wrong_type`, `not_utf8`) is stored without a label and reported
    for the label asked for: MANDATORY for 333, which requires a description, OPTIONAL for 444. A 333 description of
    the wrong type is not also reported as missing.
  - A text given as a list of byte strings is accepted as the text and not reported (decision 1). It used to be a
    `chunked_text` error with the category of the field, MANDATORY for a `name` and for a 333's `description`. That
    hid tokens whose name and description read fine from `?validation=MANDATORY_ONLY`: 591 of the 2,024 fungible
    tokens on mainnet.
  - A `name` that is not valid UTF-8 (kept as hex) is a MANDATORY `not_utf8` error, as a `name` of the wrong type is:
    every label requires a name, and hex is not one. Other texts that are not UTF-8 are OPTIONAL.
  - An LP token minted with a pool NFT is a clean 333 token, not one that "lacks an image".
  - A report made with SQL on `validation_errors` sees the errors of the datum only; it has to apply the label rules
    itself.
  - yaci-store does the same (bloxbean/yaci-store#1220, after review).
- **V1 keeps its format, not its content.**
  - V1 (`/metadata/**`) is the registry server's own format, so its responses have no `validation_errors` field and
    it has no `?validation=`.
  - It serves every stored CIP-26 entry, and the entries with errors are now stored. A V1 client therefore gets
    entries it used to get a `204` for, such as one without a description or with a name over 50 characters. It
    cannot see their errors, or hide them; it has to use V2 for that.
- **No metrics of datums.**
  - The registry does not count indexed or skipped datums.
  - The recorder counts the rows it writes (`token.metadata.invalid.tokens{standard,reason_code}`).

### 6. The newest datum is served

A CIP-68 update that breaks the CIP is stored and served with its errors. The earlier row is no longer kept in its
place, so ADR-017's "last valid datum" rule is superseded. An output that moves the reference NFT without any datum is
not a datum and stores nothing: the last stored datum is still served.

**A dropped update leaves the previous metadata in the API.** What cannot be stored (a CIP-68 datum that cannot be decoded,
a CIP-26 file that cannot be parsed or whose check throws something unforeseen) writes no row, so the API keeps serving the
metadata of the earlier row of that token, with its errors, and says nothing about the update. The update shows in
`invalid_token` (ADR-020), with its subject and its transaction or file. A CIP-26 logo far beyond any real size is dropped on
its own: the entry is stored without a logo, and the logo is an `invalid_token` row (`logo_too_large`).

### 7. A full resync is needed

V7 is additive and rewrites no data, so a node upgrades in place and keeps working. But tokens dropped before exist
nowhere, and rows written before have no errors recorded, so a **full resync** is needed for complete results. There
is no backfill.

## Consequences

### Positive

- Owners and wallets see why a token is not what its CIP asks for. A wallet can be strict with `MANDATORY_ONLY` or
  `STRICT`.
- Fewer projects find their token missing from the registry for a detail.

### Negative

- **Breaking for V2 clients that relied on a 404 meaning "invalid", or on `name` and `description` always being
  there.** They now get a 200 with errors and `valid: false`. `validation=MANDATORY_ONLY` is the closest to what they
  got before, with the differences in decision 3a.
- **Breaking for V1 clients**, with no opt-out: entries that break CIP-26 are now served, without their errors (decision
  5). A V1 client that needs to tell them apart must move to V2.
- The index is larger (the tokens that were dropped), and every row carries a small JSON list.
- A change to a rule changes the errors of rows already stored; a resync recomputes them.

## Alternatives Considered

- **Keep dropping.** Rejected for the reasons above.
- **Default to `MANDATORY_ONLY` (or `STRICT`)**, so that clients that don't set the parameter keep roughly the old
  behaviour. Rejected: the default would still hide the tokens this ADR is about. Clients that want it ask for it.
- **Keep V1 to the entries without `MANDATORY` errors**, so that V1 does not change. Rejected: V1 would then answer
  differently from V2's default for the same CIP-26 entry, and the registry would still be dropping entries for its
  oldest clients. V1 has no way to report why an entry is missing, which is the problem this ADR solves.
- **A strict/lenient switch for operators.** Rejected: it gives operators a decision they can't make,
  and two behaviours to test.
- **Repair what is wrong** (e.g. a bare IPFS CID to `ipfs://...`). Rejected: the registry would then state something
  that is not on-chain.
- **Compute the errors at read time** from the stored datum. Rejected: it parses on every request, and cannot bring
  back what was never stored.
