<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Turn a command that throws into a rejection, write the Scenes a command changes all or nothing, and put the in-memory type change back when they can't be written.

**Challenged:** amended — the in-memory change stays first (Scenes-first delayed every Property-name and type-name keystroke behind an IO hop, so the field's next keystroke named a Property that no longer existed and was rejected); a Scene write that throws now rolls the in-memory change back instead. K02b and K04 follow.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all (worst on Windows, where the running game locks Scene files)
**Findings:** CMD-1  ·  **Depends on:** K01b (order only: both edit `Applied.kt`)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandExecutor.kt`, `scenes/SceneStore.kt`, `commands/actorTypes/RenameActorType.kt`, `commands/actorTypes/PropertySceneValues.kt`, `commands/actorTypes/AddProperty.kt`, `commands/actorTypes/EditProperty.kt`, `creator/src/main/composeResources/values/strings.xml`, tests `commands/CommandExecutorTest.kt`, `scenes/SceneStoreTest.kt`, `commands/actorTypes/ActorTypeCommandsTest.kt`, `commands/actorTypes/PropertyCommandsTest.kt`, `commands/CommandTesting.kt`, `creator/CLAUDE.md` (the commands section's `CommandExecutor` bullet)

## Problem

`commands/CommandExecutor.kt` has no exception handling around `apply`:

```kotlin
    private suspend fun execute(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): CommandOutcome = changes.withLock {
        validate(invocation)?.let { return@withLock CommandOutcome.Rejected(it) }
        val applied = invocation.apply(context)
        applied.failure?.let { return@withLock CommandOutcome.Rejected(it) }
```

and the same in `step()` (`val applied = invocation.apply(context)`). A throwing apply leaves what it already changed, records no history entry and no revision, and the exception escapes: through `dispatch` to the workspace scope's `unexpectedProblemHandler` ("Kubriko Creator ran into a problem"), through `call` to the MCP server's `error_mcp_internal`; an undo step that throws stays on the history and throws again on every try.

Multi-step commands make it dangerous. `commands/actorTypes/RenameActorType.kt`:

```kotlin
        context.requireBlockTypes.rename(input.actorType, input.newName)
        val changedScenes = withContext(context.ioDispatcher) { context.scenes.renameTypeId(input.actorType, input.newName) }
        if (changedScenes.isNotEmpty()) context.onScenesChanged(changedScenes)
```

`scenes/SceneStore.kt` writes Scene by Scene:

```kotlin
    private fun changeActors(change: (SceneActorEntry) -> SceneActorEntry) = names().filter { name ->
        val actors = actors(name) ?: return@filter false
        val changed = actors.map(change)
        if (changed == actors) return@filter false
        write(name, changed)
        true
    }
```

An `IOException` in Scene 3 of 5 (the game holds it open on Windows) leaves the type renamed, Scenes 1–2 renamed, 3–5 naming a type that no longer exists, no undo entry, and `onScenesChanged` never called for 1–2. `changeSceneStates` (AddProperty, EditProperty's rename) has the same shape, after the in-memory `put`.

## Fix

1. **Executor safety net.** In `execute` and `step`, wrap `invocation.apply(context)`:
   ```kotlin
   val applied = try {
       invocation.apply(context)
   } catch (exception: Exception) {
       if (exception is CancellationException) throw exception
       return@withLock CommandOutcome.Rejected(Rejection(Res.string.error_command_failed, exception.message ?: exception.toString()))
   }
   ```
   In `step`, also set `_history.value = remaining` before returning (a step that throws is dropped, as one that no longer validates). Catch `Exception`, not `Throwable`: an `Error` keeps escaping to the scope's handler. `call()` already finishes the AI step for anything thrown (CMD-2, landed). New string `error_command_failed`: "The change couldn't be finished: %1$s". The executor stays main-thread confined; nothing here changes threads.
2. **`SceneStore.writeAll(changes: Map<String, List<SceneActorEntry>>)`** (`@EditorMutation`, blocking): remember each Scene's current text (`cached(name)?.text`, read before writing anything), write them in order; on an `IOException` at Scene k, write the remembered text back to Scenes 0 until k−1 (each restore in its own try, a failed restore added with `addSuppressed`), then rethrow. Restored files are recorded in `ownWrites` and the cache like any write. `changeActors` computes every changed Scene first, then calls `writeAll`, and returns the names only after it succeeded. KDoc: "Writes every Scene in [changes], or, when one can't be written, puts back those written before it and throws."
3. **Test hook.** `SceneStore` gets an internal constructor parameter `writeText: (File, String) -> Unit = { file, text -> file.writeTextAtomically(text) }`, used by its private `write`; tests make it throw for one file. No other behaviour change.
4. **Types first, rolled back on failure.** Keep the in-memory change (`put`, `rename`) before the Scene writes, as today. `dispatch` starts commands `UNDISPATCHED` so that the change shows before it returns, and the Property name field (`ui/actorTypes/PropertyList.kt`, `onChange = { onChanged(property.name, it) }`) and the type name field build each keystroke's command from the name the previous one left; doing the Scene IO first would delay the `put` by an IO hop on every renaming keystroke (even with no Scene holding the type), so the next keystroke would name a Property or type that no longer exists and be rejected. Instead, when the Scene writes throw (all or nothing since point 2, so the Scenes are unchanged), put the in-memory state back before rethrowing:
   - `RenameActorType.apply`: `rename(old, new)`; `try { renameTypeId on IO } catch (exception: Exception) { requireBlockTypes.rename(new, old); throw exception }` (renaming back is exact: `validate` refused a taken new name, so only references the forward rename made point to it); `onScenesChanged` only after both.
   - `AddProperty.apply`: `put(withPropertyInserted)`; `changeSceneStates(...)` in a `try` whose `catch` does `put(definition)` (the one read at the top) and rethrows.
   - `EditProperty.apply`: the `put`s (the type, and for a Manager type the renamed users), remembering each `previous` definition; the rename's `changeSceneStates` in a `try` whose `catch` `put`s every remembered previous definition back and rethrows.
   Catch `Exception` and rethrow everything, `CancellationException` included (the rollback is plain in-memory work, safe in a cancelled coroutine). The rollback queues writes of the old definitions after the new ones, which `PendingWrite.withoutOverwrittenSaves` merges when they share a batch; if a batch was written in between, the next one restores the files. Net, a throw changes nothing, the executor turns it into a rejection, no history entry or `ChangeLog` revision is recorded (none is due), and K05's read lock keeps AI reads from seeing the in-between state.
5. `commands/actorTypes/PropertySceneValues.kt` `changeSceneStates` keeps its shape (it calls `onScenesChanged` after its writes, which now come after the `put`); it gets atomicity through `changeStates` → `changeActors` → `writeAll`.
6. Leave the other partial shapes as they are (CreateScene's description write after the Scene, folder renames, which already move back): the safety net reports them, and K02b's composite covers future multi-part changes.
7. `creator/CLAUDE.md` commands section: one sentence in the `CommandExecutor` bullet: "a command that throws is rejected with the reason (an undo step that throws is dropped), and commands that change several Scenes write them all or none (`SceneStore.writeAll`), putting their type change back when the Scenes can't be written".

## Tests

- `CommandExecutorTest.aThrowingCommandIsRejected`: a test-only `WriteCommand` (pass a custom `registry` or use `execute(Invocation(...))` directly) whose `apply` throws `IOException("Locked.")` → `CommandOutcome.Rejected` with `error_command_failed` and argument `Locked.`, `history.undoable` unchanged, a second, working command right after succeeds (the lock was released).
- `CommandExecutorTest.aThrowingUndoStepIsDropped`: an entry whose revert throws → `step(isUndo = true)` returns `Rejected`, the entry is gone, `step` again returns the next one.
- `CommandExecutorTest.cancellationIsNotARejection`: apply that throws `CancellationException` → it propagates.
- `SceneStoreTest.writeAllPutsBackWhatItWroteWhenOneSceneFails` (real temp folder): Scenes `a`, `b`, `c` on disk; `writeText` hook throws for `b.json` → `assertFailsWith<IOException>`, `a.json` and `c.json` have their original bytes, `actors("a")` returns the original Actors.
- `ActorTypeCommandsTest.aRenameShowsBeforeItsScenesAreWritten`: with an `ioDispatcher` that is a `StandardTestDispatcher`, `executor.dispatch(RenameActorType Ball→Puck)` → `blockTypes["Puck"] != null` before the dispatcher runs (the field's next keystroke sees it); the same for `EditProperty`'s rename.
- `ActorTypeCommandsTest.aRenameWhoseSceneCantBeWrittenChangesNothing`: type `Ball` used in two Scenes, the hook failing for the second → `execute(RenameActorType Ball→Puck)` is `Rejected(error_command_failed)`, `blockTypes["Ball"] != null`, `blockTypes["Puck"] == null`, both Scene files unchanged, no history entry.
- `PropertyCommandsTest.anAddedPropertyWhoseSceneCantBeWrittenIsNotAdded`: same with AddProperty dropping a mismatched value; and `anEditedPropertyWhoseSceneCantBeWrittenIsNotRenamed` for a Manager type's Property used by another type (both definitions come back).
- `commandContext(...)` in `CommandTesting.kt` gets an optional `sceneWriteText` parameter passed to `SceneStore`.
