# Turn gamepad-input's declaration comments into KDoc

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-gamepad-input
**Files:**
- `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadInputManagerImpl.kt`
- `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadActivationNode.kt` (created by P04)

## Problem
The code-style skill: a comment that documents a declaration is KDoc, never `//`. At 2480325f:
- GamepadInputManagerImpl.kt: `isFocusNavigationEnabled` (:55–56), the focus-direction state
  `focusDirection`/`timeUntilNextFocusStepInMilliseconds` (:59–60), `hasFocusNavigationInput` (:66–68),
  `onFocusNavigationFrame` (:73), `deadZonedX`/`deadZonedY` (:84), and the file-level constants
  `FOCUS_STICK_THRESHOLD` (:449–450), `FOCUS_REPEAT_DELAY`/`FOCUS_REPEAT_INTERVAL` (:453–454),
  `MAXIMUM_FRAME_TIME` (:460).
- `GamepadActivationNode.setGamepadInputManager` (GamepadFocusNavigation.kt:115–116 at 2480325f; in
  `GamepadActivationNode.kt` after P04).

## Fix
Run after P04. Convert each block into a KDoc block on the declaration it documents, same content. Where one `//`
block covers two declarations (`focusDirection` + `timeUntilNextFocusStepInMilliseconds`, `deadZonedX` + `deadZonedY`,
`FOCUS_REPEAT_DELAY` + `FOCUS_REPEAT_INTERVAL`), put the KDoc on the first and keep the pair adjacent. `//` notes on
statements inside function bodies stay.

## Behaviour
Unchanged — comments only.

## Public API
None (the `isFocusNavigationEnabled` override inherits its public KDoc from `GamepadInputManager`; the converted
implementation note sits on the override).

## Tests
The existing ones.

## Verify
`./gradlew :plugins:gamepad-input:compileKotlinDesktop`

## Manual check
none
