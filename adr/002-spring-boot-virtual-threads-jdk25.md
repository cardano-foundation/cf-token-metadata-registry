# ADR-002: Spring Boot 3 with Virtual Threads on JDK 25

## Status

Accepted

## Date

2026-03-24

## Context

The registry API handles concurrent metadata queries and background blockchain synchronization. Traditional platform threads impose memory overhead and limit scalability under high concurrency. With the project targeting modern JDK releases, we had the opportunity to adopt virtual threads (Project Loom), which became production-ready in JDK 21 and have matured further in JDK 25.

Spring Boot 3.5.x provides first-class support for virtual threads through a single configuration property, making adoption straightforward.

## Decision

We run the application on JDK 25 with Spring Boot 3.5.12 and enable virtual threads via:

```properties
spring.threads.virtual.enabled=true
```

This causes Spring Boot to use virtual threads for request handling (Tomcat), scheduled tasks, and async operations. The Docker images use Eclipse Temurin 25 (both JDK for build and JRE for runtime).

Key version choices in the current stack:
- Java 25
- Spring Boot 3.5.12
- Yaci Store 2.0.0
- Cardano Client Library 0.7.1
- PostgreSQL 17

## Consequences

### Positive

- **Improved concurrency**: Virtual threads allow the API to handle many more concurrent requests without increasing memory consumption, particularly beneficial during bulk metadata queries.
- **Simplified async code**: No need for reactive programming or manual thread pool tuning. Blocking I/O (database, Git operations) works naturally with virtual threads.
- **Future-proof**: JDK 25 provides access to the latest language features, security patches, and performance improvements.
- **Spring ecosystem alignment**: Spring Boot 3.5.x is designed for JDK 21+ with full virtual thread integration across web, data, and messaging starters.

### Negative

- **Runtime requirement**: Deployments must use JDK 25+, which may not be available in all environments.
- **Library compatibility**: Some libraries may not be fully compatible with virtual threads (e.g., those using `ThreadLocal` extensively or `synchronized` blocks for I/O). This requires validation of the dependency tree.
- **Debugging complexity**: Virtual thread stack traces and thread dumps differ from platform threads, requiring updated tooling and operational knowledge.

## Alternatives Considered

- **JDK 21 LTS**: More conservative choice with virtual thread support, but misses improvements in JDK 22-25. We chose to stay current given the containerized deployment model.
- **Reactive stack (WebFlux)**: Non-blocking by design but imposes a reactive programming model throughout the codebase, significantly increasing complexity for a primarily CRUD-like API.
- **Platform threads with tuned pools**: The traditional approach, but virtual threads provide better scalability with less configuration.
