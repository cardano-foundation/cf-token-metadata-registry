# ADR-012: Prometheus Metrics with Custom Business Counters

## Status

Accepted

## Date

2026-03-24

## Context

The registry needs observability beyond health checks. Operators need to understand API usage patterns, monitor token coverage across CIP standards, and track sync status over time. Standard infrastructure metrics (JVM memory, HTTP request counts) are necessary but insufficient for operating a domain-specific service.

## Decision

We expose Prometheus-compatible metrics via Micrometer with the namespace `cftr` (CF Token Registry), combining standard infrastructure metrics with custom business counters:

**Counters (monotonically increasing)**:
- `cftr.api.queries` (tagged by `version`: v1, v2): Total API queries per version, enabling V1 deprecation planning.
- `cftr.api.cip.hits` (tagged by `cip`: 26, 68): Which CIP standard provided the result, tracking on-chain vs off-chain adoption.
- `cftr.api.subjects.queried`: Total subject lookups across all endpoints.
- `cftr.api.subjects.not_found`: Subjects queried but not found in any source.

**Gauges (point-in-time values)**:
- `cftr.tokens.cip26.count`: Total CIP-26 tokens in the registry.
- `cftr.tokens.cip68.count`: Total CIP-68 reference NFTs indexed.
- `cftr.sync.status`: Numeric sync state (0=not_started, 1=in_progress, 2=done, 3=error, 4=external_job).

**Implementation details**:
- `RegistryMetricsService` refreshes gauge values every 30 seconds (initial delay 5 seconds).
- `TimedAspect` bean enables `@Timed` annotation for method-level latency measurement.
- Metrics endpoint exposed at `/actuator/prometheus` for scraping.
- Application-level tag: `cf-token-metadata-registry`.

## Consequences

### Positive

- **Usage visibility**: Per-version query counts inform V1 deprecation decisions. CIP hit distribution shows the value of CIP-68 support.
- **Capacity planning**: Token count gauges and query rates help predict storage and compute requirements.
- **Alerting**: Sync status gauge enables alerts on sync failures. Not-found rate spikes may indicate upstream issues.
- **Standard format**: Prometheus format integrates with existing monitoring stacks (Grafana, Alertmanager) without custom tooling.

### Negative

- **Refresh overhead**: Gauge refresh queries the database every 30 seconds, adding minor load.
- **Metric cardinality**: Tag-based metrics must be managed carefully. Adding high-cardinality tags (e.g., per-subject) would cause metric explosion.
- **Counter reset on restart**: Prometheus counters reset when the application restarts. Rate-based queries handle this correctly, but raw counter values require `increase()` or `rate()` functions.

## Alternatives Considered

- **StatsD/Graphite**: Push-based metrics protocol. Less common in Kubernetes environments where Prometheus pull-based scraping is standard.
- **OpenTelemetry**: Vendor-neutral observability framework covering metrics, traces, and logs. More comprehensive but adds complexity; Micrometer already provides Prometheus integration with minimal configuration.
- **Application logs only**: Parse structured logs for metrics. Higher latency, harder to aggregate, and more expensive at scale than dedicated metrics.
