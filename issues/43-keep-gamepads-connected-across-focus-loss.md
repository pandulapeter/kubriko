# Keep gamepads connected across a focus loss, so connect and disconnect callbacks stay paired

**Challenged:** sound

**Decision needed:** On focus loss, should a connected gamepad stay connected (inputs zeroed, held buttons reported
released) instead of being silently reset to disconnected — so `onGamepadConnected` no longer fires again on every
refocus, and a pad unplugged while unfocused does get `onGamepadDisconnected`? — recommended: **yes, keep the
connection; only the inputs are released**.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `plugin-gamepad-input` — changes which `onGamepadConnected`/`onGamepadDisconnected` callbacks fire
**Depends on:** plan 00 (kotlin-test in the plugin's test source sets)
**Files:** `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadInputManagerImpl.kt`, `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadState.kt`, `plugins/gamepad-input/src/commonTest/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadStateTest.kt` (new), `plugins/gamepad-input/CLAUDE.md`, `plugins/gamepad-input/README.md` (only if it describes focus loss; at HEAD it doesn't)

## Problem

On the first unfocused tick, `onUpdate` releases every slot:

```kotlin
if (!isFocused) {
    if (wasFocused) {
        wasFocused = false
        releaseAllGamepads()
    }
    ...
}
...
private fun releaseGamepad(gamepad: GamepadState) {
    val previousButtons = gamepad.pressedButtons
    gamepad.reset()
    if (previousButtons != 0) {
        notifyButtonChanges(gamepad, previousButtons, 0)
    }
}
```

`GamepadState.reset()` also sets `isConnected = false` and `name = null`, but no `onGamepadDisconnected` is sent and
`connectedGamepadCount` is left unchanged. That path only runs when ticks continue while unfocused (a
`fixedRate`/`fixedFrequency`/`manual` tick source, or `viewportFrames(shouldPauseOnFocusLoss = false)` — the
default viewport ticks stop on focus loss, so the default setup never runs it). When it does:

- On refocus the next poll finds the pad connected while `gamepad.isConnected` is `false`, so `updateGamepad` calls
  `notifyConnected`: **`onGamepadConnected` fires again on every refocus**, for a pad that was never disconnected.
  `GamepadInputAware.onGamepadConnected` is documented as "Called when a gamepad is connected to a previously empty
  slot"; a game that shows a "Player 2 joined" banner or assigns a character on connect does it again every Alt-Tab.
- A pad **unplugged while unfocused** never gets `onGamepadDisconnected`: `gamepad.isConnected` is already `false`
  when the poll reports it gone, so `wasConnected` is `false` and the disconnect branch is skipped.
- While unfocused, `connectedGamepadCount` still reports the old count while every `gamepads[i].isConnected` is
  `false`.

The same unpaired reset happens when the platform handler is replaced (`Composable()` →
`gamepadEventHandler?.isValid() == false` → `stopListening()` → `releaseAllGamepads()`, e.g. an Android Activity
recreation on rotation), with the default tick source: the next poll re-announces every pad with `onGamepadConnected`
and no disconnect before it.

## Fix

**Option A (recommended): focus loss releases inputs, not connections.**
1. Add to `GamepadState`:
   ```kotlin
   internal fun releaseInputs() {
       leftStickX = 0f; leftStickY = 0f; rightStickX = 0f; rightStickY = 0f
       leftTrigger = 0f; rightTrigger = 0f; pressedButtons = 0
   }
   ```
   and have `reset()` call it plus clear `isConnected` and `name` (one line each, so the two can't drift).
2. The focus-loss path uses a new `releaseAllInputs()` that, per slot, remembers `pressedButtons`, calls
   `releaseInputs()`, and reports the released buttons through `notifyButtonChanges` — the same as today minus the
   disconnection. On refocus, `updateGamepad` then sees `wasConnected == true`: a still-connected pad produces no
   connect event, and one unplugged meanwhile goes through the existing disconnect branch (`releaseGamepad` +
   `notifyDisconnected`).
3. `stopListening()` when called from `Composable()` (handler replaced) reports every connected slot as
   disconnected (`releaseGamepad` + `notifyDisconnected`) before resetting the raw states, so the connect the new
   handler triggers is paired. `onDispose()` keeps today's silent reset (the Actors are being torn down with the
   instance). Split `stopListening()` into the two variants or give it a `shouldNotifyActors` parameter.
4. `GamepadState.isConnected`'s KDoc ("All other properties read as zero while this is false") stays true; no
   public KDoc changes.

**Option B: make focus loss a real disconnection.** In the focus-loss path call `notifyDisconnected` for every
connected slot after the release and set `_connectedGamepadCount.value = 0`; refocus then fires a paired
`onGamepadConnected`. Consistent, but every Alt-Tab still looks like every pad was unplugged and plugged back in,
which is what a game using the callbacks for "player joined/left" UI does not want.

## Tests

`GamepadStateTest` in `commonTest` (the constructor is `internal`, visible to the module's own tests):
1. A state with `isConnected = true`, a name, non-zero sticks/triggers and pressed buttons → `releaseInputs()` →
   still connected, same name, all axes 0, `isPressed(b)` false for every `GamepadButton`.
2. `reset()` → not connected, `name == null`, everything zero.

The manager's callback sequencing can't be unit-tested without a composition: `gamepadEventHandler` is only created
in `Composable()`, and the focus state is set by `KubrikoViewport`.

## Manual check

Desktop with a controller, a test build whose Kubriko instance uses `TickSource.fixedFrequency(60)` (started
explicitly) and an Actor that logs `onGamepadConnected`/`onGamepadDisconnected`:
1. Connect the pad, Alt-Tab away and back five times: exactly one "connected" in the log (option A).
2. Alt-Tab away, unplug the pad, come back: one "disconnected" is logged.
3. Android with the default tick source and a controller: rotate the device: "disconnected" then "connected" per pad,
   never two "connected" in a row.

Update `plugins/gamepad-input/CLAUDE.md` → Focus Navigation's last paragraph ("On focus loss and on disconnection
every held button is reported as released and the state is zeroed"): on focus loss the inputs are zeroed and held
buttons reported released, but the pad stays connected; on disconnection (including a replaced platform handler) the
pad is also reported disconnected.
