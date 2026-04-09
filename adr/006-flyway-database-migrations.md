# ADR-006: Flyway for Database Schema Management

## Status

Accepted

## Date

2026-03-24

## Context

The registry's database schema evolves over time as new features are added (CIP-68 support, sync state tracking). Schema changes must be applied reliably across development, staging, and production environments, and must support both fresh installations and upgrades from any prior version.

The application also integrates Yaci Store, which brings its own database schema for blockchain indexing. Both schemas must coexist in the same database and be managed consistently.

## Decision

We use Flyway for database migration management with the following configuration:

1. **Migration locations**: Two directories are scanned:
   - `db/migration/postgresql/` for application-specific migrations
   - `db/store/{vendor}/` for Yaci Store schema (vendor-specific, e.g., PostgreSQL)

2. **Migration files**:
   - `V0__metadata_server_db_init.sql`: Initial schema (metadata, logo, wallet_scam_lookup tables with indexes and full-text search)
   - `V1__adding_support_for_cip68.sql`: CIP-68 reference NFT table
   - `V2__add_sync_state.sql`: Off-chain sync state tracking table

3. **Configuration choices**:
   - `baseline-on-migrate=true`: Allows Flyway to adopt existing databases without a migration history
   - `out-of-order=true`: Permits applying migrations that are numbered lower than already-applied migrations, useful when multiple branches introduce migrations concurrently
   - `validate-migration-naming=true`: Enforces consistent naming conventions

4. **Execution**: Migrations run automatically on application startup when `spring.flyway.enabled=true`.

## Consequences

### Positive

- **Reproducible environments**: Any environment can be built from scratch by running all migrations in order.
- **Version tracking**: Flyway's `flyway_schema_history` table provides an audit trail of applied migrations.
- **Multi-source schema management**: A single Flyway instance manages both application and Yaci Store schemas.
- **Safe evolution**: Each migration is a versioned SQL file, reviewable in pull requests and testable in CI.

### Negative

- **Startup dependency**: The application blocks on migration execution during startup, which can be slow for large migrations.
- **Out-of-order risks**: While enabled for development convenience, out-of-order migrations can cause confusion about the expected schema state.
- **SQL-only migrations**: Complex data transformations may be harder to express in pure SQL compared to Java-based Flyway migrations.

## Alternatives Considered

- **Liquibase**: More feature-rich (XML/YAML/JSON changeset formats, rollback support) but adds complexity. Flyway's SQL-first approach is simpler and aligns with the team's preference for plain SQL.
- **JPA auto-DDL (`hibernate.ddl-auto`)**: Convenient for development but unsafe for production, as Hibernate may generate destructive schema changes. Not suitable for a production system.
- **Manual schema management**: No migration tool, just SQL scripts applied manually. Error-prone and not reproducible.
