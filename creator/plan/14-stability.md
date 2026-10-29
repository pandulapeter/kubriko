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
the code, why it matters, and the direction; none of it is built yet. The first two lose or lock up a user's work.

## Broken and hand-edited files

- **One unreadable type file locks the workspace.** `BlockTypeStore.load` (`actors/BlockTypeStore.kt`) decodes every file in
  `.kubriko/actors` and `.kubriko/managers` with nothing catching a `SerializationException` or `IllegalArgumentException`. In
  `BlockTypesController`'s `init` that coroutine dies before its write loop starts, while the `Regenerate` queued just before keeps
  `pendingWriteCount` at 1, so `awaitWrites` never returns: every build, play, Scene opening, git command, and `close()` waits
  forever, and `onCloseRequest` (`KubrikoCreatorApp.kt`) waits for `close()`, so the window can't be closed. `reloadFromDisk` throws
  the same way when the watcher sees such a file. A merge conflict, a crash mid-write, or one typo in a hand edit is enough, and
  nothing says which file. Direction: load each file on its own, report the ones that can't be read in the console by path and
  keep them out of the types; while any can't be read, regenerate nothing that depends on them (generating from a partial list
  would drop the type from `GeneratedActorTypes` and break the build) and never write a definition over the unreadable file. Also
  give `onCloseRequest` a timeout, and catch failures of the `init` coroutine so `pendingWriteCount` can't stay raised.
