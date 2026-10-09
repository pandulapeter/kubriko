# Make `KeyboardInputManagerImpl` reachable from tests without composition and test its one-tick latch

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-keyboard-input
**Files:**
- `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerImpl.kt`
- new `plugins/keyboard-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerTest.kt`

## Problem
plugin-keyboard-input has no tests at 2480325f (`plugins/keyboard-input/src` has no test source set). Its subtle part is
the per-tick snapshot with the one-tick latch and dirty re-arm (KeyboardInputManagerImpl.kt:76–109):

```kotlin
if (isActiveKeysDirty) {
    activeKeysSnapshot = buildActiveKeysSnapshot()
    ...
    isActiveKeysDirty = hasLatchedKeys && !activeKeysCache.containsAll(keysPressedSinceLastSnapshot)
}
```

Key events only arrive through the platform handler, which `Composable()` builds with the `@Composable expect fun
createKeyboardEventHandler(onKeyPressed = ::onKeyPressed, onKeyReleased = ::onKeyReleased, ...)`, so a headless
`newManualKubriko` test never receives any; `onKeyPressed`/`onKeyReleased` (:112, :121) are `private`.

## Fix
Smallest seam, recommended: widen `onKeyPressed(key: Key)` and `onKeyReleased(key: Key)` from `private` to `internal`
(the class is already `internal`, so nothing outside the module sees them). The test calls them exactly as the
platform handler does, on the test thread. No constructor change is needed — `onUpdate` never touches the handler.
(Alternative: an internal constructor parameter for the handler factory, in the way `SpriteManagerImpl` takes
`imageLoader`; heavier, since the factory is `@Composable`, and not needed here.)

## Behaviour
Unchanged — a visibility change inside an internal class.

## Public API
None.

## Tests
`KeyboardInputManagerTest` (desktopTest, `newManualKubriko` from `:tools:test-fixtures` with
`KeyboardInputManager.newInstance()` and a `KeyboardInputAware` test actor recording `handleActiveKeys` sets per
tick; wait for the actor to register with `tickUntil`, as `PhysicsContractTest` does for its plugin):
- a key pressed and released between two ticks appears in exactly one `handleActiveKeys` call, then disappears;
- a held key appears every tick until released, and the tick after release delivers an empty set once
  (`hasSentEmptyMap`), then no further empty sets;
- pressing an already-held key does not fire `onKeyPressed` again;
- `isKeyPressed` is live (true right after `onKeyPressed`, before any tick; false right after `onKeyReleased`);
- on focus loss every held key gets `onKeyReleased` — only if focus can be driven headlessly (the
  `StateManager.isFocused` contract in CLAUDE.md says a never-shown instance stays focused); otherwise skip and say
  why.
A test that fails on a real defect is `@Ignore`d with the reason and reported as a new finding — this plan does not
change behaviour.

## Verify
`./gradlew :plugins:keyboard-input:compileKotlinDesktop :plugins:keyboard-input:desktopTest`

## Manual check
none
