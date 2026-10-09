# Fail the build when a published module sets no artifactId instead of publishing it under an empty one

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (gradle/build-logic)
**Files:**
- `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/plugins/PublicArtifactPlugin.kt`
- `gradle/build-logic/CLAUDE.md`

Runs after E04 (same file; the extension class is then `ArtifactMetadataExtension`).

## Problem

```kotlin
project.afterEvaluate {
    extensions.configure<MavenPublishBaseExtension> {
        configurePublicArtifact(
            extension = this,
            artifactId = extension.artifactId.orEmpty(),      // PublicArtifactPlugin.kt:34
        )
    }
}
```

A module that applies `kubriko-public-artifact` but forgets `artifactMetadata { artifactId = "..." }` configures
publishing with an empty artifact id, which only surfaces as a confusing failure (or a malformed coordinate) at
publish time. Every published module sets it at 2480325f (checked all 20 build files applying the plugin), so nothing
fails today.

## Fix

Replace `extension.artifactId.orEmpty()` with

```kotlin
artifactId = checkNotNull(extension.artifactId?.takeIf { it.isNotBlank() }) {
    "$path applies kubriko-public-artifact but sets no artifactMetadata { artifactId = \"...\" }."
},
```

and add one line to the Gotchas of `gradle/build-logic/CLAUDE.md`, next to the existing `artifactMetadata` bullet:
"A module applying `kubriko-public-artifact` without an `artifactId` fails at configuration time."

## Behaviour
Unchanged for every module in the repository (all set an id). A misconfigured new module now fails at configuration
with a message naming it, instead of later.

## Public API
None.

## Tests
None possible beyond the build itself.

## Verify
`./gradlew publishToMavenLocal --dry-run --no-configuration-cache` (configures every published module through the
`afterEvaluate` block).

## Manual check
none
