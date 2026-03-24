# ADR-005: PostgreSQL with JSONB for Extensible Metadata

## Status

Accepted

## Date

2026-03-24

## Context

CIP-26 token metadata has a set of well-known fields (name, ticker, description, url, decimals, logo) but also supports arbitrary custom properties submitted by token issuers. These custom properties vary widely across tokens and cannot be predicted at schema design time. The storage solution must handle both structured queries on known fields and flexible storage for unknown properties.

Additionally, the API needs full-text search capability across metadata fields for discovery use cases.

## Decision

We use PostgreSQL as the primary database with the following schema design:

1. **Structured columns for well-known fields**: The `metadata` table has dedicated columns for `subject` (PK), `policy`, `name`, `ticker`, `url`, `description`, `decimals`, `updated`, and `updated_by`. These support efficient indexing and type-safe queries.

2. **JSONB column for custom properties**: A `properties` column of type `JSONB` stores arbitrary token metadata properties. This allows schema-less storage while supporting GIN indexing for containment queries.

3. **Full-text search**: A generated `tsvector` column combines weighted fields (name/ticker at weight 'A', description at 'B', url/updated_by at 'C') for PostgreSQL full-text search without an external search engine.

4. **Composite indexes**: A multi-column B-tree index on the structured fields (`defaultfields`) and a GIN index on the `properties` column provide query performance for both access patterns.

5. **Separate logo table**: Token logos (base64-encoded PNG data, up to 87,400 characters) are stored in a `logo` table with a foreign key to `metadata`, keeping the main table lean for queries that don't need logo data.

## Consequences

### Positive

- **Schema flexibility**: New CIP-26 properties are automatically supported without schema changes, as they are stored in the JSONB column.
- **Query performance**: Well-known fields benefit from B-tree indexes; custom properties benefit from GIN indexes. Full-text search is built into PostgreSQL without external infrastructure.
- **Operational simplicity**: A single database engine serves all storage needs, reducing operational complexity.
- **ACID guarantees**: PostgreSQL provides transactional consistency for metadata updates, important when syncing incremental changes from GitHub.

### Negative

- **PostgreSQL lock-in**: The use of JSONB, generated columns, and `tsvector` are PostgreSQL-specific features. Migration to another database would require significant rework.
- **JSONB query limitations**: Complex queries on deeply nested JSONB structures can be slower than queries on normalized tables with proper indexes.
- **Logo storage in SQL**: Storing large base64 strings in a relational database is not optimal. Object storage (e.g., S3) would be more efficient for large binary data, but the simplicity trade-off is acceptable given the bounded logo size.

## Alternatives Considered

- **MongoDB**: Native document storage would handle the schema-less properties naturally, but adds operational complexity (separate database engine) and loses PostgreSQL's relational strengths for the structured fields.
- **Elasticsearch**: Excellent for full-text search but would introduce a second data store requiring synchronization. PostgreSQL's built-in `tsvector` is sufficient for the current use case.
- **Fully normalized schema**: Creating a generic key-value table for custom properties would be database-agnostic but significantly more complex to query and index.
- **H2 for production**: Used in tests, but lacks JSONB, full-text search, and the operational maturity needed for production workloads.
