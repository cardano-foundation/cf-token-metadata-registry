# ADR-010: V2 API with Query Priority Model

## Status

Accepted

## Date

2026-03-24

## Context

The original V1 API was designed exclusively for CIP-26 offchain metadata, implementing the CIP-26 specification endpoints. With the addition of CIP-68 on-chain metadata support, the API needed to serve metadata from multiple sources. However, modifying the V1 API would break backward compatibility with existing consumers who expect strict CIP-26 responses.

Different consumers have different trust preferences: some prefer on-chain data (CIP-68) as the authoritative source, while others prefer the curated offchain registry (CIP-26). The API needs to support both preferences.

## Decision

We introduce a V2 API at `/api/v2` alongside the existing V1 API, with the following design:

1. **V1 API preserved**: The original CIP-26-only endpoints remain at the base path, serving only offchain metadata. No breaking changes for existing consumers.

2. **V2 API endpoints**:
   - `GET /api/v2/subjects/{subject}` - Single subject query with optional priority, property filter, and `showCipsDetails` flag
   - `POST /api/v2/subjects/query` - Batch query with the same options

3. **Query priority model**: A `QueryPriority` enum (`CIP_26`, `CIP_68`) controls the order in which sources are queried. The default priority is configured via `cip.query.priority=CIP_68,CIP_26` (on-chain preferred). V2 API consumers can override the priority per request.

4. **Required properties validation**: V2 API enforces that `name` and `description` are always included in property filters, as these are considered essential metadata fields.

5. **CIP details flag**: When `showCipsDetails=true`, the response includes information about which CIP standard provided the data, enabling transparency about the data source.

6. **Metrics tracking**: API queries are tagged by version (`v1`, `v2`) and CIP hits are tracked by standard (`26`, `68`) via Prometheus counters.

## Consequences

### Positive

- **Backward compatibility**: V1 consumers are unaffected by the introduction of multi-standard support.
- **Consumer flexibility**: V2 consumers can choose their preferred metadata source and get transparency about data provenance.
- **Gradual migration**: Consumers can migrate from V1 to V2 at their own pace.
- **Operational insight**: Per-version and per-CIP metrics allow operators to understand usage patterns and plan deprecation timelines.

### Negative

- **Dual API maintenance**: Two API versions must be maintained, tested, and documented, increasing the maintenance surface.
- **Eventual V1 deprecation**: At some point V1 will need to be deprecated, requiring consumer coordination.
- **Controller complexity**: V2 controllers contain merge logic for combining results from multiple CIP sources, making them harder to test and reason about.

## Alternatives Considered

- **Extend V1 with optional parameters**: Add CIP-68 support to V1 via query parameters. Risks breaking existing consumers who may not expect additional fields in responses.
- **GraphQL**: A query language would naturally support flexible field selection and multi-source resolution. However, it would be a significant departure from the REST-based CIP-26 specification and ecosystem conventions.
- **Separate endpoints per CIP**: Expose `/cip26/{subject}` and `/cip68/{subject}` independently. Simpler per-endpoint but pushes merge logic to consumers, who would need to know about and query both endpoints.
- **Content negotiation**: Use HTTP `Accept` headers or custom headers to select CIP priority. Less discoverable than URL-based versioning and harder to test with simple tools like `curl`.
