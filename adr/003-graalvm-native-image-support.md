# ADR-003: GraalVM Native Image Support

## Status

**Deprecated** — [ADR-017](017-thin-wrapper-over-assets-ext.md) deleted `NativeImageConfig` because the entities and REST DTOs it registered reflection hints for no longer live in this repo. A fresh hints configuration scoped to the controllers, `V1TokenMetadataMapper`, and assets-ext's types is needed before native builds can be restored. Tracked as a follow-up.

## Date

2026-03-24 (original); deprecated 2026-04-23

## Context

Container startup time and memory footprint matter in cloud-native deployments, particularly for Kubernetes environments where pods need to scale quickly and health probes have tight timeouts. GraalVM native images compile Java applications ahead-of-time (AOT) into standalone executables, drastically reducing startup time and memory usage at the cost of build complexity and some runtime optimizations.

## Decision

We provide GraalVM native image support as an alternative build target alongside the standard JVM distribution. This is implemented through:

1. **NativeImageConfig class**: Registers reflection hints for JPA entities (`TokenMetadata`, `TokenLogo`, `MetadataReferenceNft`, etc.) and REST model classes that require runtime reflection for Jackson serialization and JPA proxying.

2. **Resource hints**: Registers Flyway migration files (`db/migration/postgresql/*`, `db/store/*`) as native image resources since they are loaded at runtime.

3. **Maven native profile**: Uses `paketobuildpacks/builder-jammy-tiny` for native image compilation with flags `--no-fallback` and `-H:+ReportExceptionStackTraces`.

4. **Dockerfile.native**: Dedicated Dockerfile for building and running native executables.

5. **CI pipeline**: The `docker-build.yaml` workflow supports a `variant` parameter (`jvm` or `native`) for building either distribution.

## Consequences

### Positive

- **Fast startup**: Native images start in milliseconds rather than seconds, enabling faster pod scheduling and scaling.
- **Lower memory**: Reduced heap and metaspace requirements compared to JVM mode.
- **Deployment flexibility**: Operators can choose between JVM (better peak throughput, easier debugging) and native (faster startup, lower footprint) based on their needs.
- **CI validation**: Building native images in CI catches reflection and resource issues early.

### Negative

- **Build time**: Native image compilation is significantly slower than JVM compilation (minutes vs seconds).
- **Reflection configuration maintenance**: Every new JPA entity or REST model class must be registered in `NativeImageConfig`, creating an ongoing maintenance burden.
- **Reduced runtime optimization**: Native images lack JIT compilation, which may result in lower peak throughput for CPU-intensive operations.
- **Debugging difficulty**: Native executables have limited debugging support compared to JVM applications.

## Alternatives Considered

- **JVM-only distribution**: Simpler to maintain but loses the startup time advantage for Kubernetes deployments.
- **CRaC (Coordinated Restore at Checkpoint)**: Provides fast startup through checkpoint/restore without AOT compilation, but requires more complex container setup and has security implications for checkpointed state.
- **Spring Boot class data sharing (CDS)**: A lighter-weight optimization that improves startup without full AOT compilation. Could complement native images but provides less dramatic improvement.
