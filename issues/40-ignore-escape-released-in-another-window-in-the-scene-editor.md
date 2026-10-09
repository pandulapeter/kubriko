# Ignore an Escape released in another window in the scene editor, and close the Settings window with its own Escape

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** Desktop
**Challenged:** sound
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/KeyboardInputListener.kt`,
`tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt`,
`tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/KeyboardInputListenerTest.kt` (new),
`tools/scene-editor/CLAUDE.md`

Ships in `tool-scene-editor`. Internal only; no public API changes.

## Problem

`KeyboardInputListener` (an actor in the editor's own `Kubriko` instance) navigates back on every Escape release:

```kotlin
override fun onKeyReleased(key: Key) {
    if (key == Key.Escape || key == Key.Back) {
        navigateBack()
    }
}
```

On Desktop, `plugin-keyboard-input` listens with `Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)`
(`KeyboardEventHandler.desktop.kt`), which receives key events from **every window of the JVM**. `KeyboardInputManagerImpl`
gates presses on focus but not releases:

```kotlin
internal fun onKeyPressed(key: Key) {
    if (!activeKeysCache.contains(key) && stateManager.isFocused.value) { … }
}

internal fun onKeyReleased(key: Key) {
    keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }
    …
}
```

The editor's `isFocused` follows the lifecycle of the editor window (`LifecycleFocusEffect`, RESUMED = window focused;
desktop focus debounce is 0). So with the scene editor open next to the Showcase (or any game window of the same
process), pressing Escape **in the game window** (for example to leave a menu or exit fullscreen) is delivered to the
editor as a release: the first press deselects the editor's selected actor, the next deselects the type, and the third
closes the editor window outright when the scene has no unsaved changes (`navigateBackAction` in
`helpers/NavigateBackAction.kt`).

The same leak is what makes Escape close the Settings window today: the Settings window is a separate `Window` in
`InternalSceneEditor.kt` with no key handling of its own, so its Escape press is dropped (editor window not focused)
and only the release reaches `navigateBack()` → `NavigateBackAction.CLOSE` → `isSettingsOpen.value = false`. Fixing the
leak without giving the Settings window its own Escape handling would silently remove that behaviour, which
`tools/scene-editor/CLAUDE.md` documents ("with the Settings window open it closes that instead").

The root cause is in the published `plugin-keyboard-input` (its release is not focus-gated); changing that is an
observable behaviour change of the plugin and is a separate decision in the plugins lane. This plan is the local fix
and is correct whether or not that one lands.

## Fix

1. In `KeyboardInputListener`, only treat a release as "back" when the matching press was delivered (presses are
   focus-gated by the plugin):

   ```kotlin
   private var isBackKeyPressed = false

   override fun onKeyPressed(key: Key) {
       if (key == Key.Escape || key == Key.Back) {
           isBackKeyPressed = true
       }
       if (isShortcutModifierActive) { … unchanged … }
   }

   override fun onKeyReleased(key: Key) {
       if ((key == Key.Escape || key == Key.Back) && isBackKeyPressed) {
           isBackKeyPressed = false
           navigateBack()
       }
   }
   ```

   Keep the existing `onKeyPressed` body as it is; only add the flag at its top. (Do not use
   `keyboardInputManager.isKeyPressed(key)` inside `onKeyReleased` instead: it happens to work because the plugin
   notifies actors before removing the key from its cache, but that ordering is not part of the plugin's contract.)
   Note that a held Escape is still released by the plugin when the editor window loses focus
   (`releaseAllActiveKeys`), which navigates back as it does today — that path is a press that started in the
   editor, so it is left alone.

2. In `InternalSceneEditor.kt`, give the Settings `Window` its own Escape handling so it keeps closing on Escape:

   ```kotlin
   Window(
       onCloseRequest = { isSettingsOpen.value = false },
       title = stringResource(Res.string.editor_settings),
       state = rememberWindowState(size = DpSize(200.dp, 250.dp)),
       onPreviewKeyEvent = { event ->
           if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
               isSettingsOpen.value = false
               true
           } else {
               false
           }
       },
   ) { … }
   ```

   (Imports: `androidx.compose.ui.input.key.Key`, `key`, `type`, `KeyEventType`.) Escape pressed in the main editor
   window while Settings is open keeps closing Settings through `navigateBack()` as before.

3. `tools/scene-editor/CLAUDE.md`, the `KeyboardInputListener` paragraph: say that Escape acts only when its press
   reached the editor (the desktop keyboard listener sees every window of the process, and only presses are
   focus-gated), and that the Settings window closes on its own Escape.

## Tests

New `KeyboardInputListenerTest` in `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/`
(same package as the class, so its `internal` constructor is reachable). Build the listener with
`ViewportManager.newInstance()`, `KeyboardInputManager.newInstance()` (both usable without a `Kubriko` instance here:
the listener only calls `isKeyPressed`, which reads an empty set), `isTextInputFocused = { false }`, and counting
lambdas for `navigateBack`, `onUndo`, `onRedo`, `onInteractionModeSelected`. Call the listener's callbacks directly:

- `escapeReleasedWithoutItsPressDoesNotNavigateBack` — `onKeyReleased(Key.Escape)` alone → `navigateBack` count 0.
- `escapePressedAndReleasedNavigatesBackOnce` — `onKeyPressed(Key.Escape)`, `onKeyReleased(Key.Escape)`,
  `onKeyReleased(Key.Escape)` → count 1.
- `backKeyBehavesLikeEscape` — same with `Key.Back`.

The Settings window's `onPreviewKeyEvent` is UI wiring and has no unit test.

## Manual check

Desktop, `./gradlew :app:desktop:run`: open a game that has a scene editor, open the editor, select an actor. Click into
the Showcase window and press Escape a few times — the editor keeps its selection and stays open. Click into the editor
and press Escape — it deselects, then closes as before. Open Settings from the editor, focus it, press Escape — it
closes; open it again, focus the main editor window with no actor or type selected, press Escape — Settings closes.
