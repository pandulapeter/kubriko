# Stop Escape from closing the scene editor over unsaved changes or while typing

**Challenged:** amended — the decision also takes `isSettingsOpen`, so Escape still dismisses the editor's Settings window when the scene has unsaved changes (the keyboard listener is a process-wide AWT listener, and today the last Escape step closes Settings first); `Files` adds `InternalSceneEditor.kt`.

**Kind:** bug (data loss)  ·  **Severity:** high  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/KeyboardInputListener.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/KeyboardHelpers.kt`, `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/NavigateBackActionTest.kt` (new), `tools/scene-editor/CLAUDE.md`

**Decision needed:** Escape's last step ("close editor") is a documented shortcut. Keep it, but only when the scene has no unsaved changes and no text field is focused (option A), or drop the close step entirely so Escape only deselects (option B)? — recommended: option A.

## Problem

`KeyboardInputListener` (~56) routes every Escape release to `navigateBack`, with no text-focus gate:

```kotlin
override fun onKeyReleased(key: Key) = handleKeyPressed(
    key = key,
    onNavigateBackRequested = navigateBack,
)
```

`handleKeyPressed` (`KeyboardHelpers.kt` ~50-56) calls it for `Key.Escape` and `Key.Back`, and `EditorController.navigateBack` (~429-439) closes the editor when nothing is selected:

```kotlin
private fun navigateBack() {
    if (selectedUpdatableActor.value.first == null) {
        if (selectedTypeId.value == null) {
            onCloseRequest()
        } else {
            selectActorType(null)
        }
    } else {
        deselectSelectedActor()
    }
}
```

`onCloseRequest` ends in `onCloseRequest()` of the host — `exitApplication` for `SceneEditor.show()`. There is no save prompt, so a user who presses Escape once too often (deselect actor → deselect type → **close**) loses every unsaved edit. `KeyboardInputManager` listens window-wide (see `tools/scene-editor/CLAUDE.md`, "Navigation"), so the same happens while the filter field or a property field is focused and the user presses Escape to leave it.

## Fix

1. In `KeyboardHelpers.kt`, replace `handleKeyPressed` with a pure decision:
   ```kotlin
   internal enum class NavigateBackAction { DESELECT_ACTOR, DESELECT_TYPE, CLOSE, NONE }

   internal fun navigateBackAction(
       hasSelectedActor: Boolean,
       hasSelectedType: Boolean,
       isSettingsOpen: Boolean,
       isSceneModified: Boolean,
       isTextInputFocused: Boolean,
   ): NavigateBackAction
   ```
   Option A: `NONE` while a text input is focused; otherwise actor → `DESELECT_ACTOR`, type → `DESELECT_TYPE`, settings open → `CLOSE` (the host lambda in `InternalSceneEditor` turns that into "close the Settings window", as today), else `CLOSE` only if `!isSceneModified`, else `NONE`. (Option B: never `CLOSE` for the editor itself.)

   Why `isSettingsOpen`: `KeyboardInputManager` on desktop is a process-wide `AWTEventListener` and `onKeyReleased` is not gated by focus, so today Escape pressed in the Settings window (nothing selected) reaches `navigateBack` → `onCloseRequest` → the `InternalSceneEditor` lambda closes Settings. Without this input a modified scene would make Escape stop dismissing Settings.
2. `KeyboardInputListener.onKeyReleased`: `if (key == Key.Escape || key == Key.Back) navigateBack()`; the controller does the gating.
3. `EditorController` gets a constructor parameter `isSettingsOpen: () -> Boolean`; `InternalSceneEditor` passes `{ isSettingsOpen.value }` (that state is already remembered before the controller). `navigateBack` evaluates `navigateBackAction(getSelectedActor() != null, selectedTypeId.value != null, isSettingsOpen(), isSceneModified.value, focusedTextInputCount > 0)` and dispatches with a `when`; `CLOSE` calls `onCloseRequest()` exactly as today, and the `InternalSceneEditor` lambda keeps its settings / file-dialog checks unchanged (after plan 69 it ends in `currentOnCloseRequest()`).
4. `tools/scene-editor/CLAUDE.md` → Navigation: "handles Escape (deselect actor → deselect type → close editor)" → "… → close editor, the last step only when the scene has no unsaved changes"; and the closing sentence "Discrete shortcuts (Escape, undo/redo) are **not** gated" → "Escape is gated too; undo/redo shortcuts are not."

Not in scope: Ctrl/Cmd+Z inside a focused text field also triggers a scene undo (same global listener); that is a separate behaviour question.

## Tests

`NavigateBackActionTest` (desktopTest, pure):
- focused text input → `NONE` in every other combination;
- selected actor → `DESELECT_ACTOR` (also when a type is selected);
- only a type → `DESELECT_TYPE`;
- nothing selected, unmodified → `CLOSE`; nothing selected, modified → `NONE` (option A);
- nothing selected, settings open → `CLOSE` whether or not the scene is modified; settings open but an actor selected → `DESELECT_ACTOR`.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop, open Annoyed Penguins → Scene Editor:
1. Place one actor, deselect it, press Escape several times: the editor stays open. Save, press Escape: it closes.
2. Reopen, click into the filter field (nothing selected, scene unmodified) and press Escape: the editor stays open.
3. Select an actor and a type, press Escape twice: the actor, then the type, is deselected as before.
4. Place an actor, deselect it, open Settings, press Escape: the Settings window closes and the editor stays open.
