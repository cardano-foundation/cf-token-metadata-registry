# ADR-017: V2 Policy API

## Status

Accepted

## Date

2026-03-27

## Context

The V2 API was originally subject-centric: consumers query by `subject` (policyId + assetName) and receive display metadata from CIP-26/CIP-68. With the addition of CIP-113 programmable tokens, a gap emerged.

CIP-113 is a **policy-level** concept — transfer validation rules apply to all tokens minted under a policy, not to individual asset names. Meanwhile, the subject-centric V2 endpoints require a full subject identifier and return 404 when no CIP-26/CIP-68 metadata exists, even if CIP-113 data is available for that policy.

In practice, programmable tokens are new assets minted with transfer logic from day one. They will typically also have CIP-68 reference NFTs for display metadata. However, the natural query pattern for wallets and compliance tooling is **policy-first**: "is this policy programmable? what tokens exist under it?" rather than "tell me about this specific subject."

A policy is the natural grouping level — it tells you everything about a token **type**: what tokens exist under it (CIP-26, CIP-68), and what capabilities it has (CIP-113 programmable status).

## Decision

### 1. Policy endpoints within V2

Two new endpoints extend the V2 API:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v2/policies/{policyId}` | All tokens and capabilities for a policy |
| POST | `/api/v2/policies/query` | Batch lookup by policy IDs |

These live alongside the existing `/api/v2/subjects/` endpoints. The existing subject endpoints are unchanged — full backward compatibility.

### 2. Response structure

The policy response aggregates data from all three standards:

```json
{
  "policy_id": "ae563991...",
  "tokens": [
    {
      "subject": "ae563991...0014df10555344432",
      "name": "USDC",
      "ticker": "USDC",
      "decimals": 6,
      "source": "CIP_68"
    }
  ],
  "extensions": {
    "cip113": {
      "transfer_logic_script": "77014ec6...",
      "third_party_transfer_logic_script": "4b348779...",
      "global_state_policy_id": null
    }
  }
}
```

- **`tokens`** — deduplicated list of tokens from CIP-26 and CIP-68. CIP-68 takes precedence when the same subject exists in both (consistent with the default query priority). Each entry provides minimal display fields (name, ticker, decimals, source).
- **`extensions`** — per-policy extensions keyed by CIP identifier (e.g. `cip113`). Uses the same `Extension` interface as V2 subject endpoints (ADR-015). Omitted when the policy has no extensions.

### 3. Token summary is intentionally minimal

The `tokens` array provides just enough to identify and label each token. For full metadata (logo, description, all properties, standards breakdown), consumers use the existing `GET /api/v2/subjects/{subject}` endpoint. This avoids duplicating the complex metadata merge logic and keeps the policy response lightweight.

### 4. 404 semantics

Returns 404 when a policy has no data at all — no CIP-26 tokens, no CIP-68 reference NFTs, and no CIP-113 registration. Returns 200 with empty `tokens` if only CIP-113 data exists (the policy is programmable but has no display metadata yet).

### 5. Batch optimization

The batch endpoint (`POST /api/v2/policies/query`) pre-fetches all CIP-26, CIP-68, and CIP-113 data in three bulk queries, then assembles responses per policy. Unknown policies are silently omitted from the response.

### 6. No "list all" endpoint

A paginated "list all policies" endpoint was deliberately omitted — it would require full table scans with no practical consumer. The batch endpoint covers targeted lookups, and the `cftr_tokens_cip113_count` Prometheus metric provides the total programmable token count.

## Consequences

### Positive

- **Policy-first access pattern**: Wallets and compliance tools can query by policy ID directly without knowing specific asset names or subjects.
- **Unified view**: A single response combines CIP-26, CIP-68, and CIP-113 data for a policy, eliminating multi-request orchestration.
- **Backward compatible**: Existing subject endpoints are unchanged. The policy endpoints are purely additive.
- **Extensible**: Future per-policy metadata (royalties, minting rules) can be added as additional fields on the policy response.

### Negative

- **Minimal token summary**: The `tokens` array does not include all metadata properties. Consumers need a second call to `/api/v2/subjects/{subject}` for full details.
- **CIP-26 by policy requires new query**: The `metadata` table is keyed by subject, not policy. The new `findByPolicy()` repository method adds a query path that wasn't previously needed.

## Alternatives Considered

- **Standalone CIP-113 endpoints** (`/api/v2/cip113/registry/{policyId}`): Initially implemented, then replaced by the policy API. The standalone approach only served CIP-113 data and did not aggregate CIP-26/CIP-68 tokens, making it a narrow single-purpose endpoint that would need to be replicated for every future per-policy CIP.
- **Extend the V2 subject endpoint to accept policy-only queries**: Would require changing 404 semantics and the response structure of an existing endpoint, breaking backward compatibility.
- **Return full metadata in the tokens array**: Would duplicate the complex metadata merge logic from the subject endpoint and produce heavy responses for policies with many tokens.
