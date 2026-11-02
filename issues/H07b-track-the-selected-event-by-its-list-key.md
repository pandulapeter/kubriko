<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Track the type editor's selected Event by its `ListItemKeys` key instead of its index, so undo, redo, rejected commands, and the AI's Event changes keep the same Event selected

**Challenged:** amended — Files now include `BlockTypeEditor`'s caller (`WorkspaceCenter` after H02b) and the tests the new `onCommand` shape touches, which the merge check would have stopped on; the pending-add effect is keyed on the pending index too, so an `onSuccess` after the recomposition still selects the new Event; K09 note.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** UI-15  ·  **Depends on:** H07a, H02b (where `BlockTypeEditor` is called). **K09:** nothing to re-locate; `commands.dispatch(invocation, onSuccess)` keeps its shape through lane K (K02a turns a throw into a rejection, which doesn't call `onSuccess`, as wanted here)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/actorTypes/BlockTypeEditor.kt`, `.../ui/actorTypes/BlockTypeEditorActions.kt`, `.../ui/actorTypes/EventList.kt`, `.../ui/actorTypes/EventSelection.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/actorTypes/EventSelectionTest.kt`, `.../ui/actorTypes/BlockTypeEditorActionsTest.kt` (H07a's, for the new `onCommand` shape), `.../ui/actorTypes/ListItemKeysTest.kt`, new `.../ui/actorTypes/BlockTypeEditorEventSelectionTest.kt` (uiTest), the caller of `BlockTypeEditor`: `.../ui/WorkspaceCenter.kt` after H02b (`.../ui/WorkspaceScreen.kt` if H02b was skipped) and `creator/src/test/.../ui/WorkspaceCenterTest.kt` if it passes an `onCommand`, `creator/CLAUDE.md`

## Problem

`ui/actorTypes/BlockTypeEditor.kt`:
```kotlin
var requestedEventIndex by remember(definition.name) { mutableStateOf<Int?>(null) }
LaunchedEffect(highlight?.key) { highlight?.eventIndex?.let { requestedEventIndex = it } }
val eventIndex = selectedEventIndex(requestedEventIndex, definition.events.size)
…
onOverride = { event -> onCommand(Invocation(AddEvent, …)); requestedEventIndex = definition.events.size },
onAdded = { type -> onCommand(Invocation(AddEvent, AddEvent.Input(name, type))); requestedEventIndex = definition.events.size },
onMoved = { index, targetIndex -> onCommand(Invocation(MoveEvent, …)); requestedEventIndex = selectedEventIndexAfterMove(eventIndex, index, targetIndex) },
onRemoved = { index -> onCommand(Invocation(RemoveEvent, …)); if (eventIndex != null && index < eventIndex) requestedEventIndex = eventIndex - 1 },
```
(after H07a the `onCommand(...)` parts are `actions.addEvent(...)` etc.; the index bookkeeping is the same). The selection is a
position that only the editor's own moves and removals shift, and they shift it even when the command is rejected (a read-only
project, a failed validation). An undo, a redo, or an AI `add_event`/`remove_event`/`move_event` before the selected Event silently
shows another Event's Blocks, and the next Block edit goes to the wrong Event. After "add", `events.size` is assumed to be the
new Event's index even if the add was rejected.

Events have no identity, but `ui/actorTypes/ListItemKeys.kt` gives them keys that follow them between versions (common start
and end keep theirs, an equal item takes its old key on a move, an edited one the key of its place), already used inside
`EventList` (`val itemKeys = remember { ListItemKeys<EventDefinition>() }; val keys = remember(itemKeys, events) { itemKeys.update(events) }`).

## Fix

1. Hoist the keys into `BlockTypeEditor`: `val itemKeys = remember(name) { ListItemKeys<EventDefinition>() }`, `val eventKeys = remember(itemKeys, definition.events) { itemKeys.update(definition.events) }`; pass `keys = eventKeys` and `isRecentlyAdded = itemKeys::isRecentlyAdded` to `EventList`, which drops its own `itemKeys`/`keys` (its `key(listIdentity)` block stays for the drag state and list state).
2. Selection state: `var selectedEventKey by remember(name) { mutableStateOf<Long?>(null) }` and `var pendingEventIndex by remember(name) { mutableStateOf<Int?>(null) }` (an Event that a successful add will create at that index).
   `EventSelection.kt` gets a pure resolver replacing `selectedEventIndex`/`selectedEventIndexAfterMove`:
   ```kotlin
   /**
    * Which Event is shown: the one of [selectedKey] while it exists, else the one at [fallbackIndex] (where the selected one
    * was, clamped), else the first; none without Events.
    */
   internal fun selectedEventIndex(keys: List<Long>, selectedKey: Long?, fallbackIndex: Int?): Int?
   ```
   with `val eventIndex = selectedEventIndex(eventKeys, selectedEventKey, lastShownIndex)`, where `lastShownIndex` is remembered (updated in a `SideEffect` to the index just shown), so a removed selected Event falls back to its neighbour as today. Then `SideEffect { selectedEventKey = eventIndex?.let(eventKeys::get) }` keeps the key in step when the fallback was used.
3. Callbacks:
   - `onSelected = { index -> selectedEventKey = eventKeys[index] }`.
   - Moves and removals: only dispatch; no bookkeeping. The key follows the moved Event (equal item); removing another Event leaves the key valid; removing the selected one falls back to its index.
   - Add and override: dispatch with `onSuccess`, so a rejected add selects nothing new. `BlockTypeEditorActions.addEvent(type, onAdded: () -> Unit)` and `overrideEvent(event, onAdded)` take the callback and call `onCommand(invocation, onAdded)`; change `BlockTypeEditorActions`' `onCommand` to `(Invocation<*>, onSuccess: () -> Unit) -> Unit` and `BlockTypeEditor`'s `onCommand` parameter likewise (the caller in `WorkspaceScreen`/`WorkspaceCenter` passes `controller.commands::dispatch`, which already has `onSuccess`). `onAdded = { pendingEventIndex = definition.events.size }` captured before dispatch (the index it will be at); a `LaunchedEffect(eventKeys, pendingEventIndex)` resolves it: `pendingEventIndex?.takeIf { it < eventKeys.size }?.let { selectedEventKey = eventKeys[it]; pendingEventIndex = null }`. Keyed on both, so it resolves whichever comes second: the recomposition with the new Event, or `onSuccess` (which `dispatch` calls after `apply`, possibly after the new definition was already composed when `apply` suspends). The drag state's `onDrop` (`currentOnCommand(...)`) and every other `onCommand` call pass `{}` as `onSuccess`.
   - AI highlight: `LaunchedEffect(highlight?.key) { highlight?.eventIndex?.let { index -> eventKeys.getOrNull(index)?.let { selectedEventKey = it } } }` (read `eventKeys` through `rememberUpdatedState`).
   - `highlight?.takeIf { it.eventIndex == eventIndex }` and `listIdentity = name to eventIndex` for `EventBlocks`: use `name to selectedEventKey` instead, so an Event moving under the selection doesn't reset the Block list's scroll.
4. Delete `selectedEventIndexAfterMove` and the old `selectedEventIndex(requestedIndex, eventCount)`, and their tests in `EventSelectionTest`, replaced by the new resolver's.

`creator/CLAUDE.md`: the sentence "(`move_event`; the selected Event stays selected)" holds; add after the `ListItemKeys` sentence that the editor's selected Event is one of those keys, so it stays on the same Event through undo, redo, and the AI's Event changes.

## Tests

- `EventSelectionTest` (pure): no Events → null; key present at index 2 → 2; key gone with fallback 5 and 3 Events → 2; key null, fallback null → 0.
- `ListItemKeysTest` (existing file, one more case): `anEventInsertedBeforeAnotherLeavesItsKey`: `update([a, b])` then `update([c, a, b])` → `a`'s and `b`'s keys unchanged.
- `BlockTypeEditorEventSelectionTest` (offscreen, `uiTest(width = 1200, height = 700)`, a `BlockTypeEditor` fed from a `mutableStateOf(definition)` the test changes directly, as undo or the AI would):
  - `anEventInsertedBeforeTheSelectedOneLeavesTheSelectionOnIt`: Events `[Create, Update]`, click `Update`; set the definition to `[KeyPressed, Create, Update]` → the Blocks pane shows `Update`'s Blocks (assert a Block text unique to it); `saveSnapshot("event-selection-after-insert")`. Fails at HEAD, which shows `Create`.
  - `aRejectedAddSelectsNothingNew`: `onCommand` that never calls `onSuccess`; add an Event → the selection stays on the one selected.
  - `aSuccessfulAddSelectsTheNewEvent`: `onCommand` that appends the Event to the state and calls `onSuccess` → the new Event is selected; and once more with an `onCommand` that appends the Event, lets the test recompose (`waitForIdle()`), and only then calls `onSuccess` → selected too.
  Look at the PNG.
