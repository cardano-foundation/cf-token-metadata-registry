# ADR-008: JGit for Git Operations

## Status

**Superseded by [ADR-017](017-thin-wrapper-over-assets-ext.md)** — the rationale (use JGit instead of shelling out to `git`) is unchanged, but the code was removed from this repo. JGit is now a transitive dependency of yaci-store's `assets-ext` extension, which owns the `GitService` and the cardano-token-registry clone/pull/diff logic.

## Date

2026-03-24 (original); superseded 2026-04-23

## Context

The registry synchronizes CIP-26 token metadata from the `cardano-foundation/cardano-token-registry` GitHub repository. This requires Git operations: cloning the repository, pulling updates, computing diffs to identify changed files, and extracting commit hashes for incremental sync tracking.

The original implementation used shell commands (`git clone`, `git pull`, `git diff`, etc.) executed via `Runtime.exec()` or `ProcessBuilder`. This approach had several problems:

- **Shell injection risk**: Constructing shell commands with user-configurable parameters (organization name, project name) could allow command injection if inputs were not properly sanitized.
- **Platform dependency**: Shell commands behave differently across operating systems, making the application harder to test and deploy on non-Linux platforms.
- **Error handling**: Parsing stdout/stderr from shell commands for error detection is fragile and error-prone.

## Decision

We replaced all shell-based Git operations with JGit 7.1.0, a pure Java implementation of the Git protocol. The `GitService` class uses JGit for:

- **Clone**: `Git.cloneRepository()` to clone the token registry repository to a configurable temporary directory.
- **Pull with rebase**: `git.pull().setRebase(true)` to update the local clone with upstream changes.
- **Diff**: `DiffCommand` with `DiffEntry` to identify files changed between two commits, enabling incremental sync.
- **Commit hash extraction**: `Repository.resolve("HEAD")` to get the current HEAD commit hash for sync state tracking.
- **Resource cleanup**: `@PreDestroy` lifecycle hook ensures Git resources are properly closed on application shutdown.

## Consequences

### Positive

- **Security**: Eliminates shell injection vectors entirely. No user input is ever passed to a shell.
- **Cross-platform**: Works identically on Linux, macOS, and Windows without requiring Git to be installed.
- **Type safety**: JGit provides a Java API with proper exceptions, return types, and null safety, replacing fragile string parsing.
- **Testability**: JGit operations can be mocked in unit tests without needing a real Git binary or shell.
- **Resource management**: JGit's `Repository` and `Git` objects implement `Closeable`, integrating naturally with Java resource management.

### Negative

- **Dependency size**: JGit adds a non-trivial dependency to the classpath (~3MB), though this is negligible in a containerized deployment.
- **API complexity**: JGit's API is lower-level than shell commands for some operations, requiring more code for equivalent functionality.
- **Feature parity**: Some advanced Git features may not be available or may work differently in JGit compared to the C Git implementation.

## Alternatives Considered

- **Shell commands with sanitization**: Continue using shell commands but add input sanitization. Still platform-dependent and error-prone; defense-in-depth favors eliminating the attack surface entirely.
- **GitHub REST API**: Fetch repository content via the GitHub API instead of cloning. Avoids Git entirely but introduces API rate limits, requires authentication for private repos, and makes incremental diff computation more complex.
- **GitHub webhooks**: Push-based notification of changes. Would require exposing a webhook endpoint, introducing network security considerations. Better for latency but more complex to implement reliably.
