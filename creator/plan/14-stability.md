<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Stability

What breaks, hangs, or loses work today when files are broken, a process misbehaves, or something fails halfway. Each item names
the code, why it matters, and the direction; none of it is built yet. The user put stability before new features, so this file
comes right after `01-large-projects.md`, before the rest of `02-ai-assistant.md`.

## Processes, threads, and lifecycles

- **Out-of-order results.** `SceneEditingController.onScenesChanged` starts an untracked read per call, and
  `WorkspaceController.refreshResources` an untracked full scan; when two overlap (an AI batch, repeated undo), the older result can
  land last and show a removed Actor or Scene again until the next change. Direction: one conflated worker each, or ignore results
  older than the latest request.
- **Game launches on Windows** put the whole classpath on the command line (`session/GameLaunch.kt`), which Windows caps at 32,767
  characters; a long user folder with the usual jars gets close. Direction: pass it through a Java `@argfile`, as Hot Reload's
  launch already does.
- **Recent projects** (`RecentProjectsStore`) read, change, and write the file without a lock from several IO threads (the
  command-line project races the welcome screen's load), and `load` forgets projects whose folder is missing, so one on an
  unmounted drive is lost for good. Direction: synchronize the store and show missing projects greyed
  with Remove instead of forgetting them.

## Generated code

- **Manager lookup fields can clash.** `BlockScope.managerField` (`actors/BlockScope.kt`) names the field an Actor looks a Manager up
  in after the Manager (`score` for `Score`), falling back to `scoreManager` only for the type's own Properties. A Manager type
  named `Velocity`, `Body`, `IsRemoved`, `Sprite`, `Other`, or `Index` clashes with generated members and locals, and `Scope` in a
  Manager hides `Manager.scope`; adding a Property `score` later silently renames the field, breaking Kotlin Blocks that say
  `score.value`. Direction: always name the field `<name>Manager` (`scoreManager`), whatever the type's Properties, and reserve
  Property names ending in `Manager` (`PropertyNaming`) so nothing can clash with it. Kotlin Blocks and Kotlin UI name these fields,
  so the change is a format migration (a `FormatMigration`, the first to raise the format version) that renames them through `KotlinIdentifiers`, as Manager renames do.

## Scene editor

- **A broken Scene looks empty.** A Scene file that doesn't parse opens with no Actors (`WorkspaceController`'s `readScene` uses
  `orEmpty()`), and an Actor that can't be created from its state (`SceneView.createActor` returns null) is invisible and unselectable,
  its inspector showing a bare header. Commands on it are rejected only in the console. Direction: an invalid-Scene notice in
  `SceneEditingState.Open` (with the parse error and Open in the system editor), and Actors that couldn't be created marked in the
  Actor list and inspector, with Remove.
- **Dragging an Actor without a saved position** (a hand-written type relying on its default, or any Actor of an invalid Scene) moves
  it on screen, but `move_actor` is rejected and nothing reloads, so the screen and the file disagree until the Scene is opened
  again (`ui/scenes/SceneCanvas.kt`). Direction: treat the press as a pan unless the Actor's state has a position.
- **Renaming a Scene ignores a failed move.** `SceneStore.move` doesn't check `renameTo` (Windows refuses while the game holds the file),
  yet `renameScene` still renames the goToScene Blocks and follows the open Scene to a file that doesn't exist. Direction: `Files.move`,
  and `rename_scene` failing (`Applied.failed`) before anything else changes; the folder rename stops at the first failure.
