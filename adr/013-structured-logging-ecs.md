# ADR-013: Structured Logging with ECS Format

## Status

Accepted

## Date

2026-03-24

## Context

The registry runs in containerized environments where logs are collected and aggregated by centralized logging systems (e.g., ELK stack, Loki). Plain-text log messages are difficult to parse, filter, and correlate at scale. Structured logging formats enable log aggregation tools to index fields automatically, supporting efficient search and alerting.

Additionally, log messages that include user-supplied data (e.g., token subject identifiers) must be sanitized to prevent log injection attacks and excessive log volume from malformed inputs.

## Decision

We implement a dual-output logging strategy:

1. **Console output**: Human-readable text format for local development and debugging.
   ```properties
   logging.structured.format.console=text
   ```

2. **File output**: Elastic Common Schema (ECS) JSON format for production log aggregation.
   ```properties
   logging.structured.format.file=ecs
   logging.file.name=logs/application.log
   ```

3. **Log sanitization**: A `LogSanitizer` utility sanitizes hexadecimal subject strings in log messages, preventing log injection and controlling log volume from untrusted input.

4. **Logging framework**: SLF4J via Lombok `@Slf4j` annotation, backed by Logback (Spring Boot default). Log levels follow standard conventions:
   - `INFO`: Major operations (sync start/complete, API startup)
   - `WARN`: Recoverable issues (individual token failures, missing files)
   - `ERROR`: Unexpected failures with exception stack traces
   - `DEBUG/TRACE`: Minimal usage, available for troubleshooting

## Consequences

### Positive

- **Machine-parseable**: ECS format provides structured JSON with standardized field names, enabling automatic indexing by Elasticsearch and other tools.
- **Developer-friendly**: Text format on console keeps local development simple without requiring JSON parsing.
- **Ecosystem compatibility**: ECS is the standard format for Elastic Stack, widely supported by log shippers (Filebeat, Fluentd) and visualization tools (Kibana).
- **Security**: Log sanitization prevents log injection and ensures sensitive or malformed inputs don't corrupt log streams.

### Negative

- **Dual format maintenance**: Developers must consider how log messages render in both formats.
- **File-based logging in containers**: Writing to a log file in a container requires volume mounts or sidecar containers for log shipping. Alternatively, the ECS format could be applied to stdout directly.
- **ECS verbosity**: Structured JSON logs are significantly larger than plain text, increasing storage requirements.

## Alternatives Considered

- **JSON to stdout only**: Simpler for container environments (logs captured by container runtime), but loses the developer-friendly text format for local use.
- **Logstash format**: Another structured format, but ECS is the newer standard and provides richer field semantics.
- **Custom JSON format**: Full control over field names but loses compatibility with ECS-aware tools and dashboards.
- **Log4j2**: Alternative logging backend with native JSON layout support. However, Spring Boot's Logback integration is more mature, and switching would add configuration complexity.
