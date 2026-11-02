<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract the Scene editor's runtime (`sceneRuntime`: project class loading, the live preview, sprites) into `:creator:scene-runtime`, with the engine and plugin dependencies

**Challenged:** amended — `tools:creator-api` is `api` (`LoadedEntryPoint.entryPoint` is a `CreatorEntryPoint`, and the workspace tests implement one); `Editable` reaches Creator through creator-api's `api(projects.tools.sceneEditorApi)`, not creator-session.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (scene-runtime; "engine deps to scene-runtime")  ·  **Depends on:** M12
**Files:**
- `settings.gradle.kts`; new `creator/scene-runtime/build.gradle.kts`; `creator/build.gradle.kts` (the engine, plugin, and `creator-api` dependencies and their comment)
- moved (main and test): `sceneRuntime/**` (M06's package) → `creator/scene-runtime/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/sceneRuntime/`, with `TestEntryPoint.kt` and any test resources `ClassLoaderEntryPointLoaderTest` loads
- importers of newly public declarations (visibility only); `PackageLayeringTest.kt`; `creator/CLAUDE.md` (Scene editing: the class-sharing rule's file path; Modules list)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

`sceneRuntime/` (the Scene editor's runtime since M06: `ClassLoaderEntryPointLoader`, `ProjectClassLoader`, `SceneEditingController`,
`SceneView`, `PreviewActor`, `ProjectSpriteLoader`, …) is the only Creator code that runs the engine. It loads a project's classes
next to Creator's own, which only works if every published Kubriko module the project could use is on Creator's classpath, so the
app's build lists them all:

```kotlin
// creator/build.gradle.kts
    // Every published module a project could use, so that a project's classes loaded next to Creator's share them
    // (scenes/ClassLoaderEntryPointLoader.kt).
    implementation(projects.plugins.audioPlayback)
    …
    implementation(projects.plugins.sprites)
    implementation(projects.tools.creatorApi)
    implementation(projects.tools.creatorSession)
```

It uses core (Scene files, `ActorShape`, the codegen's `SceneSettingsCodeGenerator`), gradle-session (`ProjectClasspath`,
`SessionInitScript`), and foundation; Compose UI graphics and Skia (`org.jetbrains.skia.Image`, `Surface`) for previews; the engine
(`com.pandulapeter.kubriko.*`), the sprites, keyboard, pointer, and serialization plugins, `creator-api` (`CreatorEntryPoint`), and
`sceneEditor.Editable`/`EditableMetadata` (from `tools/scene-editor-api`, which `tools/creator-api` exposes with
`api(projects.tools.sceneEditorApi)`; `tools/creator-session`'s `implementation(projects.tools.sceneEditor)`, which S05 removes, only
ever reached the runtime classpath).

## Fix

1. `settings.gradle.kts`: `":creator:scene-runtime",`. `creator/scene-runtime/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.core)
       api(projects.creator.gradleSession)
       api(projects.engine)
       api(compose.desktop.currentOs)
       // Every published module a project could use, so that a project's classes loaded next to Creator's share them
       // (sceneRuntime/ClassLoaderEntryPointLoader.kt).
       implementation(projects.plugins.audioPlayback)
       … (the eleven plugins, moved verbatim)
       api(projects.tools.creatorApi)
       testImplementation(testFixtures(projects.creator.foundation))
   }
   ```
   `compose.desktop.currentOs` needs the Compose plugin's `compose` accessor: apply `alias(libs.plugins.compose)` (no compiler
   plugin; no `@Composable` here) or depend on the explicit artifacts the code imports (`libs.compose.ui`); pick whichever the
   build accepts without the compiler plugin, and say which in the commit's report. `api(projects.engine)` because `SceneView`'s and
   `SceneEditingState`'s public members expose engine types that `ui/scenes` uses (`ui/scenes/SceneCanvas.kt` imports
   `com.pandulapeter.kubriko`); keep the plugins `implementation` (they reach the app's runtime classpath, which is all the class
   sharing needs). `api(projects.tools.creatorApi)` because `LoadedEntryPoint.entryPoint` is a public `CreatorEntryPoint` and
   `workspace/WorkspaceControllerTest` implements one (with `implementation`, `:creator:workspace`'s tests wouldn't compile); it also
   brings `scene-editor-api` (`Editable`) as its own `api`, so that needs no line of its own. `ProjectSpriteLoader`'s
   `DrawableResource` comes from the sprites plugin's API; add `implementation(libs.compose.resources)` only if the compiler asks. Remove the moved lines and their comment from `creator/build.gradle.kts`; the app adds
   `implementation(projects.creator.sceneRuntime)`.
2. `git mv` the package and its tests (`ClassLoaderEntryPointLoaderTest` writes a probe class from the test classpath into a
   temporary folder: the probe and `TestEntryPoint` move with it; S02's leak test too).
3. **API.** Public: what `workspace` and `ui/scenes` use (`SceneEditingController`, `SceneEditingState`, `SceneView`, `ActorTypeLook`,
   `ActorBodyValues`, `EntryPointLoader`, `ClassLoaderEntryPointLoader`, `createDisposingIfCancelled`, …). Keep the class loaders
   and the sprite loading internal where only this module calls them.
4. `PackageLayeringTest`: the scene-runtime layer's files must all be under `creator/scene-runtime/src`.
5. `creator/CLAUDE.md`: the Scene editing section's rule that every published module is on Creator's classpath now points at
   `scene-runtime/build.gradle.kts`; add `scene-runtime` to the Modules list.

## Tests

- `:creator:scene-runtime:test` runs the moved tests; `ClassLoaderEntryPointLoaderTest` still loads a project class next to the
  engine's and shares `Kubriko` with it (the class-identity check is what proves the plugins are still on the classpath).
- `./gradlew :creator:allTests` with the same test count as before.

## Manual check

`./gradlew :creator:run`, open a Scene of the scratch Breakout copy: Actors draw (sprites included), a hand-written Actor type shows,
and swapping classes after a type change works (the class loader finds the engine through the module's runtime classpath).
