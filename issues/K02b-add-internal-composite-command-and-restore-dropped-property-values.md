<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Add an internal CompositeCommand that applies parts in order and rolls them back on failure, and make undoing add_property bring back the Scene values it dropped.

**Challenged:** amended — follows K02a's amended order (the `put` first, the Scene change after it with the `put` rolled back on a throw), instead of Scenes first.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** CMD-15, CMD-4  ·  **Depends on:** K02a
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/CompositeCommand.kt`, `commands/actorTypes/AddProperty.kt`, `commands/actorTypes/PropertySceneValues.kt`, `scenes/SceneStore.kt` (`changeStates` returns what it replaced), `commands/actorTypes/EditProperty.kt` and `commands/actorTypes/RenameActorType.kt` (only to follow `changeStates`' new return type), `creator/src/main/composeResources/values/strings.xml` (only if a title string is needed; prefer none), tests new `commands/CompositeCommandTest.kt`, `commands/actorTypes/PropertyCommandsTest.kt`, `commands/CommandRegistryTest.kt`, `creator/CLAUDE.md` (commands section, one sentence)

## Problem

CMD-15: `Applied.inverse` is one `Invocation<*>`, so every command needs a bespoke inverse that restores everything it changed, and one that can't loses data. CMD-4, `commands/actorTypes/AddProperty.kt`:

```kotlin
        context.requireBlockTypes.put(definition.withPropertyInserted(input.index ?: definition.properties.size, input.property))
        context.changeSceneStates(input.actorType) { it.withoutMismatchedValue(input.property.name, PropertyKind.of(input.property)) }
        return Applied(inverse = Invocation(RemoveProperty, RemoveProperty.Input(input.actorType, input.property.name)))
```

(K02a keeps this order and puts the definition back when the Scene change throws.) `RemoveProperty` deliberately keeps Scene values ("so that undoing this brings them back"), but `AddProperty` deletes values of another kind and its inverse can't bring them back. Sequence: text Property `health` with per-Actor values → remove it (values kept) → add a number `health` (text values deleted) → undo, undo → text `health` is back with every per-Actor value lost.

## Fix

The user decided: CompositeCommand now, internal only (decisions.md).

1. **`commands/CompositeCommand.kt`**: `internal object CompositeCommand : WriteCommand<CompositeCommand.Input>` with `data class Input(val parts: List<Invocation<*>>)` (`parts` not empty).
   - Not a tool: it is never registered in `CommandRegistry` and its input is never decoded. `inputSerializer` is a serializer whose `deserialize`/`serialize` throw `SerializationException("CompositeCommand isn't a tool.")` (a small private object implementing `KSerializer<Input>` with a `buildClassSerialDescriptor("CompositeCommand")`). `name = "composite"`, `description = ""`.
   - Its title is the first part's: add `fun title(input: I): StringResource = title` to `WriteCommand`, overridden here as `input.parts.first().title`, and make `Invocation.title` call it (the history, the change log, and the AI step all read `Invocation.title`; check `CommandExecutor.call`'s `command.title` stays right for registered commands). The object's own `title` is the first registered generic one that fits, e.g. reuse an existing `command_*` string; don't add a string.
   - `target(context, input)` = the first part's target. Add `fun targets(context, input): Set<CommandTarget> = setOfNotNull(target(context, input))` to `WriteCommand`, overridden here as the union of the parts' (K03 uses it).
   - `validate` = the first part's `validate` (later parts validate against the state the earlier ones leave, so they are checked during `apply`); `validateRestoring` likewise with each part's own `isRestoring`.
   - `apply`: for each part, `part.validate(context)` → on a rejection, roll back and return `Applied(inverse = null, failure = rejection)`; `part.apply(context)` → on `applied.failure` roll back and return it; on an exception roll back and rethrow (the executor turns it into a rejection, K02a). Collect each `applied.inverse`. Rollback = apply the collected inverses in reverse order with `isRestoring = true`, ignoring their results (a rollback step that fails or throws is added to the original exception with `addSuppressed`, or, for a rejection, ignored; the state is then as partial as before this plan, and the executor reports the first failure). `isRewritingFiles` = any part's. `result` = the last part's. Inverse = `Invocation(CompositeCommand, Input(collectedInverses.reversed()))`, or the single inverse when there is only one, or null when a part has none (then the whole can't be undone, like git commands).
   - `mergeKey` stays null, so composites never merge.
   - KDoc: "Changes made as one step: its parts in order, undone in reverse. Kubriko Creator's own building block for changes no single command makes; not a tool."
2. **`CommandRegistryTest.everyCommandClassIsRegistered`**: subtract `CompositeCommand::class.java` from the found classes, with a short reason in the test's name or a one-line comment ("internal, not a tool").
3. **`SceneStore.changeStates(typeId, change)`** returns, instead of the changed names, `Map<String, Map<String, String>>` (keeping `scenes/` free of command types) (Scene name → Actor ID → the state string it replaced), only for Actors that changed. `renameTypeId` keeps returning names (`.keys`). Update the callers: `changeSceneStates` returns the map, and its `onScenesChanged(changed.keys)`.
4. **`AddProperty.apply`**: `put` first, then `val replaced = context.changeSceneStates(...)` inside K02a's `try` that puts the definition back on a throw; inverse:
   ```kotlin
   val removal = Invocation(RemoveProperty, RemoveProperty.Input(input.actorType, input.property.name))
   val inverse = if (replaced.isEmpty()) removal else Invocation(CompositeCommand, CompositeCommand.Input(
       listOf(removal) + replaced.map { (scene, states) -> Invocation(SetActorsState, SetActorsState.Input(scene, states.map { (id, state) -> SetActorsState.ActorState(id, state) })) },
   ))
   ```
   Undo removes the Property (keeping values) and puts the dropped values back; redo is the composite's own inverse (adds it again, then sets the states the add left). The `RemoveProperty` KDoc's promise ("undoing this brings them back") now also holds after an add in between.
5. `creator/CLAUDE.md`: in the commands section, one sentence: "`CompositeCommand` (internal, not a tool) makes several invocations one undo step and rolls back the done ones when a later one fails; `add_property`'s undo uses it to bring back the Scene values the add dropped."

## Tests

- `CompositeCommandTest` (with `commandContext` and real commands, so no fakes diverge): two `SetAppearance`/`EditProperty` parts on one type → applied in order, the inverse reverts both (`assertRoundTrip` from `CommandTesting.kt`); a second part that validates only after the first (e.g. `AddProperty x` then `EditProperty x→y`) is accepted; a second part rejected at apply → the first is rolled back (type equals the original) and `Applied.failure` is the second's rejection; a part that throws → rolled back and rethrown; through `CommandExecutor.execute` a composite is one history entry titled by its first part, and it doesn't merge with a following mergeable change.
- `PropertyCommandsTest.undoingAnAddBringsBackTheValuesItDropped`: type `Ball` with text Property `health`, a Scene whose Actor has `"health": "full"`; `RemoveProperty health`, `AddProperty health:number` (the Scene loses the value), undo, undo → the Scene file's text equals the original; redo, redo → equals the text after the add.
- `assertRoundTrip(AddProperty …)` with a Scene holding a mismatched value passes (it failed before: the Scene text didn't come back).
