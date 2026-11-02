<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Decide what the project's own build output is in one place: a classpath entry canonically under the project's root

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (worst with `GRADLE_USER_HOME` moved, common on Windows and CI)
**Findings:** INF-6 (part b; part a landed in review batch F2)  ·  **Depends on:** none
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/project/ProjectOutput.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/ClassLoaderEntryPointLoader.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/session/GameProcess.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/GameBuilds.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt` (the `entryPointLoader` default), tests new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/project/ProjectOutputTest.kt`, `scenes/ClassLoaderEntryPointLoaderTest.kt`, `session/GameProcessTest.kt`, `workspace/WorkspaceControllerTest.kt` (its `entryPointLoader` default), `benchmark/CreatorBuildBenchmarkTest.kt` (calls `ClassLoaderEntryPointLoader.load`); `creator/CLAUDE.md` only if it names `isProjectOutput` (grep)

## Problem

Three places decide which classpath entries are the project's own output (copied before use, because the next build replaces them), with three different rules:

- `scenes/ClassLoaderEntryPointLoader.kt`, by cache path:
  ```kotlin
  internal fun isProjectOutput(file: File) = (file.isDirectory || (file.isFile && file.extension == "jar")) && file.invariantSeparatorsPath.let { path ->
      "/.gradle/caches/" !in path && "/.m2/repository/" !in path
  }
  ```
- `session/GameProcess.kt` `snapshotOf`: `!entry.exists() || !entry.canonicalFile.startsWith(projectDirectory) -> listOf(entry)` (canonically under the project folder).
- `workspace/GameBuilds.kt`: `private fun ProjectClasspath?.ownClassFolders() = this?.files.orEmpty().filter { it.isDirectory && it.canonicalFile.startsWith(rootDirectory.canonicalFile) }`.

With `GRADLE_USER_HOME` somewhere else (`/opt/gradle`, `D:\gradle`, a CI cache folder), the first rule takes every dependency jar for project output, so each Scene load copies the whole dependency set (hundreds of megabytes for a Compose game) into a temporary folder, and the loader and the game snapshot disagree about what is the project's. The decision (`decisions.md`: "Project output = under the project root") is one rule for all three.

## Fix

1. New `project/ProjectOutput.kt`:

   ```kotlin
   /**
    * What a project's own build makes, as opposed to the libraries it downloads: anything under the project's folder.
    * An included build outside that folder counts as a library, so its outputs are used where they are.
    */
   internal object ProjectOutput {
       fun isOwn(file: File, rootDirectory: File) = file.canonicalFile.startsWith(rootDirectory.canonicalFile)
   }
   ```

   `File.startsWith(File)` compares path components, so `/game2` is not under `/game`; `canonicalFile` resolves symlinks (macOS `/var` → `/private/var`) and, on Windows, the real letter case, so both sides compare the same way. `canonicalFile` touches the disk: every caller already runs on an IO dispatcher (`ClassLoaderEntryPointLoader.load` through `createDisposingIfCancelled(ioDispatcher, …)`, `GameProcess.run` in `withContext(Dispatchers.IO)`, `GameBuilds` inside `withContext(ioDispatcher)`); keep it that way. Compute `rootDirectory.canonicalFile` once per call site where it is called in a loop (an overload `isOwn(file, canonicalRoot)` taking an already canonical root, or a local `val root = rootDirectory.canonicalFile` and a private helper; pick one and use it in all three).

2. `ClassLoaderEntryPointLoader` needs the root, so it becomes a class:

   ```kotlin
   internal class ClassLoaderEntryPointLoader(private val rootDirectory: File) : EntryPointLoader
   ```

   `isProjectOutput(file)` becomes `private fun isProjectOutput(file: File) = (file.isDirectory || (file.isFile && file.extension == "jar")) && ProjectOutput.isOwn(file, rootDirectory)`; `classLoaderOf` stays an `internal` member (tests call it). Update the class KDoc's "The project's own jars are copied…" to say "the jars and folders under the project's root". `WorkspaceController`'s parameter becomes `entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader(project.rootDirectory)` (`project` is the first constructor parameter, so the default may use it); same in `WorkspaceControllerTest`'s helper (`ClassLoaderEntryPointLoader(project.rootDirectory)` or the temp root it uses) and `CreatorBuildBenchmarkTest` (the benchmark's project folder).

3. `GameProcess.snapshotOf`: `!entry.exists() || !ProjectOutput.isOwn(entry, projectDirectory) -> listOf(entry)`; the `projectDirectory` field stays (already canonical).

4. `GameBuilds.ownClassFolders`: `filter { it.isDirectory && ProjectOutput.isOwn(it, rootDirectory) }`.

Behavior change, accepted by the decision: an `includeBuild` whose folder is outside the project root is now treated like a library (not copied); on Windows its jars stay open while a Scene shows, so rebuilding that included build while Creator shows a Scene can fail to overwrite them. Note it in the `ProjectOutput` KDoc (done above) and nowhere else. No string or UI change.

## Tests

- `ProjectOutputTest` (real temp folders):
  - `aFileUnderTheRootIsOwn` (`root/game/build/libs/game.jar`), `aFileOutsideTheRootIsNot` (a sibling temp folder), `aSiblingWithTheRootAsPrefixIsNot` (`root` vs `root2/x.jar`), `aPathThroughDotDotIsResolved` (`File(root, "game/../../outside/x.jar")` → false), `aSymlinkedRootStillOwnsItsFiles` (create a symlink to `root` with `Files.createSymbolicLink`, `Assume.assumeTrue` it could be created; a file through the link is own).
- `ClassLoaderEntryPointLoaderTest`: replace `onlyJarsAndFoldersTheProjectBuildsAreCopied` with `onlyJarsUnderTheProjectRootAreCopied`: a loader for `root`, `classLoaderOf(listOf(jarUnderRoot, jarOutsideRoot), copies, …)` with both jars real (zip files written with `ZipOutputStream`, or an empty valid jar via `JarOutputStream`); assert `copies` holds exactly one file named `0-…` after the jar under the root, and a jar under `outside/.gradle-home/caches/…` (the moved-`GRADLE_USER_HOME` case) is not copied. Without the fix, the old rule copies the outside jar. Make the existing `load(...)` helper use `ClassLoaderEntryPointLoader(root)`.
- `GameProcessTest`: if it doesn't cover it already, `aClasspathEntryOutsideTheProjectIsntCopied` for `snapshotOf` (a folder outside `projectDirectory` is passed through as is, one under it gets a copy before it).

## Manual check

On Windows (or with `GRADLE_USER_HOME` set to a folder outside the project), open a Scene of the Breakout scratch project and check that the temporary `kubriko-creator-classes*` folder holds only the project's own jars (a few, not the whole dependency set), and that rebuilding after a change still swaps the classes.
