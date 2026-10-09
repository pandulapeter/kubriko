# Don't run the scene's undo or redo shortcut while a text field in the scene editor has focus

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Desktop
**Challenged:** sound
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/KeyboardInputListener.kt`,
`tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt`,
`tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/KeyboardInputListenerTest.kt`,
`tools/scene-editor/CLAUDE.md`

Ships in `tool-scene-editor`. Internal only; no public API changes. Runs after plan 40 (same files).

## Problem

`KeyboardInputListener.onKeyPressed` gates the T/S/R mode shortcuts on `isTextInputFocused()` but not undo/redo:

```kotlin
override fun onKeyPressed(key: Key) {
    if (isShortcutModifierActive) {
        when (key) {
            Key.Z -> if (isShiftActive) onRedo() else onUndo()
            Key.Y -> onRedo()
        }
    } else if (!isTextInputFocused()) {
        SceneEditorInteractionMode.entries.firstOrNull { it.shortcut == key }?.let(onInteractionModeSelected)
    }
}
```

The property panel's text fields are Compose `BasicTextField`s (`TextInput` in `tools/ui-components`, and the hex field
in `ColorPropertyEditor`), and on desktop `BasicTextField` handles these shortcuts itself — verified in
`foundation-desktop-1.12.1-sources.jar`: `commonKeyMapping` in `text/KeyMapping.kt` maps
`systemShortcutModifiers + Z → KeyCommand.UNDO`, `+ Shift + Z` and `+ Y → KeyCommand.REDO` (the skiko mapping uses Meta
on macOS, Ctrl elsewhere), and `TextFieldKeyInput` applies them through its `UndoManager` and `onValueChange`. The
plugin's AWT listener sees the same key event, so one Ctrl/Cmd+Z in a property field does both:

1. the text field undoes its text and calls `onValueChange`, which for a property editor calls
   `onBeforePropertyChange(editKey)` (`SceneDocument`) and writes the property — a **new** history entry, which clears the
   redo stack;
2. the scene undo restores the previous snapshot, replacing the actors (so the selected actor instance the field is
   bound to is swapped for a restored copy).

The result is a field showing one value, an actor with another, and a lost redo history. The scene editor's
`CLAUDE.md` currently records the gap as intended ("Escape is gated too; undo/redo shortcuts are not").

## Fix

1. Gate the whole shortcut branch on text focus, so a focused text field owns Ctrl/Cmd+Z/Y:

   ```kotlin
   override fun onKeyPressed(key: Key) {
       // (plan 40's back-key flag stays first)
       if (isTextInputFocused()) return
       if (isShortcutModifierActive) {
           when (key) {
               Key.Z -> if (isShiftActive) onRedo() else onUndo()
               Key.Y -> onRedo()
           }
       } else {
           SceneEditorInteractionMode.entries.firstOrNull { it.shortcut == key }?.let(onInteractionModeSelected)
       }
   }
   ```

   The Undo/Redo buttons of the editor UI are unaffected.

2. To make the listener testable without driving the sealed `KeyboardInputManager`'s internal callbacks, replace the
   `keyboardInputManager: KeyboardInputManager` constructor parameter with `isKeyPressed: (Key) -> Boolean`, used by
   `isShortcutModifierActive` and `isShiftActive`; `EditorController` passes `isKeyPressed = keyboardInputManager::isKeyPressed`.
   Update the construction in `KeyboardInputListenerTest` (added by plan 40) accordingly.

3. `tools/scene-editor/CLAUDE.md`: change "Escape is gated too; undo/redo shortcuts are not" to say the undo/redo
   shortcuts are gated as well, because a focused `BasicTextField` handles Ctrl/Cmd+Z/Y itself; and in the
   `KeyboardInputListener` paragraph, move "(gated while a text input is focused)" so it covers undo/redo too.

## Tests

In `KeyboardInputListenerTest` (package `com.pandulapeter.kubriko.sceneEditor.implementation.actors`, desktopTest), with
`isKeyPressed = { it == Key.CtrlLeft }` (plus `Key.ShiftLeft` where needed) and counting `onUndo`/`onRedo` lambdas:

- `undoShortcutIsIgnoredWhileATextInputIsFocused` — `isTextInputFocused = { true }`, `onKeyPressed(Key.Z)` → `onUndo` 0.
- `redoShortcutsAreIgnoredWhileATextInputIsFocused` — same for Ctrl+Shift+Z and Ctrl+Y → `onRedo` 0.
- `undoAndRedoShortcutsWorkWithoutTextFocus` — `isTextInputFocused = { false }`: Ctrl+Z → `onUndo` 1; Ctrl+Shift+Z and
  Ctrl+Y → `onRedo` 2.

## Manual check

Desktop: open the scene editor, select an actor, type into one of its text/number property fields, press Ctrl+Z (Cmd+Z
on macOS) — only the field's text reverts, and the scene's Redo button state is unchanged. Click on the canvas (field
loses focus), press Ctrl/Cmd+Z — the scene undo runs as before.
