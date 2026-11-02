<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give the workspace one selection (a type, a Scene, or nothing) that every caller goes through, instead of four places pairing `select` with `open`/`close` by hand

**Challenged:** amended — the "creating a type leaves the Scene open" bug was fixed in 0a41d2ad8; the plan now replaces that fix's hand pairing and extends its existing test instead of adding one that would pass at HEAD.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-8  ·  **Depends on:** none (see "K09" below: written against HEAD, re-locate if K09 landed first)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceSelection.kt`, new `.../workspace/SelectionController.kt`, `.../workspace/WorkspaceController.kt` (or K09's factory, see below), `.../ui/SidebarState.kt`, `.../ui/WorkspaceScreen.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/SelectionControllerTest.kt` (new), `.../workspace/WorkspaceControllerTest.kt`, `creator/CLAUDE.md`

## Problem

"An Actor/Manager type is selected" and "a Scene is shown" are two state holders that the UI keeps mutually exclusive by hand:
`BlockTypesController.selectedName` (`workspace/BlockTypesController.kt`, `private val _selectedName = MutableStateFlow<String?>(null)`)
and `SceneEditingController.state` (`scenes/SceneEditingController.kt`, `SceneEditingState.Closed` or a Scene).

The pairing is repeated in four places at the current HEAD:

- `ui/SidebarState.kt`, `rememberFolderTreeActions`:
  ```kotlin
  onSelected = { group, name ->
      if (group == FolderGroup.SCENES) {
          controller.resources.value[ResourceGroup.SCENES].orEmpty().firstOrNull { SceneStore.nameOf(it.file) == name }?.let { scene ->
              controller.blockTypes?.select(null)
              controller.sceneEditing.open(scene)
          }
      } else {
          controller.sceneEditing.close()
          controller.blockTypes?.select(name)
      }
  },
  onSelectionCleared = {
      controller.sceneEditing.close()
      controller.blockTypes?.select(null)
  },
  ```
- `ui/WorkspaceScreen.kt`, the AI-follow effect:
  ```kotlin
  if (isAiEnabled && isFollowingAi && blockTypesController?.get(name) != null && blockTypesController.selectedName.value != name) {
      controller.sceneEditing.close()
      blockTypesController.select(name)
  }
  ```
- `ui/WorkspaceScreen.kt`, the render condition `if (sceneEditing == SceneEditingState.Closed && blockTypesController != null && selectedBlockType != null)`, and `openSceneName = (sceneEditing as? SceneEditingState.Open)?.sceneName` for the sidebar highlight.
- `ui/WorkspaceScreen.kt`, `AiPanel(onShowStep = …, onTypeSelected = …, onSceneSelected = …)`, each guarding with `blockTypesController?.get(target.name) != null` / `openSceneName != target.name` before `folderTreeActions.onSelected(...)`; and `BlockTypeEditor(onTypeSelected = blockTypesController::select)`.

A fifth pairing was added during the review: `WorkspaceController.createActorType` / `createManagerType` (0a41d2ad8) now run
`commands.dispatch(invocation) { sceneEditing.close(); blockTypes?.select(name) }`, the same pairing by hand, pinned by
`WorkspaceControllerTest.creatingAnActorOrManagerTypeClosesTheSceneShownAndSelectsTheType`. Also, a Scene that is `Preparing`, `Failed`, or
`Unsupported` occupies the center pane but isn't highlighted in the sidebar, since only `Open` counts as `openSceneName`.

## Fix

What goes into the workspace layer (`workspace/`, no Compose):

1. `workspace/WorkspaceSelection.kt`:
   ```kotlin
   /** What the workspace's center shows: one type, one Scene (being prepared, open, or failed), or nothing. */
   internal sealed interface WorkspaceSelection {
       data object None : WorkspaceSelection
       data class BlockType(val name: String) : WorkspaceSelection
       data class Scene(val name: String) : WorkspaceSelection
   }
   ```
   plus an extension `internal val SceneEditingState.sceneName: String?` (null for `Closed`, `SceneStore.nameOf(scene.file)` for the four others; `Open.sceneName` already exists, keep it).

