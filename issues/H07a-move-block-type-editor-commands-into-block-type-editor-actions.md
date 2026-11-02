<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Move the type editor's 25 inline `Invocation`s into a tested `BlockTypeEditorActions`, and the screen text default into `ScreenTextDefinition.defaultFor`

**Challenged:** amended — says to keep 4ab2bdf08's Property row keys and `remember(name)`s, and that `actions` is only read through `rememberUpdatedState` in long-lived callbacks; K09 note.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-9  ·  **Depends on:** none (H07b builds on it). **K09:** nothing to re-locate (type editor UI only)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/actorTypes/BlockTypeEditorActions.kt`, `.../ui/actorTypes/BlockTypeEditor.kt`, `.../actors/ScreenTextDefinition.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/actorTypes/BlockTypeEditorActionsTest.kt` (new), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/ScreenTextDefinitionTest.kt` (new, or the existing actors test file it fits)

## Problem

`ui/actorTypes/BlockTypeEditor.kt` `BlockTypeEditor` (lines 85–259 at HEAD, about 175 lines) builds every command inline in its
callbacks, e.g.:
```kotlin
onDelete = {
    when (definition) {
        is ActorTypeDefinition -> onCommand(Invocation(DeleteActorType, DeleteActorType.Input(name)))
        is ManagerTypeDefinition -> onCommand(Invocation(DeleteManagerType, DeleteManagerType.Input(name)))
    }
},
…
onAdded = { onCommand(Invocation(AddText, AddText.Input(name, ScreenTextDefinition(text = "{${definition.properties.firstOrNull()?.name ?: "score"}}")))) },
…
onKeyChange = { key -> eventIndex?.let { onCommand(Invocation(SetEventKey, SetEventKey.Input(name, it, key))) } },
onBlockChange = { blockIndex, block -> eventIndex?.let { onCommand(Invocation(EditBlock, EditBlock.Input(name, it, blockIndex, block))) } },
```
About 25 of them (delete, parent, description, discard changes made by hand, appearance, add/edit/remove Property, add/edit/remove
text, Kotlin UI, add/override/move/remove Event, Event key/timer/other Actor type, rename timer, add/move/edit/remove Block). None
is tested: a wrong index or type name would only show in the running app. The `"score"` default for a new screen text is a
magic string in UI code.

## Fix

1. `actors/ScreenTextDefinition.kt`, companion:
   ```kotlin
   /** A new text showing the Manager's first Property ([properties]), or `{score}` when it has none yet. */
   fun defaultFor(properties: List<PropertyDefinition>) = ScreenTextDefinition(text = "{${properties.firstOrNull()?.name ?: DEFAULT_PROPERTY_NAME}}")
   private const val DEFAULT_PROPERTY_NAME = "score"
   ```
   (Use the actual Property type of `ManagerTypeDefinition.properties`.)
