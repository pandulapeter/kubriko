# Pin the published tool-scene-editor to the real debug menu instead of the Showcase flag

**Challenged:** amended — the guard is attached lazily to the tasks that actually write the publication (`tasks.withType<AbstractPublishToMaven>().configureEach`) instead of `tasks.matching { it.name.startsWith("publish") }`, which forces every task of the module to be realized during configuration of every build with the flag off (including the Showcase CI release builds), not only publishing ones.

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** desktop (published POM)
**Artifact:** `tool-scene-editor` (published dependency set)
**Files:** `tools/scene-editor/build.gradle.kts`, `tools/scene-editor/CLAUDE.md`

**Decision needed:** How should `tool-scene-editor`'s debug-menu dependency stop depending on a Showcase flag at publish time? Option A: always `projects.tools.debugMenu`. Option B: keep the Showcase swap for local builds but refuse to publish unless `showcase.isDebugMenuEnabled=true`. — recommended: option B (the released POM stays exactly what CI publishes today and the Showcase's flag combinations keep working).

## Problem

`tools/scene-editor/build.gradle.kts` (~34), in `desktopMain`:

```kotlin
implementation(if (project.findProperty("showcase.isDebugMenuEnabled") == "true") projects.tools.debugMenu else projects.tools.debugMenuNoop)
```

This is a published library, but which artifact its POM/Gradle module metadata lists (`tool-debug-menu` or `tool-debug-menu-noop`) depends on a Showcase-only flag in the publisher's `gradle.properties` at the moment of `publishToMavenCentral`. It is `true` in the committed file, so released versions list `tool-debug-menu`; publishing from a checkout where someone switched the flag off to test a release-like Showcase would silently ship an editor built against the noop — and with plan 65 unapplied, a noop that renders no canvas.

How it interacts with the Showcase: `app/shared/build.gradle.kts` (~44) and every example module pick `debugMenu` or `debugMenuNoop` with the same flag, so today all modules of one Showcase build agree. If the scene editor always used `debugMenu` (option A), a Showcase build with `showcase.isDebugMenuEnabled=false` and `showcase.isSceneEditorEnabled=true` would put both `tool-debug-menu` and `tool-debug-menu-noop` on the desktop runtime classpath — two `com.pandulapeter.kubriko.debugMenu.DebugMenu` classes, with the winner decided by classpath order. That combination is exactly what one uses to check the release-like Showcase with the editor still available, so option A would break it.

## Fix

Option B (recommended) — in `tools/scene-editor/build.gradle.kts`:

```kotlin
val isDebugMenuEnabled = project.findProperty("showcase.isDebugMenuEnabled") == "true"
...
implementation(if (isDebugMenuEnabled) projects.tools.debugMenu else projects.tools.debugMenuNoop)
...
if (!isDebugMenuEnabled) {
    tasks.withType<AbstractPublishToMaven>().configureEach {
        doFirst {
            throw GradleException("tool-scene-editor is published against tool-debug-menu: publish with showcase.isDebugMenuEnabled=true.")
        }
    }
}
```

(`import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven`.) `AbstractPublishToMaven` is the common type of `PublishToMavenLocal` and `PublishToMavenRepository`, so this covers `publishToMavenLocal` and the vanniktech `publishToMavenCentral` chain (whose per-publication `publish…PublicationTo…Repository` tasks are `PublishToMavenRepository`), and it fails before this module writes any POM. `withType(...).configureEach` is lazy: the Showcase CI workflows run `:app:desktop:createReleaseDistributable` / `:app:android:bundleRelease` / `:app:web:wasmJsBrowserDistribution` with `-Pshowcase.isDebugMenuEnabled=false`, and `./gradlew build` never runs a publish task, so neither realizes nor triggers the guard. The library workflow (`.github/workflows/library-publish.yml`, `./gradlew publishToMavenCentral --no-configuration-cache`) runs with the committed `true` and is unaffected. The `doFirst` action captures nothing from `project`, so it stays configuration-cache friendly. Other modules published in the same invocation may already have uploaded when this fails; `publishToMavenCentral()` is configured without automatic release, so nothing is released to Maven Central from a failed run. Local Showcase builds are unchanged.

Option A — replace the conditional with `implementation(projects.tools.debugMenu)`. Then the examples lane must stop combining the scene editor with `debugMenuNoop` (or accept duplicate classes on desktop when the debug menu flag is off).

`tools/scene-editor/CLAUDE.md`: add under Architecture (or a short "Build" note) "The debug menu dependency follows `showcase.isDebugMenuEnabled` for local Showcase builds; publishing requires the flag to be `true`, so the released artifact always depends on `tool-debug-menu`."

## Tests

None: build configuration. Verify with Gradle (executor, not the plan writer):
- `./gradlew -Pshowcase.isDebugMenuEnabled=false :tools:scene-editor:publishToMavenLocal` fails with the message above;
- `./gradlew :tools:scene-editor:generatePomFileForDesktopPublication` (flag `true`) succeeds and the POM under `tools/scene-editor/build/publications/desktop/` lists `tool-debug-menu` and not `tool-debug-menu-noop` (drop this check if the publication is named differently; the point is the dependency list).

## Manual check

None beyond the Gradle checks above.
