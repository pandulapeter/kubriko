<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Validate commands on the main thread from an in-memory Scene name index and warm Scene cache, and move the project-file reads of upgrade_project and set_project_settings into apply.

**Challenged:** amended — the index can lag a Scene made outside Creator by up to a watcher interval, so what writes (`changeActors`' rename and state changes, folder moves) walks afresh on IO instead of trusting it, `create_scene` and `rename_scene` check the name again on IO before writing (else a stale index lets them overwrite a Scene made in IntelliJ a second ago), and a refresh no longer drops a name Creator wrote while it walked.

**Kind:** performance  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** CMD-9 (with the `scenes.names()` call CMD-14's fix added to `validateContents`)  ·  **Depends on:** K02a (`SceneStore` write path)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/SceneStore.kt`, `commands/scenes/CreateScene.kt`, `commands/scenes/RenameScene.kt`, `commands/folders/FolderOperations.kt` (`moveFolder`'s Scene list), `workspace/WorkspaceController.kt` (`refreshResources`, `onProjectChanged`, the `CommandContext` call), `commands/CommandContext.kt`, `commands/Applied.kt` (only if K01b's `failed(Rejection)` overload isn't there), `commands/project/UpgradeProject.kt`, `commands/project/SetProjectSettings.kt`, tests `scenes/SceneStoreTest.kt`, `commands/project/*` tests (`grep -rl "UpgradeProject\|SetProjectSettings" creator/src/test`), `commands/CommandTesting.kt`, `creator/CLAUDE.md` (the `SceneStore` sentence of the Scenes section)

## Problem

`WriteCommand.validate` is synchronous and runs on the main thread (the UI's `WorkspaceController.dispatchIfValid`/`createActorType` call `commands.validate` directly; MCP calls run on the main dispatcher). It does file-system work:

- `commands/actorTypes/ActorTypeValidation.kt`: `scopeOf(definition) = BlockScope.of(definition, requireBlockTypes.types.value, scenes.names(), audioFiles())`, and `validateContents` likewise; `SceneStore.names()` stats every known Scene and walks the Scenes folder:
  ```kotlin
    fun names() = (sceneResources().map { it.file } + sceneDirectory.walkTopDown().filter { it.extension == EXTENSION }.toList())
        .filter { it.isFile }
  ```
  on every AddBlock, EditBlock, and AddEvent validation (typing into a Block's field validates each keystroke).
- `commands/scenes/SceneValidation.kt` `validateScene`: `scenes.exists(name)` and `scenes.isValid(name)` → `cached()`, which stats the file and, on a cache miss, reads and parses the whole Scene. `CreateScene`/`RenameScene` call `names()`.
- `commands/project/UpgradeProject.kt`: `validate` runs `ProjectUpgrader(context.project.rootDirectory).check()` (reads up to ~7 files).
- `commands/project/SetProjectSettings.kt`: `validate` runs `GameIdentity.read(context.project.rootDirectory)` (build.gradle.kts and gradle.properties).

## Fix

Keep `validate` synchronous (the UI shows its `StringResource` under text fields) and feed it from memory (option (b) of the verification; making every `validate` suspend would touch every command and the UI contract).

1. **Scene name index in `SceneStore`.** `private var knownNames: SortedSet<String>?` (null until built), confined like the rest of the store's callers: built and replaced only by `refresh()` on IO, updated by own `write`/`move`/`delete` (which run on IO inside commands). Make it a `@Volatile` reference to an immutable set swapped whole, so the main thread's reads never see a half-built set.
   - `fun refresh()` (blocking, call off the main thread): today's walk, plus re-reading into the cache every Scene whose `FileStamp` differs from its cache entry (so later main-thread `isValid`/`actors` calls are cache hits). KDoc says it blocks.
   - `names()` returns `knownNames` when built; before the first `refresh()` it falls back to today's walk (only possible in the first moments after opening, before the first resource scan; never skip it, or `create_scene` could overwrite an existing Scene).
   - `exists(name)` = `name in names()` while indexed (no stat).
   - `write` adds the name, `move` swaps the names, `delete` removes it, `createFolder`/`deleteFolder` don't change names. These and `refresh()` run on different IO threads and can overlap (the watcher's refresh during a command's write), so updates go through one `synchronized` block with a counter: `refresh()` walks outside it, then, inside it, swaps in the walked set plus the names own writes added and minus those they removed since the walk began (track them in two small sets cleared by each swap), so a Scene Creator just wrote never drops out of the index until the next refresh.
   - **What the index is for, and what it isn't.** It answers main-thread validation only. Code that runs on IO and acts on every Scene keeps today's walk, through a `scanNames()` that `names()` used before: `changeActors` (so `rename_actor_type` and the Property commands also change a Scene made outside Creator since the last refresh, instead of leaving it naming a type that no longer exists) and `moveFolder`'s Scene list in `commands/folders/FolderOperations.kt` (moved into its IO block). Read tools (`list_scenes`, the digest's counts) may use the index: a Scene a second old shows up on the next read.
   - **Names taken since the last refresh.** `CreateScene.apply` and `RenameScene.apply`, on IO before writing, check the name against `scanNames()` (case-insensitively, as `validate` does) and return `Applied.failed(Rejection(Res.string.error_scene_name_taken))` when it is taken; `validate` alone, from the index, could pass for a Scene created by hand a moment ago and the write would replace it.
2. **Who refreshes.** `WorkspaceController.refreshResources()` already runs `scanResources(...) to scenes.folders()` on IO; add `scenes.refresh()` there (it covers the initial scan, every command's `onScenesChanged`, and the watcher's `SCENES` changes, which call `refreshResources`). Lane K's K09a later moves this into the change router; keep the call in one place.
3. **What stays on the main thread.** `isValid`/`actors`/`problemOf` keep their stat (one `stat` per call is cheap) and read on a miss; after `refresh()` a miss only happens for a file changed in the last watcher interval, which is the residual. Say so in the `SceneStore` class KDoc: "The names are known from the last [refresh] and Creator's own writes, so validating a command reads nothing; a Scene changed elsewhere since is read again when next asked for."
4. **`upgrade_project`.** `CommandContext` gets `val projectUpgrade: () -> TemplateUpgrade = { TemplateUpgrade.UpToDate }` (KDoc: what the template files need, as last checked in the background), wired by `WorkspaceController` to `{ _projectUpgrade.value }`. `UpgradeProject.validate` uses it; `apply` checks again on IO (`withContext(ioDispatcher) { upgrader.check() }`) and returns `Applied.failed(Rejection(...))` with the same rejections when the fresh check disagrees, before calling `apply()`.
5. **`set_project_settings`.** `validate` keeps only the input-only check (`input == Input()` → `error_command_project_settings_empty`); `apply` (already on IO) reads the identity, returns `Applied.failed(Rejection(error_command_project_settings_unavailable))` when it's null, and `Applied.failed(Rejection(reason, arguments))` when `input.appliedTo(identity).validate()` rejects. The UI (`ui/settings/ProjectSettings.kt`) dispatches through the executor, so a rejection still reaches the console; `check_change` reports "would be accepted" for a value only apply can refuse, which is acceptable (say so in `SetProjectSettings`' KDoc).
6. **Test wiring.** `commandContext(...)` in `CommandTesting.kt` gets `projectUpgrade` passed through.

## Tests

- `SceneStoreTest.namesComeFromTheIndexAfterARefresh`: `refresh()`, then create a Scene file by hand (no refresh) → not in `names()`; `write("b", …)` through the store → in `names()`; `move("b", "c")` → `c` in, `b` out; `delete("c")` → out; `refresh()` → the hand-made one is in.
- `SceneStoreTest.changesReachScenesTheIndexDoesntKnowYet`: `refresh()`, a Scene holding type `Ball` created by hand, `renameTypeId("Ball", "Puck")` → the hand-made Scene names `Puck`.
- `SceneCommandsTest.createSceneDoesntOverwriteASceneMadeSinceTheRefresh`: `refresh()`, `level.json` created by hand, `execute(CreateScene level)` → `Rejected(error_scene_name_taken)`, the file's bytes unchanged.
- `SceneStoreTest.aRefreshKeepsANameWrittenWhileItWalked`: a `refresh()` whose walk is held (an internal walk hook) while `write("b", …)` runs → `"b" in names()` after the refresh.
- `SceneStoreTest.namesBeforeTheFirstRefreshWalkTheFolder`: a fresh store over a folder with a Scene → `names()` has it.
- `SceneStoreTest.refreshReadsChangedScenesIntoTheCache`: give the store an internal `readText: (File) -> String = File::readText` hook that counts reads; refresh, overwrite a Scene by hand with different content, refresh again → the count rose by one, and `actors(name)` afterwards returns the new Actors without another read.
- `UpgradeProject` command test: `projectUpgrade = { TemplateUpgrade.UpToDate }` → `validate` rejects without touching files (point `rootDirectory` at a folder with no template files); a stale `Needed` whose fresh check is `UpToDate` → `apply` returns `Applied.failed(error_command_upgrade_not_needed)`.
- `SetProjectSettings` test: a project without `gradle.properties` → `validate` accepts, `execute` returns `Rejected(error_command_project_settings_unavailable)`.