2. `ui/actorTypes/BlockTypeEditorActions.kt`:
   ```kotlin
   /** The commands the type editor of [definition] dispatches through [onCommand], one function per thing it changes. */
   internal class BlockTypeEditorActions(private val definition: BlockTypeDefinition, private val onCommand: (Invocation<*>) -> Unit) {
       private val name get() = definition.name
       fun delete()
       fun setParent(parent: String?)
       fun setDescription(description: String)
       fun discardChangesMadeByHand()
       fun setAppearance(input: SetAppearance.Input)          // whatever type `AppearanceColumn.onChange` gives today
       fun addProperty(property: …); fun editProperty(propertyName: String, property: …); fun removeProperty(propertyName: String)
       fun addText(); fun editText(index: Int, text: ScreenTextDefinition); fun removeText(index: Int)
       fun setKotlinUi(code: String)
       fun addEvent(type: EventType); fun overrideEvent(event: EventDefinition); fun moveEvent(index: Int, targetIndex: Int); fun removeEvent(index: Int)
       fun setEventKey(eventIndex: Int, key: …); fun setEventTimer(eventIndex: Int, timer: …); fun setEventOtherActorType(eventIndex: Int, other: …)
       fun renameTimer(oldName: String, newName: String)
       fun addBlock(eventIndex: Int, kind: BlockKind, gap: Int? = null); fun moveBlock(eventIndex: Int, index: Int, gap: Int)
       fun editBlock(eventIndex: Int, blockIndex: Int, block: BlockDefinition); fun removeBlock(eventIndex: Int, blockIndex: Int)
   }
   ```
   Bodies are exactly today's `Invocation(...)`s (copy them; parameter types from the commands' `Input`s). `addText` uses `ScreenTextDefinition.defaultFor(definition.properties)` (only for a `ManagerTypeDefinition`; take the properties from `definition.properties`, which `BlockTypeDefinition` has). `moveBlock` keeps `moveTarget(index, gap)?.let { … }` (no command when nothing moves). `overrideEvent` keeps `listOf(BlockDefinition.RunParentEvent)`.
3. `BlockTypeEditor`: `val actions = remember(definition, onCommand) { BlockTypeEditorActions(definition, onCommand) }` — but the drag state's `onDrop` lives across definitions (`remember(name)` with `rememberUpdatedState`), so read `actions` through `rememberUpdatedState` there as it reads `currentOnCommand` today. Every callback becomes a call (`onDelete = actions::delete`, `onKeyChange = { key -> eventIndex?.let { actions.setEventKey(it, key) } }`, …). Selection bookkeeping (`requestedEventIndex = …`) stays in the editor for now; H07b replaces it. Keep 4ab2bdf08's `propertyItemKeys`/`propertyKeys` (they key the Property rows, not commands) and its `remember(name)`s where they are; the editor still sits inside the caller's `key(selectedBlockType.name)`. `remember(definition, onCommand)` builds a new `BlockTypeEditorActions` on most recompositions (the caller passes a new lambda); that is one small object and fine, but nothing may keep an `actions` across compositions except through `rememberUpdatedState`. No behavior change; `BlockTypeEditor` drops below 120 lines, and its many command imports move to the new file.

No CLAUDE.md or strings change (the `BlockTypeEditor` description in the `ui` package row stays true; optionally name `BlockTypeEditorActions` there).

## Tests

- `ScreenTextDefinitionTest.aNewTextShowsTheFirstPropertyOrScore`: `defaultFor(emptyList()).text == "{score}"`; with Properties `lives`, `score` → `"{lives}"`.
- `BlockTypeEditorActionsTest` (plain JVM, a recording `onCommand`), asserting against literal `Invocation`s (`Invocation` is a data class):
  - `deletingUsesTheCommandOfTheKind`: Actor type → `Invocation(DeleteActorType, DeleteActorType.Input("Ball"))`; Manager type → `DeleteManagerType`.
  - `eventCommandsNameTheTypeAndEvent`: `setEventKey(2, KeyChoice.A)` → `Invocation(SetEventKey, SetEventKey.Input("Ball", 2, KeyChoice.A))`; same for timer, other Actor type, and `removeEvent(1)`.
  - `blockCommandsNameTheEventAndBlock`: `addBlock(0, BlockKind.STOP)` → `AddBlock.Input("Ball", 0, BlockKind.STOP.createDefault())` (no `index`); `addBlock(0, kind, gap = 3)` → with `index = 3`; `moveBlock(0, index = 2, gap = 2)` and `gap = 3` → nothing dispatched (no move); `gap = 0` → `MoveBlock.Input("Ball", 0, 2, 0)`.
  - `overridingAnEventRunsTheParentsFirst`: the `AddEvent.Input` carries the event's type/key/other/timer and `listOf(BlockDefinition.RunParentEvent)`.
  - `aNewTextOnAManagerWithoutPropertiesShowsScore`.
