<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Refuse to draw the Scenes of a project whose Compose or Kotlin is a newer major or minor version than Creator's, and warn about any other newer shared library

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** INF-4  ·  **Depends on:** none
**Files:** `creator/src/main/resources/kubriko-creator-session.init.gradle.kts`, `creator/build.gradle.kts`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/ProjectClasspath.kt`, new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/SharedLibraries.kt`, new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/foundation/CreatorLibraries.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/SceneEditingController.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/SceneEditingState.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt` (one constructor parameter, passed through), `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/GradleWorkspaceOpener.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceScreen.kt` (the `Unsupported` branch), `creator/src/main/composeResources/values/strings.xml`, tests `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/scenes/ProjectClasspathTest.kt`, new `scenes/SharedLibrariesTest.kt`, new `foundation/CreatorLibrariesTest.kt`, `scenes/SceneEditingControllerTest.kt`, `template/ProjectTemplateIntegrationTest.kt`; `creator/CLAUDE.md` (Creator/init-script paragraph and Scene editing paragraph)

## Problem

Creator loads a project's classes into a parent-first class loader over its own (`scenes/ClassLoaderEntryPointLoader.kt`):

```kotlin
internal fun classLoaderOf(
    files: List<File>,
    copies: File,
    parent: ClassLoader = ClassLoaderEntryPointLoader::class.java.classLoader,
) = ProjectClassLoader(
```

so every class Creator itself has (Compose, Kotlin's standard library, coroutines, kotlinx-serialization, Skiko, and the rest of its dependencies) comes from Creator, whatever version the project resolved. The only gate is the Kubriko version, in `SceneEditingController.buildAndLoad`:

```kotlin
return when {
    classpath == null -> LoadResult.Failed(Res.string.scene_build_failed)
    classpath.kubrikoVersion != creatorKubrikoVersion -> LoadResult.Unsupported(classpath.kubrikoVersion)
    else -> try {
```

A project on Creator's Kubriko version but a newer Compose or Kotlin (a project can raise them in its own catalog) is compiled against APIs Creator's copies don't have. A `LinkageError` while loading the entry point is reported as "not loaded", but a `NoSuchMethodError` later, while an Actor is created or drawn, fails single Actors silently (`SceneView.createActor` swallows `LinkageError`) or fails the Scene with a confusing problem. The user is never told the real reason: the project's libraries are newer than Creator's.

The decision (`decisions.md`: "Classloader gate: refuse a project whose Compose/Kotlin is a newer major/minor than Creator's; warn otherwise"): refuse with a clear reason for a newer major or minor of Compose or Kotlin; load, with a console warning, when a newer patch of those or a newer version of any other library Creator shares is found.

## Fix

The versions come from Gradle's own resolution on both sides, not from jar paths: the init script writes the project's resolved module versions into the classpath file, and Creator's build bundles its own.

1. **Init script** (`kubriko-creator-session.init.gradle.kts`, task `kubrikoCreatorClasspath`). At registration, next to `val version = kubrikoVersion(runtime)`, capture the lazy artifacts provider (configuration-cache safe):

   ```kotlin
   val resolvedArtifacts = runtime.incoming.artifacts.resolvedArtifacts
   ```

   In `doLast`, build the lines

   ```kotlin
   val libraries = resolvedArtifacts.get()
       .mapNotNull { it.id.componentIdentifier as? org.gradle.api.artifacts.component.ModuleComponentIdentifier }
       .map { "library.${it.group}:${it.module}=${it.version}" }
       .distinct()
   ```

   and write them after `launch + hotLaunch`:

   ```kotlin
   file.writeText((listOf("kubrikoVersion=$version") + (folders + classpath.files).map { it.absolutePath } + launch + hotLaunch + libraries).joinToString("\n"))
   ```

   Extend the script's header comment by one clause ("…and the versions its runtime classpath resolved (the "library." lines), for Kubriko Creator to check against its own"). The script's file name is a hash of its content (`SessionInitScript.file`), so the changed script gets a new path and Gradle's configuration cache misses once per project; nothing else changes for the build.

2. **Reading the classpath file** (`scenes/ProjectClasspath.kt`). Add `val libraries: Map<String, String> = emptyMap()` (`"group:module"` to version) and a `LIBRARY_PREFIX = "library."`. In `parse`, partition the `library.` lines out before the `launch.` partition, and split each at its last `=` (`substringBeforeLast('=')` / `substringAfterLast('=')`; skip a line without `=`). Update the KDoc ("…and the versions of the libraries it resolved ([libraries], empty in a file written by an older Kubriko Creator)").

   **How older classpath files are read:** a file without `library.` lines parses exactly as today with `libraries` empty, and an empty map passes the check below with no refusal and no warning (the behavior before this plan). In practice such a file is only a leftover: `SceneEditingController.readClasspath` deletes the file once read, and `GameBuilds` and `SceneEditingController` only read a file right after `runTask` succeeded, which runs the current Creator's own script (named after its content), so every file read was written by the same Creator. The other direction is harmless too: an older Creator open on the same project at the same time (the classpath file's path is the same for every Creator) reads the `library.` lines as relative file paths that don't exist, which `URLClassLoader`, `GameProcess.snapshotOf` (`!entry.exists() -> listOf(entry)`) and `java -cp` all skip. State both facts in one sentence of the `ProjectClasspath` KDoc, not more.

3. **Creator's own versions.** In `creator/build.gradle.kts`, add a task next to `generateCreatorProperties` that writes `creator-libraries.txt` (one `group:module=version` line per external module of `runtimeClasspath`, sorted) into its own output directory, registered as a resources source dir like the properties:

   ```kotlin
   val generateCreatorLibraries = tasks.register("generateCreatorLibraries") {
       val outputDirectory = layout.buildDirectory.dir("generated/creatorLibraries")
       val artifacts = configurations.runtimeClasspath.flatMap { it.incoming.artifacts.resolvedArtifacts }
       inputs.property("libraries", artifacts.map { set -> set.mapNotNull { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.let { id -> "${id.group}:${id.module}=${id.version}" } }.distinct().sorted() })
       outputs.dir(outputDirectory)
       doLast { outputDirectory.get().file("creator-libraries.txt").asFile.writeText(inputs.properties.getValue("libraries").let { @Suppress("UNCHECKED_CAST") (it as List<String>) }.joinToString("\n")) }
   }
   sourceSets.main { resources.srcDir(generateCreatorLibraries) }
   ```

   (Shape it as the build-logic style prefers; the requirement is the resolved versions of the runtime classpath, configuration-cache compatible, with the repository's own projects left out, since they are `ProjectComponentIdentifier`s.) New `foundation/CreatorLibraries.kt`: `internal object CreatorLibraries { fun load(): Map<String, String> }` reading that resource through the class loader, `internal fun parse(text: String): Map<String, String>` (same line format as step 2, without the prefix), and an `error(…)` like `CreatorConfiguration.load()` when the resource is missing. Read it once (a `by lazy` in the object is fine; it is a small resource read, called from `GradleWorkspaceOpener.open`, which already runs on a coroutine; wrap the first read in `withContext(Dispatchers.IO)` there as the opener does for other reads).

4. **The check** (new `scenes/SharedLibraries.kt`, pure logic):

   ```kotlin
   internal object SharedLibraries {
       /** A library both sides have, at a newer version in the project. */
       data class NewerLibrary(val module: String, val projectVersion: String, val creatorVersion: String)

       sealed interface Verdict {
           data object Compatible : Verdict
           /** Loads, but the console says which libraries are newer. */
           data class Newer(val libraries: List<NewerLibrary>) : Verdict
           /** Doesn't load: [library] is a newer major or minor of Compose or Kotlin. */
           data class Refused(val library: NewerLibrary) : Verdict
       }

       fun check(project: Map<String, String>, creator: Map<String, String>): Verdict
   }
   ```

   Rules: compare only modules present in both maps, skipping group `io.github.pandulapeter.kubriko` (already gated by the exact Kubriko version). Compare versions with `KubrikoBuildFiles.compareVersions` (already in `project/`, ignores what follows a dash). A module is *gated* when its group is `org.jetbrains.compose` or starts with `org.jetbrains.compose.`, or it is `org.jetbrains.kotlin:kotlin-stdlib`; a gated module whose first two numbers are higher in the project than in Creator is `Refused` (the first one, by module name, so the message is deterministic). Every other module whose project version compares higher is listed in `Newer`, sorted by module. Equal or older is fine (Creator's newer copy keeps the old API).

5. **SceneEditingController.** New constructor parameter `creatorLibraries: Map<String, String> = emptyMap()` (KDoc: "The versions of the libraries Kubriko Creator itself runs with, which the project's classes share ([SharedLibraries])"). In `buildAndLoad`, after the Kubriko version check:

   ```kotlin
   classpath.kubrikoVersion != creatorKubrikoVersion -> LoadResult.Unsupported(Res.string.scene_unsupported, listOf(creatorKubrikoVersion, classpath.kubrikoVersion))
   else -> when (val verdict = SharedLibraries.check(classpath.libraries, creatorLibraries)) {
       is SharedLibraries.Verdict.Refused -> LoadResult.Unsupported(Res.string.scene_unsupported_library, verdict.library.let { listOf(it.module, it.projectVersion, it.creatorVersion) })
       else -> { reportNewer(verdict); load(classpath, looks) }   // the existing try/catch, moved into a private function
   }
   ```

   `reportNewer` appends `ConsoleEntry.Status(Res.string.status_scene_newer_libraries, arguments = listOf(list))` where `list` is `"<module> <projectVersion> (Creator: <creatorVersion>)"` joined with `", "`, and only when that set differs from the one reported last (a private `var lastReportedNewer: List<NewerLibrary>`; `Compatible` resets it), since every change rebuilds and would otherwise repeat the warning. The controller is main-thread confined, as the class already is; `buildAndLoad` runs on `scope`, whose dispatcher is the main one, so the field needs no lock.
   `LoadResult.Unsupported` becomes `data class Unsupported(val reason: StringResource, val arguments: List<String>)`, and `SceneEditingState.Unsupported` becomes `Unsupported(scene, reason: StringResource, arguments: List<String>)` with its KDoc saying "The project's Kubriko version isn't Creator's, or its Compose or Kotlin is newer, so its classes can't be drawn in Creator's window." `ui/WorkspaceScreen.kt`'s branch becomes `message = stringResource(state.reason, *state.arguments.toTypedArray())`. Check with grep that nothing else reads `projectKubrikoVersion` (today only `WorkspaceScreen.kt`).

6. **Wiring:** `WorkspaceController` gains `creatorLibraries: Map<String, String> = emptyMap()` next to `creatorKubrikoVersion`, passed to `SceneEditingController`; `GradleWorkspaceOpener` passes `CreatorLibraries.load()`.

7. **Strings** (`strings.xml`, next to `scene_unsupported`):
   - `scene_unsupported_library`: "Kubriko Creator can't draw this project's Scenes: it uses %1$s %2$s, newer than the %3$s Kubriko Creator runs with. Playing the game still works."
   - `status_scene_newer_libraries`: "This project uses newer libraries than Kubriko Creator runs with, so its Scenes may not look as they do in the game: %1$s."

8. **creator/CLAUDE.md:** in the Creator paragraph, the `kubrikoCreatorClasspath` parenthesis gains "…and the versions its runtime classpath resolved (`library.` lines)"; in Scene editing, "When the project's Kubriko version equals Creator's" becomes "When the project's Kubriko version equals Creator's and its Compose and Kotlin are no newer major or minor version (`SharedLibraries`; another newer shared library loads with a console warning)". Keep both additions to that length (the file has a word budget).

## Tests

- `ProjectClasspathTest.libraryLinesAreReadAsVersionsAndNotAsFiles`: parse `"kubrikoVersion=1.0.0\n/game/classes\nlibrary.org.jetbrains.kotlin:kotlin-stdlib=2.4.21\nlibrary.org.jetbrains.compose.runtime:runtime-desktop=1.12.2\n"` → `files == listOf(File("/game/classes"))`, `libraries == mapOf("org.jetbrains.kotlin:kotlin-stdlib" to "2.4.21", "org.jetbrains.compose.runtime:runtime-desktop" to "1.12.2")`.
- `ProjectClasspathTest.aFileWithoutLibraryLinesHasNoLibraries`: the existing literal `"kubrikoVersion=1.0.0\n/game/build/libs/game.jar\n\n/cache/engine.jar\n"` parses with `libraries` empty (the older-file case).
- `SharedLibrariesTest` (literals only):
  - `sameVersionsAreCompatible`, `olderProjectVersionsAreCompatible`, `modulesOnlyOneSideHasAreIgnored`, `noProjectLibrariesAreCompatible` (empty project map → `Compatible`).
  - `aNewerMinorOfComposeIsRefused`: project `org.jetbrains.compose.ui:ui-desktop=1.13.0`, Creator `1.12.2` → `Refused(NewerLibrary("org.jetbrains.compose.ui:ui-desktop", "1.13.0", "1.12.2"))`; same for a pre-release `1.13.0-alpha01`.
  - `aNewerMinorOfKotlinIsRefused`: `org.jetbrains.kotlin:kotlin-stdlib` `2.5.0` vs `2.4.21`.
  - `aNewerPatchOfComposeOrKotlinOnlyWarns`: `1.12.3` vs `1.12.2` → `Newer` listing it.
  - `aNewerVersionOfAnotherLibraryOnlyWarns`: `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm=1.12.0` vs `1.11.0`, and `kotlin-reflect 2.5.0` (not gated) → `Newer`, sorted by module.
  - `kubrikoModulesAreLeftToTheKubrikoVersionCheck`: group `io.github.pandulapeter.kubriko` newer → `Compatible`.
- `CreatorLibrariesTest.linesAreReadAsModulesAndVersions` (parse of a literal) and `creatorsOwnLibrariesAreBundled`: `CreatorLibraries.load()` contains `org.jetbrains.kotlin:kotlin-stdlib` and some `org.jetbrains.compose.runtime:` module (the test runtime has the generated resource, since `processResources` includes it); fails if step 3 is missing.
- `SceneEditingControllerTest` (the fake `runTask` writes the file; add a `var projectLibraries = ""` appended to what it writes):
  - `aSceneOfAProjectOnANewerComposeCantBeEdited`: `projectLibraries = "\nlibrary.org.jetbrains.compose.runtime:runtime-desktop=9.0.0"`, controller with `creatorLibraries = mapOf("org.jetbrains.compose.runtime:runtime-desktop" to "1.12.2")` → state `Unsupported(scene, Res.string.scene_unsupported_library, listOf("org.jetbrains.compose.runtime:runtime-desktop", "9.0.0", "1.12.2"))`, `loadedClasspaths` empty.
  - `newerOtherLibrariesAreReportedOnceAndTheSceneOpens`: a newer coroutines line → state `Open`, one `status_scene_newer_libraries` report; `markOutdated()` and let the rebuild finish → still exactly one such report.
  - Update `aSceneOfAProjectOnAnotherVersionCantBeEdited` to the new `Unsupported(scene, Res.string.scene_unsupported, listOf("1.0.0", "0.9.0"))`.
- `ProjectTemplateIntegrationTest` (gated by `KUBRIKO_CREATOR_INTEGRATION`, runs the real init script): after the existing `launch` assertion, assert the parsed `libraries` contain `org.jetbrains.kotlin:kotlin-stdlib` and a module whose group starts with `org.jetbrains.compose.`. This is the only test of the script change, so don't skip it: run it once with `KUBRIKO_CREATOR_INTEGRATION=true ./gradlew :creator:test --tests "*ProjectTemplateIntegrationTest*"`.

## Manual check

Open a Scene in the Breakout scratch project (nothing newer: no warning, opens as before), then raise the project's `kotlinx-coroutines` in its catalog to a newer version than Creator's: the Scene opens and the console shows the warning once; raise `compose-multiplatform` by a minor version: the Scene shows the "can't draw" notice and Play still works.