2. `workspace/SelectionController.kt`, the only writer of the pair:
   ```kotlin
   internal class SelectionController(
       private val blockTypes: BlockTypesController?,
       private val sceneEditing: SceneEditingController,
       private val sceneOf: (name: String) -> ProjectResource?,
       scope: CoroutineScope,
   ) {
       /** One value for the center, the sidebar highlight, and the AI panel. A shown Scene wins over a type name left behind. */
       val current: StateFlow<WorkspaceSelection>   // combine(blockTypes?.selectedName ?: flowOf(null), sceneEditing.state) { … }.stateIn(scope, SharingStarted.Eagerly, initial)
       /** Shows the type [name]: closes the Scene. False (and nothing changes) when there is no such type. */
       fun selectType(name: String): Boolean
       /** Shows the Scene [name] unless it is shown already: deselects the type. False when there is no such Scene file. */
       fun openScene(name: String): Boolean
       fun clear()
   }
   ```
   - `selectType`: `if (blockTypes?.get(name) == null) return false; sceneEditing.close(); blockTypes.select(name); return true`.
   - `openScene`: `val scene = sceneOf(name) ?: return false; if (sceneEditing.state.value.sceneName == name) return true; blockTypes?.select(null); sceneEditing.open(scene); return true`.
   - `clear`: `sceneEditing.close(); blockTypes?.select(null)`.
   - The derived value: `sceneEditing.state.sceneName?.let(::Scene) ?: selectedName?.let(::BlockType) ?: None`; compute the initial value synchronously from the two `.value`s so `current.value` is right before the first emission.
   - `BlockTypesController.select` stays (its rename/delete/reload fix-ups at `_selectedName.value == oldName` etc. stay where they are and stay tested by `BlockTypesControllerTest`); give it a KDoc line saying the workspace selects through `SelectionController`. `SceneEditingController.open/close` keep their other callers (`reload`, `onScenesChanged`'s close of a deleted Scene), which only ever close or keep the same kind, so the invariant holds.

3. `WorkspaceController` creates it after `blockTypes` and `sceneEditing` and exposes `val selection`, with
   `sceneOf = { name -> _resources.value[ResourceGroup.SCENES].orEmpty().firstOrNull { SceneStore.nameOf(it.file) == name } }`.
   `createActorType` / `createManagerType` change from `commands.dispatch(invocation) { sceneEditing.close(); blockTypes?.select(name) }` to `commands.dispatch(invocation) { selection.selectType(name) }` (the type is in `BlockTypesController` by then: `put` updates `_types` synchronously before `onSuccess` runs).

**K09 dependency.** Lane K's K09 splits `WorkspaceController` into a factory (parts created in dependency order), a `ProjectChanges`
router, and the controller. If K09 landed first: build `SelectionController` in the factory right after `BlockTypesController` and
`SceneEditingController`, pass it to the controller, and keep the public `WorkspaceController.selection` property; put `sceneOf` on
whatever K09 calls the resources holder; the `createActorType`/`createManagerType` change goes wherever K09 moved those functions.
Nothing in this plan touches the router.

UI changes (`ui/`):
- `rememberFolderTreeActions`: `onSelected = { group, name -> if (group == FolderGroup.SCENES) controller.selection.openScene(name) else controller.selection.selectType(name) }`, `onSelectionCleared = controller.selection::clear`.
- `WorkspaceScreen`: collect `controller.selection.current` once (`val selection by …collectAsState()`); `selectedBlockTypeName = (selection as? WorkspaceSelection.BlockType)?.name`, `openSceneName = (selection as? WorkspaceSelection.Scene)?.name` (now also highlights a preparing or failed Scene, which is intended); the type editor renders on `selection is WorkspaceSelection.BlockType && selectedBlockType != null` (still inside 4ab2bdf08's `key(selectedBlockType.name) { BlockTypeEditor(...) }`, which keeps one type's field text out of another's), the hint on `WorkspaceSelection.None`, the Scene `when` stays on `sceneEditing` for its sub-state. AI follow: `if (isAiEnabled && isFollowingAi && selection != WorkspaceSelection.BlockType(name)) controller.selection.selectType(name)`. `AiPanel`: `onShowStep` calls `selectType(target.name)` / `openScene(target.name)` without its own guards, `onTypeSelected = { controller.selection.selectType(it) }`, `onSceneSelected = { controller.selection.openScene(it) }`. `BlockTypeEditor(onTypeSelected = { controller.selection.selectType(it) })`.
- `blockTypesController.select` and `sceneEditing.open/close` no longer appear anywhere under `ui/` and the three `onClose = controller.sceneEditing::close` of `SceneEditorPane` and both `SceneNotice`s change to `controller.selection::clear` (`sceneEditing::reload` stays). `grep -rn "\.select(\|sceneEditing\.open\|sceneEditing\.close" creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui` returns nothing afterwards.

`creator/CLAUDE.md`: in the sidebar paragraph ("Clicking the selected type or Scene again, or empty space in the sidebar, selects nothing (`onSelectionCleared`: …)") add that what the center shows is one `WorkspaceSelection` (`SelectionController`, the only place that pairs selecting a type with closing the Scene), and add `SelectionController` to the `workspace` row of the package table. No strings change.

## Tests

`SelectionControllerTest` (new, `runTest` with `UnconfinedTestDispatcher`; a real `BlockTypesController` over `FakeBlockTypeRepository` and a real `SceneEditingController` with a fake `EntryPointLoader`, built the way `WorkspaceControllerTest` builds them, or simply a `WorkspaceController` from that test's `createController` helper):
- `selectingATypeClosesTheShownScene`: open a Scene, `selectType("Player")` → `current.value == BlockType("Player")`, `sceneEditing.state.value == Closed`.
- `openingASceneDeselectsTheType`: select a type, `openScene("level")` → `Scene("level")`, `blockTypes.selectedName.value == null`.
- `anUnknownTypeOrSceneChangesNothing`: `selectType("Missing")` and `openScene("missing")` return false and leave `current` as it was.
- `openingTheSceneShownAlreadyDoesNotOpenItAgain`: count `EntryPointLoader` loads / `SceneEditingState.Preparing` emissions; a second `openScene("level")` adds none.
- `aPreparingOrFailedSceneIsTheSelection`: with a loader that never completes, `openScene` → `Scene("level")` while `Preparing`.
- `deletingTheSelectedTypeSelectsNothing` and `renamingTheSelectedTypeFollowsIt`: through commands (`DeleteActorType`, `RenameActorType`) → `None` / `BlockType(newName)`.
- `clearSelectsNothing`.
In `WorkspaceControllerTest`: keep `creatingAnActorOrManagerTypeClosesTheSceneShownAndSelectsTheType` (0a41d2ad8) passing unchanged, and add to it `controller.selection.current.value == WorkspaceSelection.BlockType(name)` after each creation. The cases that fail without this plan are the `SelectionControllerTest` ones (the class doesn't exist at HEAD); the behaviour fix among them is `aPreparingOrFailedSceneIsTheSelection` (at HEAD the sidebar highlights only an `Open` Scene).

## Manual check

In `:creator:run` with a Breakout copy: open a Scene, create an Actor type from the sidebar's +: the type editor shows and only the new type is highlighted; click a Scene while the project is still building: it is highlighted while "Preparing".
