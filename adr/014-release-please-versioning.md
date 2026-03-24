# ADR-014: Release-Please for Automated Versioning

## Status

Accepted

## Date

2026-03-24

## Context

The project follows semantic versioning and produces multiple artifacts (API JAR, Job JAR, Docker images). Manually managing version numbers, changelogs, and release tags is error-prone and creates friction in the release process. The version number must be updated consistently across the root `pom.xml` (via the `${revision}` property), the `.release-please-manifest.json`, and Docker image tags.

## Decision

We use Google's Release-Please GitHub Action (`google-github-actions/release-please-action@v4`) for automated release management:

1. **Conventional Commits**: Developers write commit messages following the Conventional Commits specification (e.g., `feat:`, `fix:`, `chore:`). Release-Please parses these to determine version bumps.

2. **Release type**: Configured as `simple` release type, appropriate for a single-version multi-module Maven project.

3. **Version sources**:
   - `.release-please-manifest.json`: Stores the current version (`{"." : "1.5.0"}`).
   - `pom.xml`: Version updated via XPath `//project/properties/revision` as an extra file.

4. **Release workflow**: On push to `main`, Release-Please either:
   - Creates/updates a release PR with changelog and version bump, or
   - If a release PR is merged, creates a GitHub release with tags.

5. **Downstream automation**: The release event triggers Docker image builds via the `docker-build.yaml` workflow, producing tagged images (`version-jvm`, `version-native`, `latest`).

6. **License**: The project uses MPL 2.0, included in release artifacts.

## Consequences

### Positive

- **Automated changelogs**: Release notes are generated from commit messages, ensuring comprehensive and consistent documentation.
- **Consistent versioning**: Version numbers are updated atomically across all configuration files.
- **Low friction releases**: Merging the release PR is the only manual step; everything else is automated.
- **Audit trail**: Every release is tied to a specific set of commits with clear categorization (features, fixes, chores).

### Negative

- **Commit discipline required**: Developers must follow Conventional Commits conventions. Poorly formatted commits result in inaccurate changelogs or missed version bumps.
- **Release PR noise**: Release-Please continuously updates a release PR on every push to `main`, which can be noisy in repositories with frequent commits.
- **Single version constraint**: All modules share the same version, which prevents independent module releases. This is acceptable given the tightly coupled nature of the modules.

## Alternatives Considered

- **Maven Release Plugin**: The standard Maven approach for releases. Handles version bumps and tagging but doesn't generate changelogs and requires more manual intervention.
- **Manual versioning**: Direct edits to `pom.xml` and manual tagging. Most flexible but most error-prone, especially for coordinating version numbers across files.
- **Semantic Release**: Similar to Release-Please but Node.js-based. Release-Please has better GitHub Actions integration and is maintained by Google.
- **JReleaser**: Java-focused release tool with support for multiple package managers. More powerful but more complex to configure for our simple release needs.