- **Writes aren't crash-safe.** `BlockTypeStore` (definitions and generated code), `SceneStore.write`, and the settings stores
  (`McpSettingsStore`, `AssistantSettingsStore`, `RecentProjectsStore`), and `KubrikoBuildFiles` and `ProjectUpgrader` on the user's build files, use `File.writeText`, which empties the file first:
  a crash, a kill, or a full disk mid-write leaves a truncated type (which then hits the item above), a Scene, or a generated file
  that fails its checksum and counts as changed by hand forever, and resets the MCP token. `McpConfigFile.write` already replaces
  files atomically. Direction: one helper (write a sibling temporary file, then `Files.move` with `ATOMIC_MOVE`, falling back to a
  plain replace where the file system can't), used by every writer, with `OwnWrites` recording the final file.
- **Names in type files are trusted.** A type's name comes from its JSON, not its file name, and isn't validated on load. `Foo.json`
  holding `"name": "Bar"` is saved as `Bar.json` beside it (two `Bar`s on the next load); `"name": "my enemy"` generates
  `class my enemy`; and two files of one name in different folders (a copy, a merge) both load, the registry lists the type twice,
  and saving either one deletes the other (`BlockTypeStore.save`, which removes same-named files in other folders). Direction: on
  load, take the name from the file name (or report a mismatch), report names `BlockTypeNaming` refuses, and report duplicates
  without loading either rather than deleting one.
- **Generated files of types deleted outside Creator stay.** Only `delete(name)` removes a generated file; a type removed by a git
  checkout, IntelliJ, or the file manager leaves its `generated/<Name>.kt`, which still names other types and Managers and breaks the
  build once they change. Direction: after regenerating, delete generated files that are unchanged (`GeneratedFile.isUnchanged`), not
  a current type, and not one of the fixed generated files; report those changed by hand instead.
- **Format versions.** `.kubriko/*.json` and Scene files carry no format version; compatibility lives in scattered lenient
  serializers (`TimerNameSerializer`, old key names). A file written by a newer Creator fails to load or loses fields when saved by
  an older one. Direction: a `formatVersion` in `.kubriko/project.json` (one number for the project's Creator files, raised only by
  changes an older Creator can't read), migrations applied on load, and a notice instead of opening a project whose version is
  newer, so nothing is overwritten. Decide before the first public release, since every project made before it is unversioned.

## Undo history

- **Undoing can fail on data that is valid to keep.** Blocks and Events keep references to deleted types, Scenes, and sounds (they
  generate comments), but inverses go through the creation-time checks: `CreateActorType`, `AddEvent`, and `AddBlock` reject them
  (`commands/actorTypes/ActorTypeValidation.kt`), so deleting `Bat`, then `Player` (whose collision Event named `Bat`), then
  undoing drops the step (`CommandExecutor.step`) and `Player` is gone. `DeleteScene`'s inverse `CreateScene` rejects a Scene file
  that isn't valid, and `RestoreActors` an unparsable state. Direction: inverses restore exactly what was there, checking only
  structure (a restore flag on the inputs that skips reference and content checks, set only by `apply`'s inverses).
- **Merged property edits undo wrongly.** `EditProperty.mergeKey` is `actorType to name` (`commands/actorTypes/EditProperty.kt`), and
  a merge keeps the first step's inverse. Renaming `hp` to `health` by typing keeps an inverse that names `hh`, which no longer
  exists, so the whole rename can't be undone; editing `speed` right after `health` merges both and undo reverts only one.
  Direction: let a `WriteCommand` combine two inverses when steps merge (address the latest name, restore the first one's value),
  and include a stable Property identity in the key.
- **Changes outside the history leave it stale.** `discard_changes` and `upgrade_project` rewrite files and return no inverse, but
  the history keeps steps recorded against the old files, which may still validate and then change the wrong thing; and the types in
  memory are only reloaded by the watcher a second later, so a command right after a discard writes the old definition back.
  Direction: after such a command, clear the history (`CommandExecutor.clearHistory`) and await a reload of the types before
  returning.

## Processes, threads, and lifecycles

- **No handler for uncaught exceptions.** The app's and each workspace's scopes have no `CoroutineExceptionHandler`, so anything a
  launch doesn't catch (the item above, an unexpected `RuntimeException` from JGit, Gradle, or a project class) goes to the default
  handler and is lost or ends the app. Direction: a handler on both scopes that logs and reports it in the console ("Kubriko
  Creator ran into a problem: …"), so the failure is visible and the rest keeps working.
- **The project's code runs during composition.** `rememberSceneView` (`ui/scenes/RememberSceneView.kt`) constructs `SceneView`,
  which calls the project's `createSerializationManager` and `createCustomManagers`, inside `remember`: a hand-written Manager whose
  constructor throws, or a stale class (`NoClassDefFoundError`), throws out of composition on the UI thread, every time the Scene
  opens. Direction: create the `SceneView` off the main thread in `SceneEditingController` (it already loads classes there) and turn
  a failure into `SceneEditingState.Failed` with its own message.
- **Class loaders leak.** A load cancelled while `ClassLoaderEntryPointLoader.load` runs (a change, Reload, or closing during it)
  throws its result away without closing it, and `SceneEditingController.dispose` closes the loaded classes five seconds later in a
  scope `WorkspaceController.close` cancels first, so the `URLClassLoader` and its copied jars in the temporary folder stay for the
  process (and `deleteOnExit` can't remove a folder that isn't empty). Over a long session of project switches this adds up.
  Direction: run the load under `NonCancellable` and close the result if the caller was cancelled, and close released classes on a
  scope that outlives the workspace (or right away on `dispose`).
- **Out-of-order results.** `SceneEditingController.onScenesChanged` starts an untracked read per call, and
  `WorkspaceController.refreshResources` an untracked full scan; when two overlap (an AI batch, repeated undo), the older result can
  land last and show a removed Actor or Scene again until the next change. Direction: one conflated worker each, or ignore results
  older than the latest request.
- **Game launches on Windows** put the whole classpath on the command line (`session/GameLaunch.kt`), which Windows caps at 32,767
  characters; a long user folder with the usual jars gets close. Direction: pass it through a Java `@argfile`, as Hot Reload's
  launch already does.
- **Recent projects** (`RecentProjectsStore`) read, change, and write the file without a lock from several IO threads (the
  command-line project races the welcome screen's load), and `load` forgets projects whose folder is missing, so one on an
  unmounted drive is lost for good. Direction: synchronize the store, write atomically (above), and show missing projects greyed
  with Remove instead of forgetting them.

## Generated code

- **Manager lookup fields can clash.** `BlockScope.managerField` (`actors/BlockScope.kt`) names the field an Actor looks a Manager up
  in after the Manager (`score` for `Score`), falling back to `scoreManager` only for the type's own Properties. A Manager type
  named `Velocity`, `Body`, `IsRemoved`, `Sprite`, `Other`, or `Index` clashes with generated members and locals, and `Scope` in a
  Manager hides `Manager.scope`; adding a Property `score` later silently renames the field, breaking Kotlin Blocks that say
  `score.value`. Direction: also fall back for every name `PropertyNaming` reserves, and decide whether the field name should be
  fixed per Manager (always `scoreManager`) despite the template upgrade that would take, since Kotlin Blocks name these fields.

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

## Open questions

1. Whether this file goes before `02-ai-assistant.md`'s remaining steps: its first two items can lock a project or lose work for
   anyone who edits files outside Creator or merges branches, which bigger projects do daily.
2. Whether a newer project format should open read-only rather than not at all.
