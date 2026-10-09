# Report key releases only for keys that were reported as pressed

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (worst on Desktop, whose listener is JVM-wide)
**Challenged:** sound
**Files:** `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerImpl.kt`, `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputAware.kt` (KDoc of `onKeyReleased`), `plugins/keyboard-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerTest.kt`, `plugins/keyboard-input/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-keyboard-input`. Changes when a public callback fires → **Decision**.

## Problem

Presses are gated on focus and de-duplicated; releases are not gated at all:

```kotlin
// KeyboardInputManagerImpl.kt
internal fun onKeyPressed(key: Key) {
    if (!activeKeysCache.contains(key) && stateManager.isFocused.value) {
        keyboardInputAwareActors.value.forEach { it.onKeyPressed(key) }
        ...
    }
}

internal fun onKeyReleased(key: Key) {
    keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }
    activeKeysCache.remove(key)
    isActiveKeysDirty = true
}
```

So `onKeyReleased` fires for keys the instance never reported as pressed, and can fire twice for one press:

- **Desktop**: the backend is `Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)`, which
  sees key events of **every window of the JVM**. A key pressed and released in another window (the scene editor's
  window, a second game window, a Swing dialog) — while this instance is unfocused — skips `onKeyPressed` but still
  reaches every actor's `onKeyReleased`. That is the root of the tools-lane finding where Escape released in the
  Showcase window closes the scene editor (the tools lane fixes the editor locally either way).
- **Double release (all platforms whose listener outlives focus)**: hold A, lose focus → `releaseAllActiveKeys()`
  reports A released; the physical key-up then arrives through the JVM-wide listener (Desktop, focus moved to another
  JVM window) → A is reported released a second time.
- In-repo consumers act on releases: `game-wallbreaker`'s and `game-space-squadron`'s `UIManager.onKeyReleased`
  start/resume the game on Space/Enter; Space Squadron already carries a `shouldDismissNextSpacebarRelease` flag to
  survive a release it did not want.

## Decision

- **(a) Deliver `onKeyReleased` only for keys in `activeKeysCache` (recommended).** Every release is then paired with
  exactly one earlier `onKeyPressed` of the same instance; a key pressed while unfocused produces neither callback,
  and the focus-loss flush stays the only release for a key held across focus loss. Matches what `isKeyPressed` and
  `handleActiveKeys` already report. Observable change: a consumer that relied on hearing releases of keys pressed
  elsewhere (none in the repo; Tesselar's `ControlOverlayManager` implements
  `KeyboardInputAware` but only overrides `handleActiveKeys`, and `MiniMapPanKeys` only calls `isKeyPressed` — grepped) stops hearing them.
- (b) Gate releases on `isFocused` like presses. Removes the cross-window case while unfocused, but still delivers a
  release for a key pressed before focus arrived, and does not fix the double release when the key-up lands after
  focus returns.
- (c) Leave the behaviour and document it in `KeyboardInputAware.onKeyReleased` KDoc.

## Fix (for option a)

1. `onKeyReleased`: `if (activeKeysCache.remove(key)) { keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }; isActiveKeysDirty = true }`.
   Keep dispatching before or after the removal consistently with today (today it dispatches first, then removes, so a
   handler calling `isKeyPressed(key)` sees `true`; preserve that by checking `contains` first, dispatching, then
   removing).
2. `releaseAllActiveKeys()` stays as is (it only iterates `activeKeysCache`).
3. KDoc of `KeyboardInputAware.onKeyReleased`: "Called when a key reported through [onKeyPressed] is released, or when
   the instance loses focus while it is held. Every release follows exactly one press."
4. `CLAUDE.md` "Key API Details": state the pairing guarantee and that the Desktop listener is JVM-wide (keys of other
   windows are ignored because presses are focus-gated and releases are paired).

## Tests

In `KeyboardInputManagerTest` (desktopTest, uses the existing `withKeyboard` harness built on
`:tools:test-fixtures`' `newManualKubriko`):
- `releasingAKeyThatWasNeverPressedIsNotReported`: `manager.onKeyReleased(Key.B)` → `actor.releasedKeys` is empty and
  a following `kubriko.tick()` reports no `Key.B`.
- `releasingAKeyTwiceReportsItOnce`: press A, release A, release A → `releasedKeys == listOf(Key.A)`.
- `handlerSeesTheKeyAsPressedDuringItsRelease` (pins step 1's ordering): a recording actor calls
  `manager.isKeyPressed(key)` inside `onKeyReleased` and records `true`.

The focus-loss path stays untested, as the class KDoc of the test explains (no public way to drive
`StateManager.isFocused` headlessly).

## Manual check

Desktop Showcase with the scene editor enabled (`showcase.isSceneEditorEnabled=true`): open a game and its scene
editor window; press and release Space/Escape in the editor window — the game must not react. Hold a movement key in
the game, click the editor window, release the key there — the game reports exactly one release.
