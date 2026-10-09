# Rename the build-logic PublishingExtension to ArtifactMetadataExtension and give it its own file

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (gradle/build-logic)
**Files:**
- `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/plugins/PublicArtifactPlugin.kt`
- `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/plugins/ArtifactMetadataExtension.kt` (new)

## Problem

`PublicArtifactPlugin.kt` declares a second top-level class whose name shadows Gradle's own
`org.gradle.api.publish.PublishingExtension` (the type behind the `publishing { }` block), although it is registered
as `artifactMetadata`:

```kotlin
open class PublishingExtension {                       // :19
    var artifactId: String? = null
}
...
val extension = project.extensions.create("artifactMetadata", PublishingExtension::class.java)   // :29
```

## Fix

1. Create `plugins/ArtifactMetadataExtension.kt` (MPL-2.0 header copied from `PublicArtifactPlugin.kt`, package
   `com.pandulapeter.kubriko.buildLogic.plugins`) holding the class renamed:
   ```kotlin
   open class ArtifactMetadataExtension {
       var artifactId: String? = null
   }
   ```
2. In `PublicArtifactPlugin.kt` delete the old class and change the registration to
   `project.extensions.create("artifactMetadata", ArtifactMetadataExtension::class.java)`.

The DSL name `artifactMetadata` is unchanged, so no module build file changes (Gradle's generated Kotlin DSL accessor
is typed from the registered class and regenerated on the next build). Grep the repository (and `../Tesselar`, which
does not apply the plugin) for `PublishingExtension` afterwards: at 2480325f the two lines above are the only uses.

## Behaviour
Unchanged; the extension keeps its registered name and property.

## Public API
None (build-logic is an `includeBuild`, not published).

## Tests
None possible beyond the build itself.

## Verify
`./gradlew publishToMavenLocal --dry-run --no-configuration-cache` (configures every `kubriko-public-artifact` module,
including the `afterEvaluate` block that reads the extension), then
`./gradlew :engine:compileKotlinDesktop :plugins:collision:compileKotlinDesktop`

## Manual check
none
