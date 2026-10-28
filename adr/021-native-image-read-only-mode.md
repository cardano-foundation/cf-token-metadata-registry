# ADR-021: A Separate Native Image for Read-Only Mode (Workaround)

## Status

Accepted, as a **temporary workaround**. Superseded once Yaci Store checks read-only mode at runtime
([bloxbean/yaci-store#1293](https://github.com/bloxbean/yaci-store/issues/1293)); then one native image serves both
roles again, and this ADR and the `native-readonly` variant are removed.

## Date

2026-10-22

## Context

The registry runs in two roles from the same code (see the `rw` and `ro` profiles of `docker-compose.yml`):
- **Indexer:** syncs CIP-26 from GitHub and indexes CIP-68 and CIP-113 from the chain.
- **Read-only API:** serves queries from the indexer's database, never connects to a node and never writes. It runs as
  replicas, for example Kubernetes pods behind a load balancer.

The role is chosen when the container starts: `STORE_READONLYMODE=true`, `STORE_SYNCAUTOSTART=false`,
`TOKEN_METADATA_SYNC_JOB=false` and the `readonly` Spring profile.

With the JVM image this works. With the published **native** image, the read-only API indexes anyway (#90):
- Yaci Store implements read-only mode as a Spring `Condition` (`ReadOnlyCondition`, reading `store.read-only-mode`).
  It removes the sync beans in read-only mode: `StartService`, `BlockFetchService`, `ChainTipService`, `HealthService`,
  and about ten more.
- A native image is built with Spring AOT, which evaluates conditions **once, at build time**. The image was built with
  the default (`false`), so it always contains the sync beans. Setting `STORE_READONLYMODE=true` when it runs changes
  nothing, and neither does `STORE_SYNCAUTOSTART=false`.
- It went unnoticed because the `api-ro` compose service builds the JVM Dockerfile by default. "Built from source
  works" compared a JVM image with a native one.

Reproduced on 2026-10-22 without a native build. A JVM run with `-Dspring.aot.enabled=true` uses the bean set AOT
generated, the same one a native image has. In read-only mode it logged "Application is ready. Let's start the sync
process ..." and wrote to `cursor_`. The same jar in normal JVM mode did neither.

Our own beans are not affected: none depends on a runtime property through `@Conditional` or `@Profile` (only
`@Profile("!test")` on repositories). The `readonly` profile only sets health-group properties, which are still read
at runtime.

## Decision

Build and publish **a second native image for read-only mode**, with read-only mode set while Spring AOT runs:

1. **Maven:** the `native` profile passes `store.read-only-mode=${native.read-only-mode}` (default `false`) to
   `process-aot`.
2. **Docker:** `api/Dockerfile.native` takes a `READ_ONLY_MODE` build argument (default `false`) and passes it to
   Maven.
3. **Images:** the `Earthfile` has an `api-native-readonly` target and `docker-build.yaml` a `native-readonly`
   variant. The image is published as `cf-token-metadata-registry-api-native-readonly:<version>`, next to
   `cf-token-metadata-registry-api:<version>` (JVM) and `cf-token-metadata-registry-api-native:<version>`.
4. **Compose:** the `api-ro` service passes `READ_ONLY_MODE=true`, so `API_DOCKERFILE=api/Dockerfile.native` builds
   the right image for it. The JVM Dockerfile ignores the argument.
5. **Nightly:** builds the read-only native image and runs a smoke test. It starts the image against an empty
   database with the `api-ro` settings, and fails if it is not ready, logs the start of a sync, or writes to `cursor_`.

**Which image for which role:**

| Role | JVM | Native |
|---|---|---|
| Indexer | `cf-token-metadata-registry-api:<version>` | `cf-token-metadata-registry-api-native:<version>` |
| Read-only API | `cf-token-metadata-registry-api:<version>`, started in read-only mode | **`cf-token-metadata-registry-api-native-readonly:<version>`**, started in read-only mode |

The read-only settings (`STORE_READONLYMODE=true`, `STORE_SYNCAUTOSTART=false`, `TOKEN_METADATA_SYNC_JOB=false`,
`SPRING_PROFILES_ACTIVE=readonly`) are still set when it runs: they configure the rest of the app, such as the health
groups and the CIP-26 job.

## Consequences

### Positive

- Kubernetes deployments can run read-only native replicas that never index.
- No change to the application code or to Yaci Store. The workaround is build and CI configuration, and is easy to
  remove.
- The nightly smoke test catches a regression of this kind.

### Negative

- **One more image:** a second native build in the publish and nightly workflows, which adds about 10–15 minutes of CI
  time each, and one more image to document.
- **The role is part of the image.** Starting the `-native` image in read-only mode still indexes, and starting the
  `-native-readonly` image without the read-only settings does not index. Operators must pick the image for the role.
- A temporary divergence from "one artifact per release", to be undone when Yaci Store is fixed.

## Alternatives Considered

- **Fix it in Yaci Store** (bloxbean/yaci-store#1293): check read-only mode at runtime instead of removing beans, so
  that one native image serves both roles. This is the real fix. It is requested upstream, and this ADR is withdrawn
  when it ships.
- **Guard it in the registry:** stop or veto the sync beans at runtime when read-only mode is requested, for example
  with a listener that runs before `StartService`. Rejected: it depends on Yaci Store internals (which beans start
  what, and in which order), and the node-facing beans would still exist.
- **Fail fast only:** refuse to start a native image built for indexing when read-only mode is requested. This replaces
  silent indexing with an error, but leaves native read-only deployments impossible. It could be added on top of this
  decision.
- **Document "use the JVM image for read-only":** the cheapest option, but it gives up the native image's memory and
  startup advantages (ADR-003) for exactly the replicas that scale out.
