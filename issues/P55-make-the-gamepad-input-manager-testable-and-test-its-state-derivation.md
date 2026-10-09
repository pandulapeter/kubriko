# Let tests hand `GamepadInputManagerImpl` a fake event handler and test its dead zone, triggers and button diff

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-gamepad-input
**Files:**
- `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadInputManagerImpl.kt`
- new `plugins/gamepad-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadInputManagerTest.kt`

## Problem
gamepad-input's only test is `GamepadStateTest` (commonTest). The manager's logic is untested: the radial dead zone
(`applyDeadZone`, scratch `deadZonedX/Y`), trigger-to-button derivation against `triggerThreshold`, the press/release
diff (`notifyButtonChanges(gamepad, previousButtons, pressedButtons)`, :360), connect/disconnect callbacks and the
release of everything on focus loss (`releaseAllInputs`). `onUpdate` (:276–296 at 2480325f) returns early without a
handler:

```kotlin
val gamepadEventHandler = gamepadEventHandler ?: return
gamepadEventHandler.poll()
```

and the handler is only ever created inside `Composable()` by the `@Composable expect fun createGamepadEventHandler()`,
so a headless test never gets one.

## Fix
Options for the seam:
- **A (recommended):** an internal constructor parameter `initialGamepadEventHandler: GamepadEventHandler? = null`
  (default keeps `newInstance` unchanged). In `onInitialize`, if non-null: assign it and call
  `startListening(rawGamepads)`. `Composable()` already only creates a handler when `gamepadEventHandler == null`, and
  checks `isValid()` (the fake returns `true`), so the production path is untouched when the parameter is null.
- B: an internal `setEventHandlerForTesting(handler)` function — same effect, but a test-only member on the class.

The fake (test source) implements the internal `GamepadEventHandler`, keeps the `Array<RawGamepadState>` it is
handed, and lets the test write raw axes/buttons/connection into it before each tick (`poll()` does nothing).

## Behaviour
Unchanged in production (`newInstance` passes nothing).

## Public API
None — an internal constructor parameter of an internal class.

## Tests
`GamepadInputManagerTest` (desktopTest, `newManualKubriko(GamepadInputManagerImpl(..., initialGamepadEventHandler =
fake))` — the impl is internal and visible to module tests; a `GamepadInputAware` test actor records callbacks):
- dead zone: a stick inside `deadZone` reads exactly zero; just outside it is rescaled so the edge of the zone maps
  to 0 and full deflection to 1 (assert against the formula in `applyDeadZone`, not new numbers);
- triggers: a trigger value at/above `triggerThreshold` presses its button, below releases it;
- button diff: one `onGamepadButtonPressed` per newly pressed button and one `onGamepadButtonReleased` per release,
  none for unchanged buttons;
- connect/disconnect: `onGamepadConnected`/`onGamepadDisconnected` and `connectedGamepadCount`;
- disconnecting a pad with held buttons releases them.
Focus-loss release only if focus can be driven headlessly; otherwise skip and say why. A test that fails on a real
defect is `@Ignore`d with the reason and reported as a new finding.

## Verify
`./gradlew :plugins:gamepad-input:compileKotlinDesktop :plugins:gamepad-input:desktopTest`

## Manual check
none
