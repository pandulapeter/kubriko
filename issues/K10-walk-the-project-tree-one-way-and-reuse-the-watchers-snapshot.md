<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Walk a project's files through one ProjectTree.walk that enters source folders whatever their names, and build the resource tree after a watcher change from the snapshot the watcher just took.

**Challenged:** amended — rebased on 48b22953b (R9): the four walks already share `ProjectTree.isIgnored`, and `out`, `Pods`, `DerivedData`, `.fleet`, `.vscode`, and `.cxx` left `ignoredDirectoryNames` for the private tool list, so the walk keeps `isIgnored` (the plan's `it.name !in ignoredDirectoryNames` would have entered a root `out/` or `Pods/`); what remains is entering `build` and the other always-ignored names inside `src`, one `walk` function, the root exception for `SceneRenaming`, and the snapshot reuse.

**Kind:** performance  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** INF-10 = ARCH-4 (part b; part a, the shared ignore list and the reserved Scene folders, landed in 6fdaa00f6)  ·  **Depends on:** K09a (the router owns the refresh and the watcher's callback)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/project/ProjectTree.kt`, `project/ProjectWatcher.kt`, `project/ResourceScanner.kt`, `project/ExpectDeclarations.kt`, `commands/scenes/SceneRenaming.kt`, `workspace/ProjectChanges.kt` (from K09a), `workspace/WorkspaceController.kt` (the `startWatching`/`scanResources` wiring), `workspace/GradleWorkspaceOpener.kt`, tests new `project/ProjectTreeTest.kt`, `project/ProjectWatcherTest.kt`, `project/ResourceScannerTest.kt`, `project/ExpectDeclarationsTest.kt`, `creator/CLAUDE.md` (the `project` row: the watcher's areas)

## Problem

Since 48b22953b the four walks decide through `ProjectTree.isIgnored(directory, rootDirectory)`, which skips `ignoredDirectoryNames` (`build`, `.gradle`, `.git`, `.idea`, `.kotlin`, `kotlin-js-store`, `node_modules`) anywhere and the IDE and native build folders (`out`, `.fleet`, `.vscode`, `.cxx`, `Pods`, `DerivedData`) only outside a `src` folder, but each still builds its own `walkTopDown().onEnter { … }`:

```kotlin
// project/ProjectWatcher.kt snapshot
                .onEnter { it == rootDirectory || (!ProjectTree.isIgnored(it, rootDirectory) && it.relativeTo(rootDirectory).invariantSeparatorsPath != hiddenDirectory) }
// project/ResourceScanner.kt scan
        .onEnter { it == rootDirectory || (it.name != CREATOR_DIRECTORY && !ProjectTree.isIgnored(it, rootDirectory) && it.relativeTo(rootDirectory).invariantSeparatorsPath != hiddenDirectory) }
// project/ExpectDeclarations.kt any
            .onEnter { it == rootDirectory || it.isWithinSources() || (it.name != CREATOR_DIRECTORY && !ProjectTree.isIgnored(it, rootDirectory)) }
// commands/scenes/SceneRenaming.kt
    .onEnter { it.name !in creatorDirectories && !ProjectTree.isIgnored(it, rootDirectory) }      // creatorDirectories = .kubriko, generated; no root exception
```

- A Kotlin package named `build` (`src/commonMain/kotlin/com/game/build/Level.kt`) is still neither shown in the sidebar nor noticed when edited in IntelliJ by the watcher and the scanner, since `build` is skipped at any depth; `ExpectDeclarations` enters anything under `src`; the others don't. `SceneRenaming` has no root exception, so a project folder that is itself called `build` is skipped whole.
- After every watcher change in Scenes or sources, `refreshResources()` walks the tree again, a second or less after the watcher's own walk found the same files.

## Fix

1. `ProjectTree.walk(root: File, skipped: Set<String> = emptySet(), hiddenDirectory: String? = null): Sequence<File>`: `root.walkTopDown().onEnter { it == root || (it.name !in skipped && relativePath(it) != hiddenDirectory && (isWithinSources(root, it) || !isIgnored(it, root))) }`, with `isWithinSources(root, directory)` = `"src"` is one of the directory's ancestor segments below `root` (the test `isIgnored` already makes with `.split('/').dropLast(1)`; move `ExpectDeclarations.isWithinSources` here and make both use one function). Keep `isIgnored` and its two lists from 48b22953b (it becomes private or is used only by `walk`); outside `src` every listed folder stays skipped exactly as now. KDoc: build, tool, and version control folders are skipped outside source folders only, so source packages may have any name.
2. The four walkers use it: `ProjectWatcher.snapshot` with no extras (it must see `.kubriko`); `ResourceScanner.scan` and `ExpectDeclarations.any` with `skipped = setOf(".kubriko")`; `SceneRenaming` with `setOf(".kubriko", "generated")`. Keep `SceneNaming.reservedFolderOf` (landed): Scene folders named like output folders stay refused, now only for clarity.
3. **Reuse the snapshot.** `ProjectWatcher.start(onChange: (changes: Set<ProjectChange>, snapshot: ProjectSnapshot) -> Unit)` where `ProjectSnapshot` wraps the `Map<String, FileStamp>` it already builds (paths relative to the root, every file with an area, which includes every file `ResourceScanner.classify` can accept: Kotlin under `src` and `composeResources` files, both in `SOURCES`/`SCENES`). `ResourceScanner.scan(root, snapshot: ProjectSnapshot, hiddenDirectory)` builds the groups from those paths without touching the disk (`File(root, path)` for each). The router (K09a) runs the scan from the snapshot for watcher-triggered refreshes and keeps the walking scan for refreshes after Creator's own writes (which need files the last snapshot may not have yet). Check that `snapshot()` excludes exactly what `scan` excluded (the hidden generated directory: both; `.kubriko`: the snapshot has `.kubriko/actors/…` etc., which `classify` returns null for, so they drop out).
4. The resource refresh after a watcher change thus costs no walk; the watcher's 1 s walk is the only periodic one.
5. `creator/CLAUDE.md` `project` row: "excluding generated code" → add "output folders outside `src` (any name inside it)", and that the resource tree after an outside change comes from the watcher's snapshot. In the definition-of-done list's shared building blocks, "`ProjectTree.isIgnored` for every walk through a project" (48b22953b) becomes "`ProjectTree.walk`".

## Tests

- `ProjectTreeTest` (real temp folder): `build/`, `out/`, `node_modules/`, `Pods/`, `DerivedData/` at the root and in a module are skipped; `src/commonMain/kotlin/com/game/build/Level.kt` and `src/commonMain/composeResources/files/scenes/out/level.json` are found; `skipped` and `hiddenDirectory` are honoured; a project root named `build` is walked.
- `ResourceScannerTest.aPackageNamedBuildIsShown`; `ResourceScannerTest.aScanFromASnapshotEqualsAWalk`: the same project scanned both ways gives equal groups.
- `ProjectWatcherTest.aChangeInAPackageNamedBuildIsSeen` (virtual time, real files; 48b22953b already covers `out`).
- `ExpectDeclarationsTest` stays green (its behaviour already entered `src`).
- `ProjectChangesTest` (K09a): a watcher change passes its snapshot to the scan and no walking scan runs.
