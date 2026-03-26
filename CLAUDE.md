# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

This is a CIP-26 compliant offchain metadata registry for Cardano tokens built with Spring Boot 3. The project consists of:

- **api**: REST API server implementing CIP-26 specification with extended querying capabilities
- **job**: Background job processor for syncing token metadata from GitHub
- **common**: Shared utilities and domain models
- **cli**: Command-line tools for metadata operations
- **integration-test**: End-to-end integration tests run against a live API instance

The API integrates with Yaci Store for blockchain data and uses PostgreSQL for persistence with Flyway migrations.

## Key Commands

### Build and Run
```bash
# Build entire project
mvn clean package

# Run tests
mvn clean verify

# Run with Docker Compose (includes Postgres DB)
docker compose up

# Rebuild Docker images after code changes
docker compose build
```

### Development
```bash
# Run single test
mvn test -Dtest=TestClassName

# Skip tests during build
mvn clean package -DskipTests

# Check API health
curl http://localhost:8081/actuator/health
```

## Database Management

Database migrations are managed by Flyway and located in:
- `api/src/main/resources/db/migration/postgresql/`
- `api/src/main/resources/db/store/{vendor}/`

Migrations run automatically on application startup when `spring.flyway.enabled=true`.

## Configuration

Main configuration is in `api/src/main/resources/application.properties`. Key environment variables:
- `DB_URL`: PostgreSQL connection URL
- `DB_USERNAME`, `DB_PASSWORD`: Database credentials
- `TOKEN_METADATA_SYNC_JOB`: Enable/disable GitHub sync job
- `GITHUB_ORGANIZATION`, `GITHUB_PROJECT_NAME`: Source repository for metadata

## API Endpoints

The API implements CIP-26 endpoints plus extensions:
- OpenAPI spec available at `/apidocs`
- Health check at `/actuator/health`
- Prometheus metrics at `/actuator/prometheus`

## Testing Strategy

- Unit tests use JUnit 5, Mockito, and **AssertJ** (`assertThat()`) for fluent assertions
- Use **`@Nested`** inner classes to group tests by concern (e.g. `@Nested class FindByPolicyId`, `@Nested class ContainsRegistryNode`)
- Controller tests use `@WebMvcTest` with `MockMvc` and `@MockBean` for dependencies
- End-to-end integration tests live in `integration-test/` module — they use `RestTemplate` against a running API (no Spring test context)
- Test coverage tracked via JaCoCo, reports in `api/target/site/jacoco/`
- Do not test POJOs/records — focus on real logic (parsing, filtering, query behavior, edge cases)
- **Do not use `var`** — always use explicit types for local variables
- **Annotate nullable returns with `@Nullable`** — use `jakarta.annotation.Nullable` on any method that can return null