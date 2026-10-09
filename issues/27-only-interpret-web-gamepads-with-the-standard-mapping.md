# Only interpret web gamepads that report the standard mapping

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Web (Wasm)
**Challenged:** sound
**Files:** `plugins/gamepad-input/src/webMain/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadEventHandler.web.kt`, `plugins/gamepad-input/CLAUDE.md`, `plugins/gamepad-input/README.md`

Ships in `io.github.pandulapeter.kubriko:plugin-gamepad-input`. Changes which pads a game sees on the web →
**Decision**.

## Problem

The web backend reads every connected pad with the indices of the Gamepad API's "standard" layout:

```kotlin
// GamepadEventHandler.web.kt
private val STANDARD_BUTTON_INDICES = intArrayOf(0, 1, 2, 3, 4, 5, NO_BUTTON, NO_BUTTON, 10, 11, 12, 13, 14, 15, 9, 8, 16)
...
private fun isGamepadConnected(index: Int): Boolean =
    js("(() => { const pad = globalThis.__kubrikoGamepads[index]; return !!pad && pad.connected; })()")
```

but never checks `Gamepad.mapping`. A pad the browser could not map reports `mapping === ""` and its buttons and axes
in raw device order (common on Firefox/Linux and Safari for less common pads). Kubriko reports it as connected and
feeds the game scrambled input: triggers read from whatever buttons 6/7 are, a face button firing as `START`, a hat
switch or a trigger axis read as the right stick. `CLAUDE.md` claims "only the 'standard' mapping is interpreted",
and the README says such pads report buttons "in an order this plugin has no way to interpret" — neither is what the
code does.

## Decision

- **(a) Treat non-standard pads as not connected (recommended).** Matches the documented behaviour; a game never
  receives input it cannot trust, and `onGamepadConnected` never fires for such a pad. The pad still works once the
  browser learns its mapping (or in another browser).
- (b) Report them as connected but with only the left stick (axes 0/1, which almost every raw layout shares) and no
  buttons. A game sees a pad it can barely use, and axes 0/1 are not guaranteed either.
- (c) Keep reading them and only correct the docs.

## Fix (for option a)

1. In `isGamepadConnected`, also require `pad.mapping === "standard"`:
   `js("(() => { const pad = globalThis.__kubrikoGamepads[index]; return !!pad && pad.connected && pad.mapping === 'standard'; })()")`.
   The `poll()` loop already resets a slot whose pad is not "connected", so a pad whose mapping changes is handled.
2. `CLAUDE.md` Web row: "pads whose `mapping` is not `"standard"` are treated as not connected".
3. `README.md` Platform Limitations, Web bullet: replace "report their buttons in an order this plugin has no way to
   interpret" with "are not reported at all (they would deliver their buttons in an order this plugin has no way to
   interpret)".

No allocation added; it is one extra comparison inside an existing JS call.

## Tests

None runnable: the Wasm test tasks are disabled and the check lives in embedded JavaScript.

## Manual check

Web, a pad Chrome maps as standard (Xbox/DualSense): unchanged. A pad that reports `mapping: ""` (check with
`navigator.getGamepads()` in the console; Firefox on Linux with a generic USB pad is the easy case): `test-input`
(with `showcase.areTestExamplesEnabled=true`) must not list it.
