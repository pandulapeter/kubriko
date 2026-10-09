# Extract the gamepad focus-navigation loop from `GamepadInputManagerImpl` into an internal `GamepadFocusNavigator`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-gamepad-input
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:**
- `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadInputManagerImpl.kt`
- new `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadFocusNavigator.kt`
- new `plugins/gamepad-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadFocusNavigatorTest.kt`
- `plugins/gamepad-input/CLAUDE.md`

## Problem
`GamepadInputManagerImpl` (471 lines at 70de96c6, after P18's KDoc landed in e7f3127f) holds two responsibilities: polling pads into `GamepadState`s and
notifying actors, and a composition-driven focus-navigation loop (`isFocusNavigationEnabled`, `focusDirection`,
`timeUntilNextFocusStepInMilliseconds`, `wasActivationButtonPressed`, `wasBackButtonPressed`,
`hasFocusNavigationInput`, `previousFocusFrameTimeNanos`, `hadFocusNavigationInput`, `onFocusNavigationFrame`,
`FocusNavigationEffect`, `restFocusNavigation`, `updateFocusNavigation`, `readFocusDirection`, the `FOCUS_*` constants
— roughly :55–277 and :454–471). The public sealed `GamepadInputManager` also carries that loop's internal stacks
(`focusedActivationTargets`, `focusNavigationHosts` and their attach/detach functions, GamepadInputManager.kt:67–99).
None of it can be tested without a real `FocusManager` and frame clock.

## Decision
Awaiting the user — worth doing at all?
- **A (recommended, after P55):** extract `internal class GamepadFocusNavigator(gamepads, isAnyGamepadPressing, ...)`
  owning the state and `updateFocusNavigation(deltaTimeInMilliseconds)`, with the host stack passed in. The manager
  keeps `isFocusNavigationEnabled` (public, observed by the composition) and `FocusNavigationEffect`, which delegates
  each frame to the navigator. Test the repeat delay/interval, the stick threshold, south-activates/east-backs and
  "rest state on open" with a fake `FocusManager` (`moveFocus` recorder) and a fake host.
- B: keep the loop in the manager; test it only through P55's handler seam plus a fake host. Less churn, still no
  frame-clock test.
- C: leave it.

The internal members on the public `GamepadInputManager` stay where they are in every option (they are `internal`,
so not API, but moving them out of a sealed public class touches `GamepadActivationNode` and
`GamepadFocusNavigationHost` (each in its own file since P04, landed in 309fee68) and is not needed for testability).

## Fix (option A)
Move verbatim: the state fields listed above, `restFocusNavigation`, `updateFocusNavigation`, `readFocusDirection`,
`isAnyFocusDirectionHeld` and the `FOCUS_*`/`NANOSECONDS_PER_MILLISECOND`/`MAXIMUM_FRAME_TIME` constants into the
navigator; `private` → `internal` only where the manager calls in. `onFocusNavigationFrame` stays a hoisted,
non-capturing-per-frame lambda (zero per-frame allocation). The `withFrameNanos` loop, `LaunchedEffect` and the
`hasFocusNavigationInput.first { it }` suspension stay in the manager's Composable, on the composition's thread, as
today. Update `plugins/gamepad-input/CLAUDE.md`'s focus-navigation section and Key Files. Grep for every moved name.

## Behaviour
Unchanged: the same code runs on the same thread at the same frames; only its owner changes.

## Public API
None (`GamepadInputManager`'s public members are untouched).

## Tests
`GamepadFocusNavigatorTest` as in option A.

## Verify
`./gradlew :plugins:gamepad-input:compileKotlinDesktop :plugins:gamepad-input:compileAndroidMain :plugins:gamepad-input:desktopTest`

## Manual check
With a controller on desktop and Android: the Showcase menus still walk with the sticks and D-pad, repeat when
held, activate with south and back out with east, and a popup takes over the sticks and hands them back.
